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
package com.github.fractals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.AsyncTask
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View

/**
 * Fractals view.
 * @author Moshe Waisberg
 */
class FractalsView : View, FractalAsyncTask.FieldAsyncTaskListener, Fractals {

    private var bitmap: Bitmap? = null
    private var task: FractalAsyncTask? = null
    private var listener: FractalsListener? = null
    /**
     * The matrix for the bitmap.
     */
    val bitmapMatrix = Matrix()

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun clear() {
        bitmapMatrix.reset()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val b = getBitmap()
        val dx = (measuredWidth - b.width) / 2f
        val dy = (measuredHeight - b.height) / 2f
        canvas.drawBitmap(b, dx, dy, null)
    }

    override fun start(delay: Long) {
        if (!isRendering) {
            task = FractalAsyncTask(this, Canvas(getBitmap()))
            task!!.execute(bitmapMatrix)
        }
    }

    override fun stop() {
        if (task != null) {
            task!!.cancel(true)
        }
    }

    override fun onTaskStarted(task: FractalAsyncTask) {
        if (listener != null) {
            listener!!.onRenderFieldStarted(this)
        }
    }

    override fun onTaskFinished(task: FractalAsyncTask) {
        if (task == this.task) {
            invalidate()
            if (listener != null) {
                listener!!.onRenderFieldFinished(this)
            }
        }
    }

    override fun onTaskCancelled(task: FractalAsyncTask) {
        if (listener != null) {
            listener!!.onRenderFieldCancelled(this)
        }
    }

    override fun repaint(task: FractalAsyncTask) {
        postInvalidate()
    }

    /**
     * Get the bitmap.
     * @return the bitmap.
     */
    fun getBitmap(): Bitmap {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        val bitmapOld = bitmap
        if (bitmapOld != null) {
            val bw = bitmapOld.width
            val bh = bitmapOld.height

            if ((width != bw) || (height != bh)) {
                val m = Matrix()
                // Changed orientation?
                if ((width < bw) && (height > bh)) {// Portrait?
                    m.postRotate(90f, bw / 2f, bh / 2f)
                } else {// Landscape?
                    m.postRotate(270f, bw / 2f, bh / 2f)
                }
                val rotated = Bitmap.createBitmap(bitmapOld, 0, 0, bw, bh, m, true)
                bitmap = Bitmap.createScaledBitmap(rotated, width, height, true)
            }
        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        return bitmap!!
    }

    /**
     * Set the listener for events.
     * @param listener the listener.
     */
    fun setElectricFieldsListener(listener: FractalsListener) {
        this.listener = listener
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()

        val ss = SavedState(superState)
        ss.values = FloatArray(9)
        bitmapMatrix.getValues(ss.values)
        return ss
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        val ss = state
        super.onRestoreInstanceState(ss.superState)

        if (ss.values != null) {
            clear()
            val matrix = bitmapMatrix
            matrix.setValues(ss.values)
            restart()
        }
    }

    /**
     * Is the task busy rendering the fields?
     * @return `true` if rendering.
     */
    val isRendering: Boolean
        get() = (task != null) && !task!!.isCancelled && (task!!.status != AsyncTask.Status.FINISHED)

    class SavedState : View.BaseSavedState {

        internal var values: FloatArray? = null

        protected constructor(source: Parcel) : super(source) {
            source.readFloatArray(values)
        }

        constructor(superState: Parcelable) : super(superState) {}

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloatArray(values)
        }

        companion object {

            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(p: Parcel): SavedState {
                    return SavedState(p)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }
}
