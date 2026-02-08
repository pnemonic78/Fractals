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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.os.SystemClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import timber.log.Timber
import java.lang.Thread.sleep
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

/**
 * Fractals task.
 * @author Moshe Waisberg
 */
class FractalTask(
    private val bitmap: Bitmap,
    private val matrix: Matrix,
    private val hues: Double = DEFAULT_HUES
) : Flow<FractalImage> {

    private var runner: FractalRunner? = null
    var brightness = 1f
        set(value) {
            field = value
            runner?.let { it.brightness = value }
        }
    var saturation = 1f
        set(value) {
            field = value
            runner?.let { it.saturation = value }
        }
    var startDelay = 0L
        set(value) {
            field = value
            runner?.let { it.startDelay = value }
        }

    override suspend fun collect(collector: FlowCollector<FractalImage>) {
        runner = FractalRunner(matrix, bitmap, hues, collector).apply {
            this@FractalTask.brightness = brightness
            this@FractalTask.saturation = saturation
            this@FractalTask.startDelay = startDelay
            run()
        }
    }

    private class FractalRunner(
        val matrix: Matrix,
        val bitmap: Bitmap,
        val hues: Double,
        val collector: FlowCollector<FractalImage>
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val hsv = floatArrayOf(0f, 1f, 1f)
        var startDelay = 0L
        var running = false
            private set

        var saturation: Float
            get() = hsv[1]
            set(value) {
                hsv[1] = value
            }
        var brightness: Float
            get() = hsv[2]
            set(value) {
                hsv[2] = value
            }

        private var zoom = 1.0
        private var scrollX = 0.0
        private var scrollY = 0.0

        init {
            with(paint) {
                strokeCap = Paint.Cap.SQUARE
                style = Paint.Style.FILL
                strokeWidth = 1f
            }
        }

        suspend fun run() {
            running = true
            if (startDelay > 0L) {
                try {
                    sleep(startDelay)
                } catch (_: InterruptedException) {
                }
            }
            collector.emit(FractalImage(bitmap, Int.MAX_VALUE))

            val timeStart = SystemClock.elapsedRealtime()
            val matrixValues = FloatArray(9)
            matrix.getValues(matrixValues)
            scrollX = matrixValues[Matrix.MTRANS_X].toDouble()
            scrollY = matrixValues[Matrix.MTRANS_Y].toDouble()
            zoom = matrixValues[Matrix.MSCALE_X].toDouble()

            val w = bitmap.width
            val h = bitmap.height
            var sizeMax = max(w, h)
            val sizeMin = min(w, h)
            val sizeRe = if (h >= w) sizeMin.toDouble() else sizeMin * RE_SIZE / IM_SIZE
            val sizeIm = if (h >= w) sizeMin * IM_SIZE / RE_SIZE else sizeMin.toDouble()
            val sizeSetRe = sizeRe * zoom / RE_SIZE
            val sizeSetIm = sizeIm * zoom / IM_SIZE
            val offsetRe = scrollX + min(0.0, (sizeRe - w) / 2.0)
            val offsetIm = scrollY + min(0.0, (sizeIm - h) / 2.0)

            var shifts = 0
            while (sizeMax > 1) {
                sizeMax = sizeMax ushr 1
                shifts++
            }
            val density = 1e+1

            // Make "resolution2" a power of 2, so that "resolution" is always divisible by 2.
            var resolution2 = 1 shl shifts
            var resolution = resolution2
            var resF = resolution.toFloat()

            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            var dxRe = resolution / sizeSetRe
            var dyIm = resolution / sizeSetIm
            var dxRe2 = resolution2 / sizeSetRe
            var dyIm2 = resolution2 / sizeSetIm
            val x0Re = offsetRe / sizeSetRe + RE_MIN / zoom
            val y0Im = offsetIm / sizeSetIm + IM_MIN / zoom

            plotMandelbrot(canvas, 0f, 0f, resF, resF, x0Re, y0Im, density)

            var x1: Float
            var y1: Float
            var x2: Float
            var y2: Float
            var x1Re: Double
            var y1Im: Double
            var x2Re: Double
            var y2Im: Double

            loop@ do {
                y1 = 0f
                y2 = resF
                y1Im = y0Im
                y2Im = y1Im + dyIm

                do {
                    x1 = 0f
                    x2 = resF
                    x1Re = x0Re
                    x2Re = x1Re + dxRe

                    do {
                        plotMandelbrot(canvas, x1, y2, resF, resF, x1Re, y2Im, density)
                        plotMandelbrot(canvas, x2, y1, resF, resF, x2Re, y1Im, density)
                        plotMandelbrot(canvas, x2, y2, resF, resF, x2Re, y2Im, density)

                        x1 += resolution2
                        x2 += resolution2
                        x1Re += dxRe2
                        x2Re += dxRe2
                    } while ((x1 < w) && !isDisposed())

                    if (isDisposed()) {
                        break@loop
                    }
                    collector.emit(FractalImage(bitmap, resolution))

                    y1 += resolution2
                    y2 += resolution2
                    y1Im += dyIm2
                    y2Im += dyIm2
                } while (y1 < h)

                resolution2 = resolution
                resolution = resolution2 shr 1
                resF = resolution.toFloat()
                dxRe = resolution / sizeSetRe
                dyIm = resolution / sizeSetIm
                dxRe2 = resolution2 / sizeSetRe
                dyIm2 = resolution2 / sizeSetIm
            } while ((resolution >= 1) && !isDisposed())

            running = false
            if (!isDisposed()) {
                collector.emit(FractalImage(bitmap, resolution))
            }

            val timeEnd = SystemClock.elapsedRealtime()
            Timber.v("Rendered in %dms", timeEnd - timeStart)
        }

        /**
         * Plot a Mandelbrot point.
         * <br/>
         * `z := z * z + c`
         * <br/>
         * http://en.wikipedia.org/wiki/Mandelbrot_set
         */
        private fun plotMandelbrot(
            canvas: Canvas,
            x1: Float,
            y1: Float,
            w: Float,
            h: Float,
            kRe: Double,
            kIm: Double,
            density: Double
        ) {
            var zRe = 0.0
            var zIm = 0.0
            var zReSrq = 0.0
            var zImSrq = 0.0
            var d: Double
            var r: Double
            var i = 0
            var underflow: Boolean

            do {
                r = zReSrq - zImSrq + kRe
                zIm = 2.0 * zRe * zIm + kIm
                zRe = r
                zReSrq = zRe * zRe
                zImSrq = zIm * zIm
                d = zReSrq + zImSrq
                i++
                underflow = i < OVERFLOW
            } while (underflow && d < 9)

            var z = i.toDouble()
            if (underflow) {
                z += LOG2_LOG2_2 - ln(ln(d)) / LOG2
            } else {
                z = 0.0
            }

            paint.color = mapColor(z, density)
            canvas.drawRect(x1, y1, x1 + w, y1 + h, paint)
        }

        private fun mapColor(z: Double, density: Double): Int {
            if (z == 0.0) {
                return Color.BLACK
            }
            hsv[0] = ((z * density) % hues).toFloat()
            return Color.HSVToColor(hsv)
        }

        suspend fun isDisposed(): Boolean {
            val job = currentCoroutineContext()[Job] ?: return true
            return job.isCancelled || job.isCompleted || !job.isActive
        }

        suspend fun dispose() {
            val job = currentCoroutineContext()[Job] ?: return
            job.cancel()
        }

        companion object {
            private const val RE_MIN = -2.0
            private const val RE_MAX = -RE_MIN
            private const val RE_SIZE = RE_MAX - RE_MIN
            private const val IM_MIN = RE_MIN
            private const val IM_MAX = -IM_MIN
            private const val IM_SIZE = IM_MAX - IM_MIN

            private val LOG2 = ln(2.0)
            private val LOG2_LOG2 = ln(LOG2) / LOG2
            private val LOG2_LOG2_2 = 2.0 + LOG2_LOG2
            private const val OVERFLOW = 300
        }
    }

    companion object {
        const val DEFAULT_HUES = 360.0
    }
}
