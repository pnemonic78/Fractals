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
package com.github.fractals.wallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.service.wallpaper.WallpaperService;
import android.text.format.DateUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.Random;

/**
 * Fractals wallpaper service.
 *
 * @author Moshe Waisberg
 */
public class FractalsWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new FractalsWallpaperEngine();
    }

    /**
     * Fractals wallpaper engine.
     *
     * @author moshe.w
     */
    protected class FractalsWallpaperEngine extends Engine implements
            GestureDetector.OnGestureListener,
            GestureDetector.OnDoubleTapListener,
            WallpaperListener {

        /**
         * Enough time for user to admire the wallpaper before starting the next rendition.
         */
        private static final long DELAY = 10L * DateUtils.SECOND_IN_MILLIS;

        private WallpaperView fieldsView;
        private GestureDetector gestureDetector;
        private final Random random = new Random();
        private boolean drawing;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);

            Context context = FractalsWallpaperService.this;
            fieldsView = new WallpaperView(context, this);

            gestureDetector = new GestureDetector(context, this);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            fieldsView.cancel();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            fieldsView.setSize(width, height);
            randomise();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            fieldsView.cancel();
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            draw();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            gestureDetector.onTouchEvent(event);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                fieldsView.start();
            } else {
                fieldsView.cancel();
            }
        }

        /**
         * Add random charges.
         */
        private void randomise() {
            randomise(0L);
        }

        /**
         * Add random charges.
         *
         * @param delay the start delay, in milliseconds.
         */
        private void randomise(long delay) {
            fieldsView.clear();
            float x = (random.nextBoolean() ? +0.25f : -0.25f) * random.nextFloat() * fieldsView.getWidth();
            float y = (random.nextBoolean() ? +0.25f : -0.25f) * random.nextFloat() * fieldsView.getHeight();
            float z = Math.max(0.25f, random.nextFloat() * 5f);
            Matrix matrix = fieldsView.getBitmapMatrix();
            matrix.preTranslate(x, y);
            matrix.postScale(z, z);
            fieldsView.restart(delay);
        }

        @Override
        public void onRenderFieldStarted(WallpaperView view) {
        }

        @Override
        public void onRenderFieldFinished(WallpaperView view) {
            if (view == fieldsView) {
                randomise(DELAY);
            }
        }

        @Override
        public void onRenderFieldCancelled(WallpaperView view) {
        }

        @Override
        public void onDraw(WallpaperView view) {
            if (view == fieldsView) {
                draw();
            }
        }

        public void draw() {
            if (drawing) {
                return;
            }
            drawing = true;
            SurfaceHolder surfaceHolder = getSurfaceHolder();
            if (surfaceHolder.getSurface().isValid()) {
                try {
                    Canvas canvas = surfaceHolder.lockCanvas();
                    if (canvas != null) {
                        fieldsView.draw(canvas);
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
            drawing = false;
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
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
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
    }
}
