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
import android.service.wallpaper.WallpaperService
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.lifecycleScope
import com.github.fractals.Fractals
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.random.Random

/**
 * Fractals wallpaper service.
 *
 * @author Moshe Waisberg
 */
class FractalsWallpaperService : WallpaperService(), LifecycleOwner {

    private val dispatcher = ServiceLifecycleDispatcher(this)

    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    override val lifecycle: Lifecycle
        get() = dispatcher.lifecycle

    override fun onCreateEngine(): Engine {
        return FractalsWallpaperEngine()
    }

    /**
     * Fractals wallpaper engine.
     * @author Moshe Waisberg
     */
    private inner class FractalsWallpaperEngine : Engine(), WallpaperListener {

        private lateinit var mainView: WallpaperView
        private val random = Random.Default
        private val isDrawing = AtomicBoolean()

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)

            val context: Context = this@FractalsWallpaperService
            mainView = WallpaperView(context, lifecycleScope, this)
        }

        override fun onDestroy() {
            super.onDestroy()
            mainView.stop()
            mainView.onDestroy()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            mainView.setSize(width, height)
            randomise()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            mainView.stop()
            mainView.onDestroy()
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            draw(mainView)
        }

        override fun onTouchEvent(event: MotionEvent) {
            mainView.onTouchEvent(event)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                mainView.start()
            } else {
                mainView.stop()
            }
        }

        /**
         * Add random zoom and pan.
         * @param delay the start delay, in milliseconds.
         */
        private fun randomise(delay: Long = 0L) {
            mainView.clear()
            val f = random.nextDouble(-0.25, 0.25).toFloat()
            val x = f * mainView.width.toFloat()
            val y = f * mainView.height.toFloat()
            val z = max(0.5f, f * 100f)
            val matrix = mainView.bitmapMatrix
            matrix.preTranslate(x, y)
            matrix.postScale(z, z)
            mainView.restart(delay)
        }

        override fun onRenderFieldPan(view: Fractals, dx: Int, dy: Int) {
            // Pan not relevant to wallpaper.
        }

        override fun onRenderFieldZoom(view: Fractals, scale: Double) {
            // Zoom not relevant to wallpaper.
        }

        override fun onRenderFieldStarted(view: Fractals) = Unit

        override fun onRenderFieldFinished(view: Fractals) {
            if (view === mainView) {
                randomise(DELAY)
            }
        }

        override fun onRenderFieldCancelled(view: Fractals) = Unit

        override fun onDraw(view: WallpaperView) {
            if (view === mainView) {
                draw(view)
            }
        }

        fun draw(view: WallpaperView) {
            if (!isDrawing.compareAndSet(false, true)) {
                return
            }
            val surfaceHolder = this.surfaceHolder
            if (surfaceHolder.surface.isValid) {
                try {
                    val canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        try {
                            view.draw(canvas)
                        } finally {
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                }
            }
            isDrawing.set(false)
        }
    }

    companion object {
        /**
         * Enough time for user to admire the wallpaper before starting the next rendition.
         */
        private const val DELAY = 10L * DateUtils.SECOND_IN_MILLIS
    }
}
