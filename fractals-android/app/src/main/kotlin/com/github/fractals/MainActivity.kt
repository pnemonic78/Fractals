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
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
class MainActivity : Activity(),
        FractalsListener {

    private val REQUEST_SAVE = 1

    private lateinit var mainView: FractalsView
    private val disposables = CompositeDisposable()
    private var menuStop: MenuItem? = null
    private var menuSave: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainView = findViewById(R.id.fractals)
        mainView.setElectricFieldsListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainView.stop()
        disposables.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val rendering = !mainView.isIdle()

        menuStop = menu.findItem(R.id.menu_stop)
        menuStop!!.isEnabled = rendering
        menuSave = menu.findItem(R.id.menu_save_file)
        menuSave!!.isEnabled = rendering

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_stop -> {
                stop()
                return true
            }
            R.id.menu_fullscreen -> {
                if (actionBar?.isShowing == true) {
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
            if (activity.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_SAVE)
                return
            }
        }

        // Busy saving?
        if ((menuSave == null) || !menuSave!!.isEnabled) {
            return
        }
        menuSave!!.isEnabled = false

        val context = this
        val bitmap = mainView.bitmap!!
        val task = SaveFileTask(context, bitmap)
        task.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(SaveFileObserver(context, bitmap))
        disposables.add(task)
    }

    override fun onRenderFieldPan(view: Fractals, dx: Int, dy: Int) {
    }

    override fun onRenderFieldZoom(view: Fractals, scale: Double) {
    }

    override fun onRenderFieldStarted(view: Fractals) {
        if (view == mainView) {
            runOnUiThread {
                menuStop?.isEnabled = true
                menuSave?.isEnabled = true
            }
        }
    }

    override fun onRenderFieldFinished(view: Fractals) {
        if (view == mainView) {
            runOnUiThread {
                menuStop?.isEnabled = false
                menuSave?.isEnabled = true
                Toast.makeText(this, R.string.finished, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRenderFieldCancelled(view: Fractals) {
        if (view == mainView) {
            runOnUiThread {
                menuStop?.isEnabled = false
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_SAVE) {
            if (permissions.isNotEmpty() && (permissions[0] == Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (grantResults.isNotEmpty() && (grantResults[0] == PERMISSION_GRANTED)) {
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
        if ((actionBar != null) && actionBar.isShowing) {
            // Hide the status bar.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN)
            } else {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
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
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
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
        mainView.stop()
        mainView.clear()
    }

    private fun start() {
        mainView.start()
    }

    override fun onResume() {
        super.onResume()
        start()
    }
}
