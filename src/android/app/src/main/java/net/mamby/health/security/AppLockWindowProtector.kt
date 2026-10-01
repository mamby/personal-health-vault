package net.mamby.health.security

import android.view.Window
import android.view.WindowManager

object AppLockWindowProtector {
    fun setScreenshotsAllowed(window: Window, allowed: Boolean) {
        if (allowed) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            protect(window)
        }
    }

    fun protect(window: Window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
