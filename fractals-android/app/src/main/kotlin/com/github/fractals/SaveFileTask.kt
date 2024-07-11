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

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.Log
import com.github.reactivex.DefaultDisposable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Task to save a bitmap to a file.
 *
 * @author Moshe Waisberg
 */
class SaveFileTask(private val context: Context, private val bitmap: Bitmap) : Observable<Uri>(),
    Disposable {

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

    private class SaveFileRunner(
        val context: Context,
        val bitmap: Bitmap,
        val observer: Observer<in Uri>
    ) : DefaultDisposable() {

        fun run() {
            saveContent(context, bitmap, observer)
        }

        @TargetApi(Build.VERSION_CODES.Q)
        private fun saveContent(context: Context, bitmap: Bitmap, observer: Observer<in Uri>) {
            val path =
                DIRECTORY_PICTURES + File.separator + context.getString(R.string.app_folder_pictures)

            val values = ContentValues()
            values.put(MediaStore.Images.Media.DISPLAY_NAME, generateFileName())
            values.put(MediaStore.Images.Media.MIME_TYPE, IMAGE_MIME)
            values.put(MediaStore.Images.Media.RELATIVE_PATH, path)
            values.put(MediaStore.MediaColumns.WIDTH, bitmap.width)
            values.put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)

            val cr = context.contentResolver
            var uri: Uri? = null
            try {
                uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri == null) {
                    Log.e(TAG, "save failed: $uri")
                    return
                }

                cr.openOutputStream(uri)!!.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)

                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    cr.update(uri, values, null, null)
                    Log.i(TAG, "save success: $bitmap $uri")

                    if (!isDisposed) {
                        observer.onNext(uri)
                        observer.onComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "save failed: $uri", e)
                observer.onError(e)
                return
            }
        }

        override fun onDispose() {
        }

        fun generateFileName(): String {
            return "fractal-" + timestampFormat.format(Date()) + IMAGE_EXT
        }

        companion object {
            private const val TAG = "SaveFileTask"

            private val timestampFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        }
    }

    companion object {
        const val IMAGE_MIME = "image/png"

        private const val IMAGE_EXT = ".png"
    }
}
