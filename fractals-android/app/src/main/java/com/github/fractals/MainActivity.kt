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

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast

/**
 * Main activity.
 * @author Moshe Waisberg
 */
class MainActivity : Activity(),
        View.OnTouchListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener,
        FractalsListener {

    private val REQUEST_SAVE = 1

    private var fractalsView: FractalsView? = null
    private var gestureDetector: GestureDetector? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var saveTask: AsyncTask<*, *, *>? = null
    private var menuStop: MenuItem? = null
    private var scrollXViewing = 0f
    private var scrollYViewing = 0f
    private var zoomViewing = 1f
    private var scrolling: Boolean = false
    private var scaling: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fractalsView = findViewById(R.id.fractals) as FractalsView
        fractalsView!!.setOnTouchListener(this)
        fractalsView!!.setElectricFieldsListener(this)

        gestureDetector = GestureDetector(this, this)
        scaleGestureDetector = ScaleGestureDetector(this, this)
    }

    override fun onDestroy() {
        super.onDestroy()
        fractalsView!!.cancel()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (v === fractalsView) {
            var result = scaleGestureDetector!!.onTouchEvent(event)
            result = gestureDetector!!.onTouchEvent(event) || result
            result = result || super.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> if (scrolling) {
                    onScrollEnd()
                }
            }

            return result
        }
        return false
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
        fractalsView!!.scaleX = zoomViewing
        fractalsView!!.scaleY = zoomViewing
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        zoomViewing *= detector.scaleFactor
        fractalsView!!.scaleX = zoomViewing
        fractalsView!!.scaleY = zoomViewing
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        fractalsView!!.scaleX = 1f
        fractalsView!!.scaleY = 1f
        val matrix = fractalsView!!.bitmapMatrix
        matrix.postScale(zoomViewing, zoomViewing)

        fractalsView!!.restart()
        scaling = false
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (scaling) {
            return false
        }
        scrolling = true
        scrollXViewing += distanceX
        scrollYViewing += distanceY
        fractalsView!!.scrollTo(scrollXViewing.toInt(), scrollYViewing.toInt())
        return true
    }

    private fun onScrollEnd() {
        scrolling = false
        if (scaling) {
            return
        }
        fractalsView!!.cancel()
        fractalsView!!.scrollTo(0, 0)
        val matrix = fractalsView!!.bitmapMatrix
        matrix.postTranslate(scrollXViewing, scrollYViewing)
        fractalsView!!.start()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        menuStop = menu.findItem(R.id.menu_stop)
        menuStop!!.isEnabled = fractalsView!!.isRendering

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_stop -> {
                stop()
                return true
            }
            R.id.menu_fullscreen -> {
                if (actionBar!!.isShowing) {
                    showFullscreen()
                } else {
                    hideFullscreen()
                }
                return true
            }
            R.id.menu_save_file -> {
                saveToFile()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Save the bitmap to a file.
     */
    private fun saveToFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activity = this@MainActivity
            if (activity.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_SAVE)
                return
            }
        }

        // Busy saving?
        if ((saveTask != null) && (saveTask!!.status == AsyncTask.Status.RUNNING)) {
            return
        }
        saveTask = SaveFileTask(this).execute(fractalsView!!.getBitmap())
    }

    override fun onRenderFieldPan(view: FractalsView, dx: Int, dy: Int) {
    }

    override fun onRenderFieldZoom(view: FractalsView, scale: Double) {
    }

    override fun onRenderFieldStarted(view: FractalsView) {
        if (view == fractalsView) {
            if (menuStop != null) {
                menuStop!!.isEnabled = view.isRendering
            }
        }
    }

    override fun onRenderFieldFinished(view: FractalsView) {
        if (view == fractalsView) {
            if (menuStop != null) {
                menuStop!!.isEnabled = false
            }
            Toast.makeText(this, R.string.finished, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRenderFieldCancelled(view: FractalsView) {
        if (view == fractalsView) {
            if (menuStop != null) {
                menuStop!!.isEnabled = false
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_SAVE) {
            if (permissions.isNotEmpty() && (permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    saveToFile()
                    return
                }
            }
        }
    }

    /**
     * Maximise the image in fullscreen mode.
     * @return `true` if screen is now fullscreen.
     */
    private fun showFullscreen(): Boolean {
        val actionBar = actionBar
        if (actionBar != null && actionBar.isShowing) {
            // Hide the status bar.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                val decorView = window.decorView
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
            }

            // Hide the action bar.
            actionBar.hide()
            return true
        }
        return false
    }

    /**
     * Restore the image to non-fullscreen mode.
     * @return `true` if screen was fullscreen.
     */
    private fun hideFullscreen(): Boolean {
        val actionBar = actionBar
        if (actionBar != null && !actionBar.isShowing) {
            // Show the status bar.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                val decorView = window.decorView
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }

            // Show the action bar.
            actionBar.show()
            return true
        }
        return false
    }

    override fun onBackPressed() {
        if (hideFullscreen()) {
            return
        }
        super.onBackPressed()
    }

    private fun stop() {
        fractalsView!!.cancel()
        fractalsView!!.clear()

        if (saveTask != null) {
            saveTask!!.cancel(true)
        }
    }

    private fun start() {
        fractalsView!!.start()
    }

    override fun onResume() {
        super.onResume()
        start()
    }
}
