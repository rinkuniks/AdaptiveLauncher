package com.adaptive.launcher.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class LockAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { instance = this; super.onServiceConnected() }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    fun tryLock(): Boolean = try { performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) } catch (_: Exception) { false }
    companion object {
        var instance: LockAccessibilityService? = null
            private set
        fun isEnabled(context: android.content.Context): Boolean {
            val enabled = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_accessibility_services") ?: return false
            return enabled.contains(context.packageName)
        }
    }
}
