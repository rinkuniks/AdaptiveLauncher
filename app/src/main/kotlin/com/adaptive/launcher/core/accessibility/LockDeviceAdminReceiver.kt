package com.adaptive.launcher.core.accessibility

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class LockDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {}
    override fun onDisabled(context: Context, intent: Intent) {}
}
