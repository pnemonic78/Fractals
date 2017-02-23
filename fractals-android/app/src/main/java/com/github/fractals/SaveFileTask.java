package com.github.fractals;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.content.ContentValues.TAG;

/**
 * Task to save a bitmap to a file.
 *
 * @author moshe.w
 */
public class SaveFileTask extends AsyncTask<Bitmap, File, File> {

    private static final int REQUEST_APP = 0x0466; // "APP"
    private static final int REQUEST_VIEW = 0x7133; // "VIEW"

    private static final int ID_NOTIFY = 0x5473; // "SAVE"

    protected final Context context;
    protected final DateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);

    protected Bitmap bitmap;
    protected Notification.Builder builder;

    public SaveFileTask(Context context) {
        this.context = context;
    }

    @Override
    protected File doInBackground(Bitmap... params) {
        File folderPictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File folder = new File(folderPictures, context.getString(R.string.app_folder_pictures));
        folder.mkdirs();

        Bitmap bitmap = params[0];
        this.bitmap = bitmap;
        File file = new File(folder, generateFileName());

        Resources res = context.getResources();
        int iconWidth = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
        int iconHeight = res.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
        Bitmap largeIcon = Bitmap.createScaledBitmap(bitmap, iconWidth, iconHeight, false);

        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_APP, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new Notification.Builder(context)
                .setContentTitle(context.getText(R.string.saving_title))
                .setContentText(context.getText(R.string.saving_text))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.stat_notify)
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .setOngoing(true);

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID_NOTIFY, notification);

        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.i(TAG, "save success: " + file);
            return file;
        } catch (IOException e) {
            Log.e(TAG, "save failed: " + file, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(File file) {
        builder.setOngoing(false);

        if ((file != null) && (bitmap != null)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "image/png");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_VIEW, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentTitle(context.getText(R.string.saved_title))
                    .setContentText(context.getText(R.string.saved_text))
                    .setContentIntent(pendingIntent);
        } else {
            builder.setContentText(context.getText(R.string.save_failed));
        }

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = builder.getNotification();
        }

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID_NOTIFY, notification);
    }

    @Override
    protected void onCancelled(File file) {
        super.onCancelled(file);
        if (file != null) {
            file.delete();
        } else {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(ID_NOTIFY);
        }
    }

    protected String generateFileName() {
        return "ef-" + timestampFormat.format(new Date()) + ".png";
    }
}
