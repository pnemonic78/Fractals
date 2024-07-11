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
import android.view.GestureDetector
import android.view.MotionEvent
import com.github.fractals.FractalTask
import com.github.fractals.Fractals
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Live wallpaper view.
 *
 * @author Moshe Waisberg
 */
class WallpaperView(context: Context, listener: WallpaperListener) :
    Fractals,
    Observer<Bitmap>,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    var width: Int = 0
        private set
    var height: Int = 0
        private set
    private var bitmap: Bitmap? = null
    private var task: FractalTask? = null
    private var listener: WallpaperListener? = null
    private val gestureDetector: GestureDetector = GestureDetector(context, this)
    private var isIdle = false

    /**
     * The matrix for the bitmap.
     */
    val bitmapMatrix = Matrix()

    init {
        setWallpaperListener(listener)
    }

    override fun clear() {
        bitmapMatrix.reset()
    }

    override fun start(delay: Long) {
        if (isIdle) {
            val observer = this
            FractalTask(bitmapMatrix, bitmap!!).apply {
                task = this
                saturation = 0.5f
                brightness = 0.5f
                startDelay = delay
                subscribeOn(Schedulers.computation())
                    .subscribe(observer)
            }
        }
    }

    override fun stop() {
        task?.cancel()
        isIdle = true
    }

    fun onTouchEvent(event: MotionEvent) {
        gestureDetector.onTouchEvent(event)
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

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) = Unit

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) = Unit

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onNext(value: Bitmap) {
        invalidate()
    }

    override fun onError(e: Throwable) {
        isIdle = true
        listener?.onRenderFieldCancelled(this)
    }

    override fun onComplete() {
        isIdle = true
        listener?.onRenderFieldFinished(this)
    }

    override fun onSubscribe(d: Disposable) {
        isIdle = false
        listener?.onRenderFieldStarted(this)
    }

    /**
     * Set the listener for events.
     *
     * @param listener the listener.
     */
    fun setWallpaperListener(listener: WallpaperListener) {
        this.listener = listener
    }

    protected fun invalidate() {
        listener?.onDraw(this)
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
                if (rotated !== bitmapOld) bitmapOld.recycle()
                bitmap = Bitmap.createScaledBitmap(rotated, width, height, true)
                if (rotated !== bitmap) rotated.recycle()
            }
        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        }
    }

    fun draw(canvas: Canvas) {
        onDraw(canvas)
    }

    private fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
    }

    fun onDestroy() {
        bitmap?.recycle()
        bitmap = null
    }
}
