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
import android.graphics.Point
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.*
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

/**
 * Fractals view.
 *
 * @author Moshe Waisberg
 */
class FractalsView : View,
    Fractals,
    Observer<Bitmap>,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    ScaleGestureDetector.OnScaleGestureListener {

    private val size: Point by lazy {
        val sizeValue = Point()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // include navigation bar
            display.getRealSize(sizeValue)
        } else {
            // exclude navigation bar
            display.getSize(sizeValue)
        }
        sizeValue
    }

    var bitmap: Bitmap? = null
        get() {
            val size = this.size
            val width = size.x
            val height = size.y

            val bitmapOld = field
            if (bitmapOld != null) {
                val bw = bitmapOld.width
                val bh = bitmapOld.height

                if ((width != bw) || (height != bh)) {
                    val m = Matrix()
                    // Changed orientation?
                    if (width < bw && height > bh) {// Portrait?
                        m.postRotate(90f, bw / 2f, bh / 2f)
                    } else {// Landscape?
                        m.postRotate(270f, bw / 2f, bh / 2f)
                    }
                    val rotated = Bitmap.createBitmap(bitmapOld, 0, 0, bw, bh, m, true)
                    bitmap = Bitmap.createScaledBitmap(rotated, width, height, true)
                }
            } else {
                field = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }
            return field
        }
    private var task: FractalTask? = null
    private var listener: FractalsListener? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var scaleGestureDetector: ScaleGestureDetector
    private var scrollXViewing = 0f
    private var scrollYViewing = 0f
    private var zoomViewing = 1f
    private var scrolling: Boolean = false
    private var scaling: Boolean = false
    /**
     * The matrix for the bitmap.
     */
    private val bitmapMatrix = Matrix()
    private var measuredWidthDiff = 0f
    private var measuredHeightDiff = 0f

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        gestureDetector = GestureDetector(context, this)
        scaleGestureDetector = ScaleGestureDetector(context, this)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val b = bitmap ?: return
        measuredWidthDiff = (w - b.width) / 2f
        measuredHeightDiff = (h - b.height) / 2f
    }

    override fun clear() {
        bitmapMatrix.reset()
    }

    override fun onDraw(canvas: Canvas) {
        val b = bitmap ?: return
        canvas.drawBitmap(b, measuredWidthDiff, measuredHeightDiff, null)
    }

    override fun start(delay: Long) {
        if (isIdle()) {
            val observer = this
            val t = FractalTask(bitmapMatrix, bitmap!!)
            task = t
            t.startDelay = delay
            t.subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer)
        }
    }

    override fun stop() {
        task?.cancel()
    }

    override fun onNext(value: Bitmap) {
        postInvalidate()
    }

    override fun onError(e: Throwable) {
        listener?.onRenderFieldCancelled(this)
    }

    override fun onComplete() {
        listener?.onRenderFieldFinished(this)
        clear()
    }

    override fun onSubscribe(d: Disposable) {
        listener?.onRenderFieldStarted(this)
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

        super.onRestoreInstanceState(state.superState)

        if (state.values != null) {
            clear()
            val matrix = bitmapMatrix
            matrix.setValues(state.values)
            restart()
        }
    }

    /**
     * Is the task idle and not rendering the fields?
     * @return `true` if idle.
     */
    fun isIdle(): Boolean = (task == null) || task!!.isIdle()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (scrolling || (event.pointerCount <= 1)) {
            gestureDetector.onTouchEvent(event)
        } else {
            scaleGestureDetector.onTouchEvent(event)
        }
        super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> if (scrolling) {
                onScrollEnd()
            }
        }

        return true
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

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        if (scrolling) {
            return false
        }
        scaling = true
        zoomViewing = 1f
        this.scaleX = zoomViewing
        this.scaleY = zoomViewing
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        zoomViewing *= detector.scaleFactor
        this.scaleX = zoomViewing
        this.scaleY = zoomViewing
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        this.scaleX = 1f
        this.scaleY = 1f
        val matrix = bitmapMatrix
        matrix.postScale(zoomViewing, zoomViewing)

        restart()
        scaling = false
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (scaling) {
            return false
        }
        scrolling = true
        scrollXViewing += distanceX
        scrollYViewing += distanceY
        scrollTo(scrollXViewing.toInt(), scrollYViewing.toInt())
        return true
    }

    private fun onScrollEnd() {
        scrolling = false
        if (scaling) {
            return
        }
        stop()
        scrollTo(0, 0)
        val matrix = bitmapMatrix
        matrix.postTranslate(scrollXViewing, scrollYViewing)
        start()
        scrollXViewing = 0f
        scrollYViewing = 0f
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    class SavedState : View.BaseSavedState {

        internal var values: FloatArray? = null

        private constructor(source: Parcel) : super(source) {
            source.readFloatArray(values)
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeFloatArray(values)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }
}
