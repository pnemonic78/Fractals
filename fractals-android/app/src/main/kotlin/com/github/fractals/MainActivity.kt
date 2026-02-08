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
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
class MainActivity : ComponentActivity(), FractalsListener {

    private lateinit var mainView: FractalsView
    private var menuStop: MenuItem? = null
    private var menuShare: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mainView = findViewById(R.id.fractals)
        mainView.setFractalsListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mainView.stop()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val rendering = !mainView.isIdle()

        menuStop = menu.findItem(R.id.menu_stop).also {
            it.isVisible = rendering
        }

        menuShare = menu.findItem(R.id.menu_share)?.also {
            it.isEnabled = rendering
        }

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

            R.id.menu_share -> {
                val bitmap = mainView.bitmap ?: return false
                share(bitmap = bitmap)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Save the bitmap to a file, and then share it.
     */
    private fun share(bitmap: Bitmap) {
        // Busy sharing?
        val menuItem = menuShare ?: return
        if (!menuItem.isEnabled || !menuItem.isVisible) {
            return
        }
        menuItem.isEnabled = false

        val context: Context = this
        lifecycleScope.launch {
            SaveFileTask(context, bitmap)
                .flowOn(Dispatchers.IO)
                .catch { e -> onSaveFile(context, e, menuItem) }
                .flowOn(Dispatchers.Main)
                .collect { uri -> onSaveFile(uri, menuItem) }
        }
    }

    override fun onRenderFieldPan(view: Fractals, dx: Int, dy: Int) = Unit

    override fun onRenderFieldZoom(view: Fractals, scale: Double) = Unit

    override fun onRenderFieldStarted(view: Fractals) {
        if (view == mainView) {
            runOnUiThread {
                menuStop?.isVisible = true
                menuShare?.isVisible = false
            }
        }
    }

    override fun onRenderFieldFinished(view: Fractals) {
        if (view == mainView) {
            runOnUiThread {
                menuStop?.isVisible = false
                menuShare?.isVisible = true
                menuShare?.isEnabled = true
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

    /**
     * Maximise the image in fullscreen mode.
     * @return `true` if screen is now fullscreen.
     */
    private fun showFullscreen(): Boolean {
        val actionBar = actionBar
        if ((actionBar != null) && actionBar.isShowing) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).also { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
    private fun showNormalScreen(): Boolean {
        val actionBar = actionBar
        if (actionBar != null && !actionBar.isShowing) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, window.decorView).also { controller ->
                controller.show(WindowInsetsCompat.Type.systemBars())
            }

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

    private fun onSaveFile(uri: Uri, menuItem: MenuItem) {
        Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, uri)
            type = SaveFileTask.IMAGE_MIME
            startActivity(Intent.createChooser(this, getString(R.string.share_title)));
        }
        menuItem.isEnabled = true
    }

    private fun onSaveFile(context: Context, error: Throwable, menuItem: MenuItem) {
        error.printStackTrace()
        Toast.makeText(context, R.string.share_failed, Toast.LENGTH_LONG).show()
        menuItem.isEnabled = true
    }
}
