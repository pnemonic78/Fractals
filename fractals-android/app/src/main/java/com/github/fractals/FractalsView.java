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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Fractals view.
 *
 * @author Moshe Waisberg
 */
public class FractalsView extends View implements FractalAsyncTask.FieldAsyncTaskListener {

    /**
     * Make the fractal appear to be shifted in the leftward direction.
     */
    public static final float LEFT = +1;
    /**
     * Make the fractal appear to be shifted in the upwrd direction.
     */
    public static final float UP = +1;
    /**
     * Make the fractal appear to be shifted in the rightward direction.
     */
    public static final float RIGHT = -1;
    /**
     * Make the fractal appear to be shifted in the downward direction.
     */
    public static final float DOWN = -1;

    private Bitmap bitmap;
    private FractalAsyncTask task;
    private FractalsListener listener;
    private float scrollX = 0;
    private float scrollY = 0;
    private float zoom = 1;

    public FractalsView(Context context) {
        super(context);
    }

    public FractalsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FractalsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void clear() {
        setScroll(0, 0);
        setZoom(1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        Bitmap bitmap = getBitmap();
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        canvas.translate((width - bitmapWidth) / 2, (height - bitmapHeight) / 2);
        canvas.drawBitmap(bitmap, 0, 0, null);
    }

    /**
     * Start the task.
     */
    public void start() {
        if (!isRendering()) {
            task = new FractalAsyncTask(this, new Canvas(getBitmap()));
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
        cancel();
        start();
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
            invalidate();
            if (listener != null) {
                listener.onRenderFieldFinished(this);
            }
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
        postInvalidate();
    }

    /**
     * Get the bitmap.
     *
     * @return the bitmap.
     */
    public Bitmap getBitmap() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

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
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    /**
     * Set the listener for events.
     *
     * @param listener the listener.
     */
    public void setElectricFieldsListener(FractalsListener listener) {
        this.listener = listener;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.scrollX = scrollX;
        ss.scrollY = scrollY;
        ss.zoom = zoom;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.zoom != 0) {
            clear();
            setScroll(ss.scrollX, ss.scrollY);
            setZoom(ss.zoom);
            restart();
        }
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
     * Set the scrolling offsets. Parameter values should already be scaled to the zoom.
     *
     * @param scrollX The horizontal offset. Positive values make the image appear to move leftwards.
     * @param scrollY The vertical offset. Positive values make the image appear to move upwards.
     * @see #LEFT
     * @see #RIGHT
     * @see #UP
     * @see #DOWN
     */
    public void setScroll(float scrollX, float scrollY) {
        this.scrollX = scrollX;
        this.scrollY = scrollY;
    }

    public float getScrollXF() {
        return scrollX;
    }

    public float getScrollYF() {
        return scrollY;
    }

    /**
     * Set the zoom scale factor.
     *
     * @param zoom The zoom.
     */
    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    /**
     * Get the zoom scale factor.
     *
     * @return The zoom.
     */
    public float getZoom() {
        return zoom;
    }

    public static class SavedState extends BaseSavedState {

        float scrollX, scrollY;
        float zoom;

        protected SavedState(Parcel source) {
            super(source);
            scrollX = source.readFloat();
            scrollY = source.readFloat();
            zoom = source.readFloat();
        }

        protected SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeFloat(scrollX);
            out.writeFloat(scrollY);
            out.writeFloat(zoom);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bitmap = null;
        if (task != null) {
            restart();
        }
    }
}
