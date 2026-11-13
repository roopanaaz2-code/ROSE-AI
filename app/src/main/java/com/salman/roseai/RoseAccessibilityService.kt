package com.salman.roseai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Optional, user-enabled service for explicit navigation commands.
 * It does not inspect, collect, store, or transmit screen text.
 */
class RoseAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        activeService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not read view text or capture screen content.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (activeService === this) activeService = null
        super.onDestroy()
    }

    private fun perform(action: String): Boolean {
        return when (action) {
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "scroll_up" -> dispatchScroll(down = false)
            "scroll_down" -> dispatchScroll(down = true)
            "swipe_left" -> dispatchHorizontalSwipe(toLeft = true)
            "swipe_right" -> dispatchHorizontalSwipe(toLeft = false)
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "quick_settings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "screenshot" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) else false
            else -> false
        }
    }

    private fun dispatchScroll(down: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val metrics = resources.displayMetrics
        val x = metrics.widthPixels * 0.5f
        val startY = metrics.heightPixels * if (down) 0.78f else 0.32f
        val endY = metrics.heightPixels * if (down) 0.32f else 0.78f
        val path = Path().apply {
            moveTo(x, startY)
            lineTo(x, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        return dispatchGesture(gesture, null, null)
    }


    private fun dispatchHorizontalSwipe(toLeft: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val metrics = resources.displayMetrics
        val y = metrics.heightPixels * 0.5f
        val startX = metrics.widthPixels * if (toLeft) 0.82f else 0.18f
        val endX = metrics.widthPixels * if (toLeft) 0.18f else 0.82f
        val path = Path().apply {
            moveTo(startX, y)
            lineTo(endX, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 350))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    companion object {
        @Volatile private var activeService: RoseAccessibilityService? = null

        fun isEnabledInProcess(): Boolean = activeService != null

        /** Returns true only when Android accepted the requested system action. */
        fun performUserAction(action: String): Boolean =
            activeService?.perform(action) ?: false
    }
}
