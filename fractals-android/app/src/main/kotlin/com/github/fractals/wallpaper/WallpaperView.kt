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
package com.github.fractals.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.AsyncTask
import android.view.GestureDetector
import android.view.MotionEvent
import com.github.fractals.FractalAsyncTask
import com.github.fractals.Fractals

/**
 * Live wallpaper view.
 * @author Moshe Waisberg
 */
class WallpaperView(context: Context, listener: WallpaperListener) :
        Fractals,
        FractalAsyncTask.FieldAsyncTaskListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener {

    var width: Int = 0
        private set
    var height: Int = 0
        private set
    private var bitmap: Bitmap? = null
    private var task: FractalAsyncTask? = null
    private var listener: WallpaperListener? = null
    private val gestureDetector: GestureDetector
    var idle = false
        private set
    /**
     * The matrix for the bitmap.
     */
    val bitmapMatrix = Matrix()

    init {
        gestureDetector = GestureDetector(context, this)
        setWallpaperListener(listener)
    }

    override fun clear() {
        bitmapMatrix.reset()
    }

    fun draw(canvas: Canvas) {
        onDraw(canvas)
    }

    protected fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    override fun start(delay: Long) {
        if (!isRendering) {
            task = FractalAsyncTask(this, Canvas(bitmap!!))
            task!!.setSaturation(0.5f)
            task!!.setBrightness(0.5f)
            task!!.setStartDelay(delay)
            task!!.execute(bitmapMatrix)
        }
    }

    override fun stop() {
        if (task != null) {
            task!!.cancel(true)
        }
    }

    /**
     * Set the listener for events.
     * @param listener the listener.
     */
    fun setWallpaperListener(listener: WallpaperListener) {
        this.listener = listener
    }

    override fun onTaskStarted(task: FractalAsyncTask) {
        if (listener != null) {
            listener!!.onRenderFieldStarted(this)
        }
    }

    override fun onTaskFinished(task: FractalAsyncTask) {
        if (task === this.task) {
            invalidate()
            if (listener != null) {
                listener!!.onRenderFieldFinished(this)
            }
            clear()
        }
    }

    override fun onTaskCancelled(task: FractalAsyncTask) {
        if (listener != null) {
            listener!!.onRenderFieldCancelled(this)
        }
    }

    override fun repaint(task: FractalAsyncTask) {
        invalidate()
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {}

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {}

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    private fun invalidate() {
        if (listener != null) {
            listener!!.onDraw(this)
        }
    }

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height

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
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        }
    }

    /**
     * Is the task busy rendering the fields?
     * @return `true` if rendering.
     */
    val isRendering: Boolean
        get() = task != null && !task!!.isCancelled && task!!.status != AsyncTask.Status.FINISHED

    fun onTouchEvent(event: MotionEvent) {
        gestureDetector.onTouchEvent(event)
    }
}
