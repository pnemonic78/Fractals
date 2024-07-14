package com.github.fractals

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.View
import android.view.ViewParent
import android.view.WindowManager

fun View.findDisplay(): Display {
    var display = this.display
    if (display != null) return display

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display = context.display
        if (display != null) return display
    }

    var parent: ViewParent? = this.parent
    while (parent != null) {
        if (parent is View) {
            return parent.findDisplay()
        }
        parent = parent.parent
    }

    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    @Suppress("DEPRECATION")
    return windowManager.defaultDisplay
}