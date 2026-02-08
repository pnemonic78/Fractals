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

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Task to save a bitmap to a file.
 *
 * @author Moshe Waisberg
 */
class SaveFileTask(private val context: Context, private val bitmap: Bitmap) : Flow<Uri> {

    override suspend fun collect(collector: FlowCollector<Uri>) {
        val runner = SaveFileRunner(context, bitmap, collector)
        runner.run()
    }

    private class SaveFileRunner(
        val context: Context,
        val bitmap: Bitmap,
        val collector: FlowCollector<Uri>
    ) {
        suspend fun run() {
            saveContent(context, bitmap, collector)
        }

        private suspend fun saveContent(
            context: Context,
            bitmap: Bitmap,
            collector: FlowCollector<Uri>
        ) {
            val path =
                DIRECTORY_PICTURES + File.separator + context.getString(R.string.app_folder_pictures)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, generateFileName())
                put(MediaStore.Images.Media.MIME_TYPE, IMAGE_MIME)
                put(MediaStore.Images.Media.RELATIVE_PATH, path)
                put(MediaStore.MediaColumns.WIDTH, bitmap.width)
                put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val cr = context.contentResolver
            var uri: Uri? = null
            try {
                uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri == null) {
                    Timber.e("save failed: %s", uri)
                    return
                }

                cr.openOutputStream(uri)!!.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)

                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    cr.update(uri, values, null, null)
                    Timber.i("save success: %s %s", bitmap, uri)

                    collector.emit(uri)
                }
            } catch (e: Exception) {
                Timber.e(e, "save failed: %s", uri)
                throw e
            }
        }

        fun generateFileName(): String {
            return "fractal-" + timestampFormat.format(Date()) + IMAGE_EXT
        }

        companion object {
            private val timestampFormat = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        }
    }

    companion object {
        const val IMAGE_MIME = "image/png"

        private const val IMAGE_EXT = ".png"
    }
}
