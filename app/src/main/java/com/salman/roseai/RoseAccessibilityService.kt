package com.salman.roseai

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/** Optional service. The user must enable it manually in Android Settings. */
class RoseAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* Explicit actions only. */ }
    override fun onInterrupt() = Unit
}
