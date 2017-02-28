/*
 * Source file of the Remove Duplicates project.
 * Copyright (c) 2016. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/2.0
 *
 * Contributors can be contacted by electronic mail via the project Web pages:
 *
 * https://github.com/pnemonic78/Fractals
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package com.github.fractals;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.AsyncTask;

/**
 * Fractals task.
 *
 * @author Moshe Waisberg
 */
public class FractalAsyncTask extends AsyncTask<Double, Canvas, Canvas> {

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

    private final FieldAsyncTaskListener listener;
    private final Canvas canvas;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float[] hsv = {0f, 1f, 1f};
    private long startDelay = 0L;
    private double zoom = 1;
    private double scrollX = 0;
    private double scrollY = 0;

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

        scrollX = 0;
        scrollY = 0;
        zoom = 1;
    }

    @Override
    protected Canvas doInBackground(Double... params) {
        try {
            Thread.sleep(startDelay);
        } catch (InterruptedException e) {
            // Ignore.
        }
        if (params.length >= 2) {
            scrollX = params[0];
            scrollY = params[1];
            if (params.length >= 3) {
                zoom = params[2];
            }
        }

        int w = canvas.getWidth();
        int h = canvas.getHeight();
        int size = Math.max(w, h);
        int sw = Math.min(w, h);
        int sh = sw;

        int shifts = 0;
        while (size > 1) {
            size >>>= 1;
            shifts++;
        }
        double density = 1e+1;

        // Make "resolution2" a power of 2, so that "resolution" is always divisible by 2.
        int resolution2 = 1 << shifts;
        int resolution = resolution2;

        canvas.drawColor(Color.WHITE);
        plotMandelbrot(canvas, 0, 0, resolution, resolution, sw, sh, density);

        int x1, y1, x2, y2;

        do {
            y1 = 0;
            y2 = resolution;

            while (y1 < h) {
                x1 = 0;
                x2 = resolution;

                while (x1 < w) {
                    plotMandelbrot(canvas, x1, y2, resolution, resolution, sw, sh, density);
                    plotMandelbrot(canvas, x2, y1, resolution, resolution, sw, sh, density);
                    plotMandelbrot(canvas, x2, y2, resolution, resolution, sw, sh, density);

                    x1 += resolution2;
                    x2 += resolution2;
                    if (isCancelled()) {
                        return null;
                    }
                }
                listener.repaint(this);

                y1 += resolution2;
                y2 += resolution2;
                if (isCancelled()) {
                    return null;
                }
            }

            resolution2 = resolution;
            resolution = resolution2 >> 1;
            if (isCancelled()) {
                return null;
            }
        } while (resolution >= 1);

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
    private void plotMandelbrot(Canvas canvas, int x, int y, int w, int h, int sw, int sh, double density) {
        double kRe = ((x + scrollX) / zoom / (sw / 3.5)) - 2.5;
        double kIm = ((y + scrollY) / zoom / (sh / 3.5)) - 2.5;
        double zRe = 0;
        double zIm = 0;
        double zReSrq = 0;
        double zImSrq = 0;
        double r;
        int i = 0;

        do {
            r = zReSrq - zImSrq + kRe;
            zIm = (2 * zRe * zIm) + kIm;
            zRe = r;
            zReSrq = zRe * zRe;
            zImSrq = zIm * zIm;
            i++;
        } while ((i < 1000) && ((zReSrq + zImSrq) < 9));

        paint.setColor(mapColor(i, density));
        rect.set(x, y, x + w, y + h);
        canvas.drawRect(rect, paint);
    }

    private int mapColor(double c, double density) {
        if (c == 0) {
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
