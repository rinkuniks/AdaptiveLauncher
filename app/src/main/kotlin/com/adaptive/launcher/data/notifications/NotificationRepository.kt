package com.adaptive.launcher.data.notifications

import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppNotification(val packageName: String, val title: String?, val text: String?, val whenMs: Long)

@Singleton
class NotificationRepository @Inject constructor() {
    private val _notifications = MutableStateFlow<Map<String, List<AppNotification>>>(emptyMap())
    val notifications: StateFlow<Map<String, List<AppNotification>>> = _notifications
    fun update(sbn: StatusBarNotification){
        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()
        val cur = _notifications.value.toMutableMap()
        val list = cur[pkg]?.toMutableList() ?: mutableListOf()
        list.removeAll{ it.title==title && it.text==text }
        list.add(0, AppNotification(pkg, title, text, sbn.postTime))
        if(list.size>5) list.subList(5, list.size).clear()
        cur[pkg]=list
        _notifications.value = cur
    }
    fun remove(sbn: StatusBarNotification){
        val pkg = sbn.packageName
        val cur = _notifications.value.toMutableMap()
        cur.remove(pkg)
        _notifications.value = cur
    }
}
