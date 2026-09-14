package com.adaptive.launcher.data.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdaptiveNotificationListener : NotificationListenerService() {
    @Inject lateinit var repo: NotificationRepository
    override fun onNotificationPosted(sbn: StatusBarNotification?) { if(sbn!=null) try{ repo.update(sbn)}catch(_:Exception){} }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { if(sbn!=null) try{ repo.remove(sbn)}catch(_:Exception){} }
}
