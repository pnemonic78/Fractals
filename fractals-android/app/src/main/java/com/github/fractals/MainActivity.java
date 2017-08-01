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
package com.github.fractals;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Main activity.
 *
 * @author Moshe Waisberg
 */
public class MainActivity extends Activity implements
        View.OnTouchListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener,
        FractalsListener {

    private static final int REQUEST_SAVE = 1;

    private FractalsView fractalsView;
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private AsyncTask saveTask;
    private MenuItem menuStop;
    private float scrollXViewing, scrollYViewing;
    private float zoomViewing = 1f;
    private boolean scrolling;
    private boolean scaling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fractalsView = (FractalsView) findViewById(R.id.fractals);
        fractalsView.setOnTouchListener(this);
        fractalsView.setElectricFieldsListener(this);

        gestureDetector = new GestureDetector(this, this);
        scaleGestureDetector = new ScaleGestureDetector(this, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fractalsView.cancel();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v == fractalsView) {
            boolean result = scaleGestureDetector.onTouchEvent(event);
            result = gestureDetector.onTouchEvent(event) || result;
            result = result || super.onTouchEvent(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (scrolling) {
                        onScrollEnd();
                    }
                    break;
            }

            return result;
        }
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (scrolling) {
            return false;
        }
        scaling = true;
        zoomViewing = 1f;
        fractalsView.setScaleX(zoomViewing);
        fractalsView.setScaleY(zoomViewing);
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        zoomViewing *= detector.getScaleFactor();
        fractalsView.setScaleX(zoomViewing);
        fractalsView.setScaleY(zoomViewing);
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        fractalsView.setScaleX(1f);
        fractalsView.setScaleY(1f);
        Matrix matrix = fractalsView.getBitmapMatrix();
        matrix.postScale(zoomViewing, zoomViewing);

        fractalsView.restart();
        scaling = false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (scaling) {
            return false;
        }
        scrolling = true;
        scrollXViewing += distanceX;
        scrollYViewing += distanceY;
        fractalsView.scrollTo((int) scrollXViewing, (int) scrollYViewing);
        return true;
    }

    private void onScrollEnd() {
        scrolling = false;
        if (scaling) {
            return;
        }
        fractalsView.cancel();
        fractalsView.scrollTo(0, 0);
        Matrix matrix = fractalsView.getBitmapMatrix();
        matrix.postTranslate(scrollXViewing, scrollYViewing);
        fractalsView.start();
        scrollXViewing = 0f;
        scrollYViewing = 0f;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menuStop = menu.findItem(R.id.menu_stop);
        menuStop.setEnabled(fractalsView.isRendering());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop:
                stop();
                return true;
            case R.id.menu_fullscreen:
                if (getActionBar().isShowing()) {
                    showFullscreen();
                } else {
                    hideFullscreen();
                }
                return true;
            case R.id.menu_save_file:
                saveToFile();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Save the bitmap to a file.
     */
    private void saveToFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Activity activity = MainActivity.this;
            if (activity.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_SAVE);
                return;
            }
        }

        // Busy saving?
        if ((saveTask != null) && (saveTask.getStatus() == AsyncTask.Status.RUNNING)) {
            return;
        }
        saveTask = new SaveFileTask(this).execute(fractalsView.getBitmap());
    }

    @Override
    public void onRenderFieldPan(FractalsView view, int dx, int dy) {
    }

    @Override
    public void onRenderFieldZoom(FractalsView view, double scale) {
    }

    @Override
    public void onRenderFieldStarted(FractalsView view) {
        if (view == fractalsView) {
            if (menuStop != null) {
                menuStop.setEnabled(view.isRendering());
            }
        }
    }

    @Override
    public void onRenderFieldFinished(FractalsView view) {
        if (view == fractalsView) {
            if (menuStop != null) {
                menuStop.setEnabled(false);
            }
            Toast.makeText(this, R.string.finished, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRenderFieldCancelled(FractalsView view) {
        if (view == fractalsView) {
            if (menuStop != null) {
                menuStop.setEnabled(false);
            }
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_SAVE) {
            if ((permissions.length > 0) && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    saveToFile();
                    return;
                }
            }
        }
    }

    /**
     * Maximise the image in fullscreen mode.
     *
     * @return {@code true} if screen is now fullscreen.
     */
    private boolean showFullscreen() {
        ActionBar actionBar = getActionBar();
        if ((actionBar != null) && actionBar.isShowing()) {
            // Hide the status bar.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }

            // Hide the action bar.
            actionBar.hide();
            return true;
        }
        return false;
    }

    /**
     * Restore the image to non-fullscreen mode.
     *
     * @return {@code true} if screen was fullscreen.
     */
    private boolean hideFullscreen() {
        ActionBar actionBar = getActionBar();
        if ((actionBar != null) && !actionBar.isShowing()) {
            // Show the status bar.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                View decorView = getWindow().getDecorView();
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }

            // Show the action bar.
            actionBar.show();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (hideFullscreen()) {
            return;
        }
        super.onBackPressed();
    }

    private void stop() {
        fractalsView.cancel();
        fractalsView.clear();

        if (saveTask != null) {
            saveTask.cancel(true);
        }
    }

    private void start() {
        fractalsView.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        start();
    }
}
