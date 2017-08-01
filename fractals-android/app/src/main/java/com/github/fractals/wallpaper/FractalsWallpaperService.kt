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

import android.service.wallpaper.WallpaperService
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import java.util.*

/**
 * Fractals wallpaper service.
 * @author Moshe Waisberg
 */
class FractalsWallpaperService : WallpaperService() {

    override fun onCreateEngine(): WallpaperService.Engine {
        return FractalsWallpaperEngine()
    }

    /**
     * Fractals wallpaper engine.
     * @author moshe.w
     */
    protected inner class FractalsWallpaperEngine : WallpaperService.Engine(),
            GestureDetector.OnGestureListener,
            GestureDetector.OnDoubleTapListener,
            WallpaperListener {

        /**
         * Enough time for user to admire the wallpaper before starting the next rendition.
         */
        private val DELAY = 10L * DateUtils.SECOND_IN_MILLIS

        private var fieldsView: WallpaperView? = null
        private var gestureDetector: GestureDetector? = null
        private val random = Random()
        private var drawing: Boolean = false

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)

            val context = this@FractalsWallpaperService
            fieldsView = WallpaperView(context, this)

            gestureDetector = GestureDetector(context, this)
        }

        override fun onDestroy() {
            super.onDestroy()
            fieldsView!!.cancel()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            fieldsView!!.setSize(width, height)
            randomise()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            fieldsView!!.cancel()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            draw()
        }

        override fun onTouchEvent(event: MotionEvent) {
            gestureDetector!!.onTouchEvent(event)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                fieldsView!!.start()
            } else {
                fieldsView!!.cancel()
            }
        }

        /**
         * Add random charges.
         * @param delay the start delay, in milliseconds.
         */
        private fun randomise(delay: Long = 0L) {
            fieldsView!!.clear()
            val x = (if (random.nextBoolean()) +0.25f else -0.25f) * random.nextFloat() * fieldsView!!.width.toFloat()
            val y = (if (random.nextBoolean()) +0.25f else -0.25f) * random.nextFloat() * fieldsView!!.height.toFloat()
            val z = Math.max(0.25f, random.nextFloat() * 5f)
            val matrix = fieldsView!!.bitmapMatrix
            matrix.preTranslate(x, y)
            matrix.postScale(z, z)
            fieldsView!!.restart(delay)
        }

        override fun onRenderFieldStarted(view: WallpaperView) {}

        override fun onRenderFieldFinished(view: WallpaperView) {
            if (view === fieldsView) {
                randomise(DELAY)
            }
        }

        override fun onRenderFieldCancelled(view: WallpaperView) {}

        override fun onDraw(view: WallpaperView) {
            if (view === fieldsView) {
                draw()
            }
        }

        fun draw() {
            if (drawing) {
                return
            }
            drawing = true
            val surfaceHolder = surfaceHolder
            if (surfaceHolder.surface.isValid) {
                try {
                    val canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        fieldsView!!.draw(canvas)
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
            drawing = false
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
   }
}
