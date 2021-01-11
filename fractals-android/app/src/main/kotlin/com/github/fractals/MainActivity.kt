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
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
class MainActivity : Activity(),
    FractalsListener {

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
        disposables.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val rendering = !mainView.isIdle()

        menuStop = menu.findItem(R.id.menu_stop)
        menuStop!!.isVisible = rendering

        menuSave = menu.findItem(R.id.menu_save_file)
        menuSave!!.isVisible = rendering

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
                    showNormalScreen()
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
            val activity: Activity = this@MainActivity
            if (activity.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_SAVE)
                return
            }
        }

        // Busy saving?
        val menuItem = menuSave ?: return
        if (!menuItem.isVisible) {
            return
        }
        menuItem.isVisible = false

        val context: Context = this
        val bitmap = mainView.bitmap
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
                menuStop?.isVisible = true
                menuSave?.isVisible = true
            }
        }
    }

    override fun onRenderFieldFinished(view: Fractals) {
        if (view == mainView) {
            runOnUiThread {
                menuStop?.isVisible = false
                menuSave?.isVisible = true
                Toast.makeText(this, R.string.finished, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRenderFieldCancelled(view: Fractals) {
        if (view == mainView) {
            runOnUiThread {
                menuStop?.isVisible = false
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
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

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
    private fun showNormalScreen(): Boolean {
        val actionBar = actionBar
        if (actionBar != null && !actionBar.isShowing) {
            // Show the status bar.
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE

            // Show the action bar.
            actionBar.show()
            return true
        }
        return false
    }

    override fun onBackPressed() {
        if (showNormalScreen()) {
            return
        }
        super.onBackPressed()
    }

    private fun stop() {
        menuStop?.isVisible = false
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

    companion object {
        private const val REQUEST_SAVE = 0x5473 // "SAVE"
    }
}
