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
public class FractalAsyncTask extends AsyncTask<Charge, Canvas, Canvas> {

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
    private double warp = 2;
    private double zoom = 1e+3;
    private final Complex pan = new Complex(0, 0);

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
    }

    @Override
    protected Canvas doInBackground(Charge... params) {
        try {
            Thread.sleep(startDelay);
        } catch (InterruptedException e) {
            // Ignore.
        }

        final ChargeHolder[] charges = ChargeHolder.toChargedParticles(params);
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        int size = Math.max(w, h);

        int shifts = 0;
        while (size > 1) {
            size >>>= 1;
            shifts++;
        }
        double density = zoom;

        // Make "resolution2" a power of 2, so that "resolution" is always divisible by 2.
        int resolution2 = 1 << shifts;
        int resolution = resolution2;

        canvas.drawColor(Color.WHITE);
        plotMandelbrot(canvas, 0, 0, resolution, resolution, density);

        int x1, y1, x2, y2;

        do {
            y1 = 0;
            y2 = resolution;

            while (y1 < h) {
                x1 = 0;
                x2 = resolution;

                while (x1 < w) {
                    plotMandelbrot(canvas, x1, y2, resolution, resolution, density);
                    plotMandelbrot(canvas, x2, y1, resolution, resolution, density);
                    plotMandelbrot(canvas, x2, y2, resolution, resolution, density);

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
     * {@code Z := Z * Z + K}
     */
    private void plotMandelbrot(Canvas canvas, int x, int y, int w, int h, double density) {
        double zRe = (x / zoom) + pan.getReal();
        double zIm = (y / zoom) + pan.getImaginary();
        double zReAbs, zImAbs;
        double kRe = zRe;
        double kIm = zIm;
        final double kReAbs = Math.abs(kRe);
        final double kImAbs = Math.abs(kIm);

        int i = 0;
        do {
            zRe = (zRe * zRe) - (zIm * zIm) + kRe;
            zIm = (warp * zRe * zIm) + kIm;
            zReAbs = Math.abs(zRe);
            zImAbs = Math.abs(zIm);
            i++;
        }
        while ((zReAbs <= w) && (zImAbs <= h) && (i <= 1000) && ((zReAbs != kReAbs) || (zImAbs != kImAbs)));

        paint.setColor(mapColor(i, density));
        rect.set(x, y, x + w, y + h);
        canvas.drawRect(rect, paint);
    }

    private int mapColor(double z, double density) {
        if (Double.isInfinite(z)) {
            return Color.WHITE;
        }
        hsv[0] = (float) ((z * density) % 360);
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

    private static class ChargeHolder {
        public final int x;
        public final int y;
        public final double size;
        public final double sizeSqr;

        public ChargeHolder(Charge charge) {
            this(charge.x, charge.y, charge.size);
        }

        public ChargeHolder(int x, int y, double size) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.sizeSqr = Math.signum(size) * size * size;
        }

        public static ChargeHolder[] toChargedParticles(Charge[] charges) {
            final int length = charges.length;
            ChargeHolder[] result = new ChargeHolder[length];

            for (int i = 0; i < length; i++) {
                result[i] = new ChargeHolder(charges[i]);
            }

            return result;
        }
    }
}
