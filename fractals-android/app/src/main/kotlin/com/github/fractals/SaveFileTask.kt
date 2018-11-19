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
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.util.Log
import com.github.reactivex.DefaultDisposable
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Task to save a bitmap to a file.
 *
 * @author Moshe Waisberg
 */
class SaveFileTask(private val context: Context, private val bitmap: Bitmap) : Observable<Uri>(), Disposable {

    companion object {
        const val IMAGE_MIME = "image/png"
    }

    private lateinit var runner: SaveFileRunner

    override fun subscribeActual(observer: Observer<in Uri>) {
        val d = SaveFileRunner(context, bitmap, observer)
        runner = d
        observer.onSubscribe(d)
        d.run()
    }

    override fun isDisposed(): Boolean {
        return runner.isDisposed
    }

    override fun dispose() {
        runner.dispose()
    }

    private class SaveFileRunner(val context: Context, val bitmap: Bitmap, val observer: Observer<in Uri>) : DefaultDisposable() {

        private val timestampFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

        fun run() {
            val folderPictures = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)
            val folder = File(folderPictures, context.getString(R.string.app_folder_pictures))
            folder.mkdirs()
            val file = File(folder, generateFileName())

            var url: Uri? = null
            var out: OutputStream? = null
            val mutex = Object()
            try {
                out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                Log.i(TAG, "save success: $file")
                url = Uri.fromFile(file)
            } catch (e: Exception) {
                Log.e(TAG, "save failed: $file", e)
                observer.onError(e)
            } finally {
                if (out != null) {
                    try {
                        out.close()
                    } catch (ignore: Exception) {
                    }
                }
            }
            if (url != null) {
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(IMAGE_MIME), { path: String, uri: Uri? ->
                    if ((uri != null) && (SCHEME_FILE != uri.scheme)) {
                        url = uri
                        observer.onNext(uri)
                    }
                    synchronized(mutex) {
                        mutex.notify()
                    }
                })
                synchronized(mutex) {
                    mutex.wait()
                }
            }

            if (!isDisposed) {
                observer.onComplete()
            }
        }

        override fun onDispose() {
        }

        fun generateFileName(): String {
            return "fractal-" + timestampFormat.format(Date()) + IMAGE_EXT
        }

        companion object {
            private const val TAG = "SaveFileTask"

            private const val IMAGE_EXT = ".png"
            private const val SCHEME_FILE = "file"
        }
    }
}
