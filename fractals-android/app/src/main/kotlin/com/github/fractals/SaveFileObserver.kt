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
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.support.v4.app.NotificationCompat
import io.reactivex.Observer
import io.reactivex.disposables.Disposable
import io.reactivex.internal.disposables.DisposableHelper

/**
 * Task to save a bitmap to a file.
 *
 * @author Moshe Waisberg
 */
class SaveFileObserver(val context: Context, val bitmap: Bitmap) : Observer<Uri> {

    private val REQUEST_APP = 0x0466 // "APP"
    private val REQUEST_VIEW = 0x7133 // "VIEW"

    private val ID_NOTIFY = 0x5473 // "SAVE"

    private val CHANNEL_ID = "save_file"

    private val IMAGE_MIME = SaveFileTask.IMAGE_MIME

    private var disposable: Disposable? = null
    private val nm: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    override fun onSubscribe(d: Disposable) {
        if (DisposableHelper.validate(this.disposable, d)) {
            this.disposable = d
            onStart()
        }
    }

    private fun onStart() {
        val res = context.resources
        val iconWidth = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        val iconHeight = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
        val largeIcon = Bitmap.createScaledBitmap(bitmap, iconWidth, iconHeight, false)

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(context, REQUEST_APP, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            initChannels(nm)
        }
        builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentTitle(context.getText(R.string.saving_title))
                .setContentText(context.getText(R.string.saving_text))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.stat_notify)
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .setOngoing(true)

        nm.notify(ID_NOTIFY, builder.build())
    }

    override fun onNext(value: Uri) {
        builder.setOngoing(false)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(value, IMAGE_MIME)
        val pendingIntent = PendingIntent.getActivity(context, REQUEST_VIEW, intent, FLAG_UPDATE_CURRENT)

        builder.setContentTitle(context.getText(R.string.saved_title))
                .setContentText(context.getText(R.string.saved_text))
                .setContentIntent(pendingIntent)

        nm.notify(ID_NOTIFY, builder.build())
    }

    override fun onError(e: Throwable) {
        builder.setOngoing(false)
        builder.setContentText(context.getText(R.string.save_failed))

        nm.notify(ID_NOTIFY, builder.build())
    }

    override fun onComplete() {
        finish()
    }

    /**
     * Cancels the upstream's disposable.
     */
    private fun finish() {
        val s = this.disposable
        this.disposable = DisposableHelper.DISPOSED
        s?.dispose()
    }

    fun cancel() {
        finish()
        nm.cancel(ID_NOTIFY)
    }

    @TargetApi(VERSION_CODES.O)
    private fun initChannels(nm: NotificationManager) {
        var channel = nm.getNotificationChannel(CHANNEL_ID)
        if (channel == null) {
            channel = android.app.NotificationChannel(CHANNEL_ID, context.getString(R.string.saving_title), NotificationManager.IMPORTANCE_DEFAULT)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(channel)
        }
    }
}