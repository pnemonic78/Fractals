/*
 * Copyright 2016, Moshe Waisberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.fractals;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

/**
 * Fractals task.
 *
 * @author Moshe Waisberg
 */
public class FractalAsyncTask extends AsyncTask<Matrix, Canvas, Canvas> {

    private static final String TAG = "FractalAsyncTask";

    public interface FieldAsyncTaskListener {
        /**
         * Notify the listener that the task has started processing the charges.
         *
         * @param task the caller task.
         */
        void onTaskStarted(FractalAsyncTask task);

        /**
         * Notify the listener that the task has finished.
         *
         * @param task the caller task.
         */
        void onTaskFinished(FractalAsyncTask task);

        /**
         * Notify the listener that the task has aborted.
         *
         * @param task the caller task.
         */
        void onTaskCancelled(FractalAsyncTask task);

        /**
         * Notify the listener to repaint its bitmap.
         *
         * @param task the caller task.
         */
        void repaint(FractalAsyncTask task);
    }

    private static final double RE_MIN = -2.0;
    private static final double RE_MAX = -RE_MIN;
    private static final double RE_SIZE = RE_MAX - RE_MIN;
    private static final double IM_MIN = RE_MIN;
    private static final double IM_MAX = -IM_MIN;
    private static final double IM_SIZE = IM_MAX - IM_MIN;

    private static final double LOG2 = Math.log(2.0);
    private static final double LOG2_LOG2 = Math.log(LOG2) / LOG2;
    private static final double LOG2_LOG2_2 = 2.0 + LOG2_LOG2;
    private static final int OVERFLOW = 300;

    private final FieldAsyncTaskListener listener;
    private final Canvas canvas;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float[] hsv = {0f, 1f, 1f};
    private long startDelay = 0L;
    private double zoom = 1.0;
    private double scrollX = 0.0;
    private double scrollY = 0.0;

    public FractalAsyncTask(FieldAsyncTaskListener listener, Canvas canvas) {
        this.listener = listener;
        this.canvas = canvas;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);

        listener.onTaskStarted(this);

        scrollX = 0.0;
        scrollY = 0.0;
        zoom = 1.0;
    }

    @Override
    protected Canvas doInBackground(Matrix... params) {
        if (startDelay > 0L) {
            try {
                Thread.sleep(startDelay);
            } catch (InterruptedException e) {
                // Ignore.
            }
        }
        final long timeStart = SystemClock.elapsedRealtime();
        if (params.length >= 1) {
            Matrix matrix = params[0];
            float[] matrixValues = new float[9];
            matrix.getValues(matrixValues);
            scrollX = matrixValues[Matrix.MTRANS_X];
            scrollY = matrixValues[Matrix.MTRANS_Y];
            zoom = matrixValues[Matrix.MSCALE_X];
        }

        int w = canvas.getWidth();
        int h = canvas.getHeight();
        int sizeMax = Math.max(w, h);
        int sizeMin = Math.min(w, h);
        double sizeRe = (h >= w) ? sizeMin : sizeMin * RE_SIZE / IM_SIZE;
        double sizeIm = (h >= w) ? sizeMin * IM_SIZE / RE_SIZE : sizeMin;
        double sizeSetRe = sizeRe * zoom / RE_SIZE;
        double sizeSetIm = sizeIm * zoom / IM_SIZE;
        double offsetRe = scrollX + Math.min(0.0, (sizeRe - w) / 2.0);
        double offsetIm = scrollY + Math.min(0.0, (sizeIm - h) / 2.0);

        int shifts = 0;
        while (sizeMax > 1) {
            sizeMax >>>= 1;
            shifts++;
        }
        double density = 1e+1;

        // Make "resolution2" a power of 2, so that "resolution" is always divisible by 2.
        int resolution2 = 1 << shifts;
        int resolution = resolution2;

        canvas.drawColor(Color.WHITE);

        double dxRe = resolution / sizeSetRe;
        double dyIm = resolution / sizeSetIm;
        double dxRe2 = resolution2 / sizeSetRe;
        double dyIm2 = resolution2 / sizeSetIm;
        double x0Re = (offsetRe / sizeSetRe) + (RE_MIN / zoom);
        double y0Im = (offsetIm / sizeSetIm) + (IM_MIN / zoom);

        plotMandelbrot(canvas, 0, 0, resolution, resolution, x0Re, y0Im, density);
        listener.repaint(this);

        int x1, y1, x2, y2;
        double x1Re, y1Im, x2Re, y2Im;

        do {
            y1 = 0;
            y2 = resolution;
            y1Im = y0Im;
            y2Im = y1Im + dyIm;

            while (y1 < h) {
                x1 = 0;
                x2 = resolution;
                x1Re = x0Re;
                x2Re = x1Re + dxRe;

                while (x1 < w) {
                    plotMandelbrot(canvas, x1, y2, resolution, resolution, x1Re, y2Im, density);
                    plotMandelbrot(canvas, x2, y1, resolution, resolution, x2Re, y1Im, density);
                    plotMandelbrot(canvas, x2, y2, resolution, resolution, x2Re, y2Im, density);

                    x1 += resolution2;
                    x2 += resolution2;
                    x1Re += dxRe2;
                    x2Re += dxRe2;
                    if (isCancelled()) {
                        return null;
                    }
                }
                listener.repaint(this);

                y1 += resolution2;
                y2 += resolution2;
                y1Im += dyIm2;
                y2Im += dyIm2;
                if (isCancelled()) {
                    return null;
                }
            }

            resolution2 = resolution;
            resolution = resolution2 >> 1;
            dxRe = resolution / sizeSetRe;
            dyIm = resolution / sizeSetIm;
            dxRe2 = resolution2 / sizeSetRe;
            dyIm2 = resolution2 / sizeSetIm;
            if (isCancelled()) {
                return null;
            }
        } while (resolution >= 1);

        final long timeEnd = SystemClock.elapsedRealtime();
        Log.v(TAG, "Rendered in " + (timeEnd - timeStart) + "ms");

        return canvas;
    }

    @Override
    protected void onProgressUpdate(Canvas... values) {
        super.onProgressUpdate(values);
        listener.repaint(this);
    }

    @Override
    protected void onPostExecute(Canvas result) {
        super.onPostExecute(result);
        listener.onTaskFinished(this);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        listener.onTaskCancelled(this);
    }

    /**
     * Plot a Mandelbrot point.
     * <br>
     * {@code z := z * z + c}
     * <br>
     * http://en.wikipedia.org/wiki/Mandelbrot_set
     */
    private void plotMandelbrot(Canvas canvas, int x, int y, int w, int h, double kRe, double kIm, double density) {
        double zRe = 0.0;
        double zIm = 0.0;
        double zReSrq = 0.0;
        double zImSrq = 0.0;
        double d;
        double r;
        int i = 0;
        boolean underflow;

        do {
            r = zReSrq - zImSrq + kRe;
            zIm = (2.0 * zRe * zIm) + kIm;
            zRe = r;
            zReSrq = zRe * zRe;
            zImSrq = zIm * zIm;
            d = zReSrq + zImSrq;
            i++;
            underflow = i < OVERFLOW;
        } while (underflow && (d < 9));

        double z = i;
        if (underflow) {
            z += LOG2_LOG2_2 - (Math.log(Math.log(d)) / LOG2);
        } else {
            z = 0.0;
        }

        paint.setColor(mapColor(z, density));
        rect.set(x, y, x + w, y + h);
        canvas.drawRect(rect, paint);
    }

    private int mapColor(double c, double density) {
        if (c == 0.0) {
            return Color.BLACK;
        }
        hsv[0] = (float) ((c * density) % 360);
        return Color.HSVToColor(hsv);
    }

    /**
     * Set the HSV saturation.
     *
     * @param value a value between [0..1] inclusive.
     */
    public void setSaturation(float value) {
        hsv[1] = value;
    }

    /**
     * Set the HSV brightness.
     *
     * @param value a value between [0..1] inclusive.
     */
    public void setBrightness(float value) {
        hsv[2] = value;
    }

    /**
     * Set the start delay.
     *
     * @param delay the start delay, in milliseconds.
     */
    public void setStartDelay(long delay) {
        startDelay = delay;
    }
}
