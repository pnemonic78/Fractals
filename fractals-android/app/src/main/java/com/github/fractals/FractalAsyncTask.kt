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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.AsyncTask
import android.os.SystemClock
import android.util.Log

/**
 * Fractals task.
 * @author Moshe Waisberg
 */
class FractalAsyncTask(private val listener: FieldAsyncTaskListener, private val canvas: Canvas) : AsyncTask<Matrix, Canvas, Canvas>() {

    private val TAG = "FractalAsyncTask"

    interface FieldAsyncTaskListener {
        /**
         * Notify the listener that the task has started processing the charges.

         * @param task the caller task.
         */
        fun onTaskStarted(task: FractalAsyncTask)

        /**
         * Notify the listener that the task has finished.

         * @param task the caller task.
         */
        fun onTaskFinished(task: FractalAsyncTask)

        /**
         * Notify the listener that the task has aborted.

         * @param task the caller task.
         */
        fun onTaskCancelled(task: FractalAsyncTask)

        /**
         * Notify the listener to repaint its bitmap.

         * @param task the caller task.
         */
        fun repaint(task: FractalAsyncTask)
    }

    private val RE_MIN = -2.0
    private val RE_MAX = -RE_MIN
    private val RE_SIZE = RE_MAX - RE_MIN
    private val IM_MIN = RE_MIN
    private val IM_MAX = -IM_MIN
    private val IM_SIZE = IM_MAX - IM_MIN

    private val LOG2 = Math.log(2.0)
    private val LOG2_LOG2 = Math.log(LOG2) / LOG2
    private val LOG2_LOG2_2 = 2.0 + LOG2_LOG2
    private val OVERFLOW = 300

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val hsv = floatArrayOf(0f, 1f, 1f)
    private var startDelay = 0L
    private var zoom = 1.0
    private var scrollX = 0.0
    private var scrollY = 0.0

    override fun onPreExecute() {
        super.onPreExecute()

        paint.strokeCap = Paint.Cap.SQUARE
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 1f

        listener.onTaskStarted(this)

        scrollX = 0.0
        scrollY = 0.0
        zoom = 1.0
    }

    override fun doInBackground(vararg params: Matrix): Canvas? {
        if (startDelay > 0L) {
            try {
                Thread.sleep(startDelay)
            } catch (ignore: InterruptedException) {
            }
        }
        val timeStart = SystemClock.elapsedRealtime()
        if (params.size >= 1) {
            val matrix = params[0]
            val matrixValues = FloatArray(9)
            matrix.getValues(matrixValues)
            scrollX = matrixValues[Matrix.MTRANS_X].toDouble()
            scrollY = matrixValues[Matrix.MTRANS_Y].toDouble()
            zoom = matrixValues[Matrix.MSCALE_X].toDouble()
        }

        val w = canvas.width
        val h = canvas.height
        var sizeMax = Math.max(w, h)
        val sizeMin = Math.min(w, h)
        val sizeRe = if (h >= w) sizeMin.toDouble() else sizeMin * RE_SIZE / IM_SIZE
        val sizeIm = if (h >= w) sizeMin * IM_SIZE / RE_SIZE else sizeMin.toDouble()
        val sizeSetRe = sizeRe * zoom / RE_SIZE
        val sizeSetIm = sizeIm * zoom / IM_SIZE
        val offsetRe = scrollX + Math.min(0.0, (sizeRe - w) / 2.0)
        val offsetIm = scrollY + Math.min(0.0, (sizeIm - h) / 2.0)

        var shifts = 0
        while (sizeMax > 1) {
            sizeMax = sizeMax ushr 1
            shifts++
        }
        val density = 1e+1

        // Make "resolution2" a power of 2, so that "resolution" is always divisible by 2.
        var resolution2 = 1 shl shifts
        var resolution = resolution2

        canvas.drawColor(Color.WHITE)

        var dxRe = resolution / sizeSetRe
        var dyIm = resolution / sizeSetIm
        var dxRe2 = resolution2 / sizeSetRe
        var dyIm2 = resolution2 / sizeSetIm
        val x0Re = offsetRe / sizeSetRe + RE_MIN / zoom
        val y0Im = offsetIm / sizeSetIm + IM_MIN / zoom

        plotMandelbrot(canvas, 0, 0, resolution, resolution, x0Re, y0Im, density)
        listener.repaint(this)

        var x1: Int
        var y1: Int
        var x2: Int
        var y2: Int
        var x1Re: Double
        var y1Im: Double
        var x2Re: Double
        var y2Im: Double

        do {
            y1 = 0
            y2 = resolution
            y1Im = y0Im
            y2Im = y1Im + dyIm

            while (y1 < h) {
                x1 = 0
                x2 = resolution
                x1Re = x0Re
                x2Re = x1Re + dxRe

                while (x1 < w) {
                    plotMandelbrot(canvas, x1, y2, resolution, resolution, x1Re, y2Im, density)
                    plotMandelbrot(canvas, x2, y1, resolution, resolution, x2Re, y1Im, density)
                    plotMandelbrot(canvas, x2, y2, resolution, resolution, x2Re, y2Im, density)

                    x1 += resolution2
                    x2 += resolution2
                    x1Re += dxRe2
                    x2Re += dxRe2
                    if (isCancelled) {
                        return null
                    }
                }
                listener.repaint(this)

                y1 += resolution2
                y2 += resolution2
                y1Im += dyIm2
                y2Im += dyIm2
                if (isCancelled) {
                    return null
                }
            }

            resolution2 = resolution
            resolution = resolution2 shr 1
            dxRe = resolution / sizeSetRe
            dyIm = resolution / sizeSetIm
            dxRe2 = resolution2 / sizeSetRe
            dyIm2 = resolution2 / sizeSetIm
            if (isCancelled) {
                return null
            }
        } while (resolution >= 1)

        val timeEnd = SystemClock.elapsedRealtime()
        Log.v(TAG, "Rendered in " + (timeEnd - timeStart) + "ms")

        return canvas
    }

    override fun onProgressUpdate(vararg values: Canvas) {
        super.onProgressUpdate(*values)
        listener.repaint(this)
    }

    override fun onPostExecute(result: Canvas) {
        super.onPostExecute(result)
        listener.onTaskFinished(this)
    }

    override fun onCancelled() {
        super.onCancelled()
        listener.onTaskCancelled(this)
    }

    /**
     * Plot a Mandelbrot point.
     * <br/>
     * `z := z * z + c`
     * <br/>
     * http://en.wikipedia.org/wiki/Mandelbrot_set
     */
    private fun plotMandelbrot(canvas: Canvas, x: Int, y: Int, w: Int, h: Int, kRe: Double, kIm: Double, density: Double) {
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
            z += LOG2_LOG2_2 - Math.log(Math.log(d)) / LOG2
        } else {
            z = 0.0
        }

        paint.color = mapColor(z, density)
        rect.set(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
        canvas.drawRect(rect, paint)
    }

    private fun mapColor(c: Double, density: Double): Int {
        if (c == 0.0) {
            return Color.BLACK
        }
        hsv[0] = (c * density % 360).toFloat()
        return Color.HSVToColor(hsv)
    }

    /**
     * Set the HSV saturation.
     * @param value a value between [0..1] inclusive.
     */
    fun setSaturation(value: Float) {
        hsv[1] = value
    }

    /**
     * Set the HSV brightness.
     * @param value a value between [0..1] inclusive.
     */
    fun setBrightness(value: Float) {
        hsv[2] = value
    }

    /**
     * Set the start delay.
     * @param delay the start delay, in milliseconds.
     */
    fun setStartDelay(delay: Long) {
        startDelay = delay
    }
}
