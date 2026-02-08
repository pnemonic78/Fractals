package com.github.fractals

import android.app.Application
import timber.log.Timber

class FractalsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}