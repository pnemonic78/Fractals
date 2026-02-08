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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.lifecycle.LifecycleCoroutineScope
import com.github.fractals.FractalImage
import com.github.fractals.FractalTask
import com.github.fractals.Fractals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/**
 * Live wallpaper view.
 *
 * @author Moshe Waisberg
 */
class WallpaperView(
    context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    listener: WallpaperListener
) : Fractals,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    var width: Int = 0
        private set
    var height: Int = 0
        private set
    private var bitmap: Bitmap? = null
    private var task: Job? = null
    private var listener: WallpaperListener? = null
    private val gestureDetector: GestureDetector = GestureDetector(context, this)

    /**
     * The matrix for the bitmap.
     */
    val bitmapMatrix = Matrix()

    init {
        setWallpaperListener(listener)
    }

    fun isIdle(): Boolean {
        val job = task ?: return true
        return job.isCancelled || job.isCompleted || !job.isActive
    }

    override fun clear() {
        bitmapMatrix.reset()
    }

    override fun start(delay: Long) {
        if (isIdle()) {
            task = lifecycleScope.launch(Dispatchers.Default) {
                FractalTask(bitmap!!, bitmapMatrix).apply {
                    saturation = 0.5f
                    brightness = 0.5f
                    startDelay = delay
                }
                    .flowOn(Dispatchers.Default)
                    .onStart { onTaskStart() }
                    .onCompletion { onTaskComplete() }
                    .catch { onTaskError(it) }
                    .collect { onTaskNext(it) }
            }
        }
    }

    override fun stop() {
        lifecycleScope.launch {
            task?.cancel()
        }
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

    private fun onTaskNext(value: FractalImage) {
        this.bitmap = value.bitmap
        invalidate()
    }

    private fun onTaskError(e: Throwable) {
        listener?.onRenderFieldCancelled(this)
    }

    private fun onTaskComplete() {
        listener?.onRenderFieldFinished(this)
    }

    private fun onTaskStart() {
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

    private fun invalidate() {
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
                bitmap = rotated.scale(width, height)
                if (rotated !== bitmap) rotated.recycle()
            }
        } else {
            bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
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