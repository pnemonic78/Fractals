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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;

import com.github.fractals.Charge;
import com.github.fractals.FractalAsyncTask;
import com.github.fractals.R;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.fractals.FractalsView.MAX_CHARGES;

/**
 * Live wallpaper view.
 *
 * @author Moshe Waisberg
 */
public class WallpaperView implements FractalAsyncTask.FieldAsyncTaskListener {

    private int width, height;
    private final List<Charge> charges = new CopyOnWriteArrayList<>();
    private Bitmap bitmap;
    private FractalAsyncTask task;
    private int sameChargeDistance;
    private WallpaperListener listener;

    public WallpaperView(Context context, WallpaperListener listener) {
        Resources res = context.getResources();
        sameChargeDistance = res.getDimensionPixelSize(R.dimen.same_charge);
        sameChargeDistance = sameChargeDistance * sameChargeDistance;
        setWallpaperListener(listener);
    }

    public boolean addCharge(int x, int y, double size) {
        return addCharge(new Charge(x, y, size));
    }

    public boolean addCharge(Charge charge) {
        if (charges.size() < MAX_CHARGES) {
            if (charges.add(charge)) {
                if (listener != null) {
                    listener.onChargeAdded(this, charge);
                }
                return true;
            }
        }
        return false;
    }

    public boolean invertCharge(int x, int y) {
        Charge charge = findCharge(x, y);
        if (charge != null) {
            charge.size = -charge.size;
            if (listener != null) {
                listener.onChargeInverted(this, charge);
            }
            return true;
        }
        return false;
    }

    public Charge findCharge(int x, int y) {
        final int count = charges.size();
        Charge charge;
        Charge chargeNearest = null;
        int dx, dy, d;
        int dMin = Integer.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            charge = charges.get(i);
            dx = x - charge.x;
            dy = y - charge.y;
            d = (dx * dx) + (dy * dy);
            if ((d <= sameChargeDistance) && (d < dMin)) {
                chargeNearest = charge;
                dMin = d;
            }
        }

        return chargeNearest;
    }

    public void clear() {
        charges.clear();
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
            task.execute(charges.toArray(new Charge[charges.size()]));
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
}
