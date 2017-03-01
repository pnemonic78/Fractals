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
package com.github.fractals.wallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;

import com.github.fractals.FractalAsyncTask;

/**
 * Live wallpaper view.
 *
 * @author Moshe Waisberg
 */
public class WallpaperView implements FractalAsyncTask.FieldAsyncTaskListener {

    private int width, height;
    private Bitmap bitmap;
    private FractalAsyncTask task;
    private WallpaperListener listener;
    private float scrollX = 0;
    private float scrollY = 0;
    private float zoom = 1;

    public WallpaperView(Context context, WallpaperListener listener) {
        setWallpaperListener(listener);
    }

    public void clear() {
        scrollX = 0;
        scrollY = 0;
        zoom = 1;
    }

    public void draw(Canvas canvas) {
        onDraw(canvas);
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    /**
     * Start the task.
     */
    public void start() {
        start(0L);
    }

    /**
     * Start the task.
     *
     * @param delay the start delay, in milliseconds.
     */
    public void start(long delay) {
        if (!isRendering()) {
            task = new FractalAsyncTask(this, new Canvas(bitmap));
            task.setSaturation(0.5f);
            task.setBrightness(0.5f);
            task.setStartDelay(delay);
            task.execute((double) scrollX, (double) scrollY, (double) zoom);
        }
    }

    /**
     * Cancel the task.
     */
    public void cancel() {
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * Restart the task with modified charges.
     */
    public void restart() {
        restart(0L);
    }

    /**
     * Restart the task with modified charges.
     *
     * @param delay the start delay, in milliseconds.
     */
    public void restart(long delay) {
        cancel();
        start(delay);
    }

    /**
     * Set the listener for events.
     *
     * @param listener the listener.
     */
    public void setWallpaperListener(WallpaperListener listener) {
        this.listener = listener;
    }

    @Override
    public void onTaskStarted(FractalAsyncTask task) {
        if (listener != null) {
            listener.onRenderFieldStarted(this);
        }
    }

    @Override
    public void onTaskFinished(FractalAsyncTask task) {
        if (task == this.task) {
            if (listener != null) {
                invalidate();
                listener.onRenderFieldFinished(this);
            }
            clear();
        }
    }

    @Override
    public void onTaskCancelled(FractalAsyncTask task) {
        if (listener != null) {
            listener.onRenderFieldCancelled(this);
        }
    }

    @Override
    public void repaint(FractalAsyncTask task) {
        invalidate();
    }

    private void invalidate() {
        if (listener != null) {
            listener.onDraw(this);
        }
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;

        Bitmap bitmapOld = bitmap;
        if (bitmapOld != null) {
            int bw = bitmapOld.getWidth();
            int bh = bitmapOld.getHeight();

            if ((width != bw) || (height != bh)) {
                Matrix m = new Matrix();
                // Changed orientation?
                if ((width < bw) && (height > bh)) {// Portrait?
                    m.postRotate(90, bw / 2, bh / 2);
                } else {// Landscape?
                    m.postRotate(270, bw / 2, bh / 2);
                }
                Bitmap rotated = Bitmap.createBitmap(bitmapOld, 0, 0, bw, bh, m, true);
                bitmap = Bitmap.createScaledBitmap(rotated, width, height, true);
            }
        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Is the task busy rendering the fields?
     *
     * @return {@code true} if rendering.
     */
    public boolean isRendering() {
        return (task != null) && !task.isCancelled() && (task.getStatus() != AsyncTask.Status.FINISHED);
    }

    /**
     * Set the scrolling offsets.
     *
     * @param scrollX the horizontal offset.
     * @param scrollY the vertical offset.
     */
    public void setScroll(float scrollX, float scrollY) {
        this.scrollX = scrollX;
        this.scrollY = scrollY;
    }

    /**
     * Set the zoom scale factor.
     *
     * @param zoom the zoom.
     */
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }
}
