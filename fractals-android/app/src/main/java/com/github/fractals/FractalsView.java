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
 * https://github.com/pnemonic78/Electric-Fields
 *
 * Contributor(s):
 *   Moshe Waisberg
 *
 */
package com.github.fractals;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Electric Fields view.
 *
 * @author Moshe Waisberg
 */
public class FractalsView extends View implements FractalAsyncTask.FieldAsyncTaskListener {

    public static final int MAX_CHARGES = 10;

    private final List<Charge> charges = new CopyOnWriteArrayList<>();
    private Bitmap bitmap;
    private FractalAsyncTask task;
    private int sameChargeDistance;
    private FractalsListener listener;

    public FractalsView(Context context) {
        super(context);
        init(context);
    }

    public FractalsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FractalsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Resources res = context.getResources();
        sameChargeDistance = res.getDimensionPixelSize(R.dimen.same_charge);
        sameChargeDistance = sameChargeDistance * sameChargeDistance;
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(getBitmap(), 0, 0, null);
    }

    /**
     * Start the task.
     */
    public void start() {
        if (!isRendering()) {
            task = new FractalAsyncTask(this, new Canvas(getBitmap()));
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

        if (charges.size() > 0) {
            SavedState ss = new SavedState(superState);
            ss.charges = charges;
            return ss;
        }

        return superState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.charges != null) {
            clear();
            for (Charge charge : ss.charges) {
                addCharge(charge);
            }
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

    public static class SavedState extends BaseSavedState {

        List<Charge> charges;

        protected SavedState(Parcel source) {
            super(source);
            charges = new ArrayList<>();
            source.readTypedList(charges, Charge.CREATOR);
        }

        protected SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeTypedList(charges);
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
}
