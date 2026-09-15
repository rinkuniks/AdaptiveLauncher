package com.adaptive.launcher.feature.settings

import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adaptive.launcher.core.device.DeviceProfileFactory
import com.adaptive.launcher.data.backup.BackupRepository
import com.adaptive.launcher.data.notifications.NotificationRepository
import com.adaptive.launcher.data.streams.StreamEntity
import com.adaptive.launcher.data.widgets.WidgetRepository
import com.adaptive.launcher.domain.gestures.GestureRepository
import com.adaptive.launcher.domain.gestures.LauncherAction
import com.adaptive.launcher.domain.gestures.LauncherGesture
import com.adaptive.launcher.domain.profiles.ProfileManager
import com.adaptive.launcher.domain.streams.StreamRepository
import com.adaptive.launcher.domain.themes.ThemeMode
import com.adaptive.launcher.domain.themes.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUi(
    val deviceLabel: String="",
    val refreshHz: Int=60,
    val swDp: Int=0,
    val layoutMode: String="",
    val profilesLabel: String="",
    val themeMode: ThemeMode=ThemeMode.System,
    val gestureMap: Map<LauncherGesture, LauncherAction> = emptyMap(),
    val streams: List<StreamEntity> = emptyList(),
    val notifications: Map<String, List<com.adaptive.launcher.data.notifications.AppNotification>> = emptyMap(),
    val widgetInfo: String="",
    val widgetIds: List<Int> = emptyList(),
    val backupJson: String? = null,
    val backupStatus: String? = null,
    val hasNotificationAccess: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeRepository: ThemeRepository,
    private val gestureRepository: GestureRepository,
    private val streamRepository: StreamRepository,
    private val notificationRepository: NotificationRepository,
    private val widgetRepository: WidgetRepository,
    private val profileManager: ProfileManager,
    private val backupRepository: BackupRepository,
): ViewModel(){
    private val _backupJson = MutableStateFlow<String?>(null)
    private val _backupStatus = MutableStateFlow<String?>(null)

    val ui: StateFlow<SettingsUi> = combine(
        themeRepository.mode,
        streamRepository.streams,
        notificationRepository.notifications,
        _backupJson,
        _backupStatus,
        combine(LauncherGesture.all.map{ g -> gestureRepository.observe(g).map{ a -> g to a } }){ it.toMap() },
        widgetRepository.widgetIds,
    ){ args ->
        val mode = args[0] as ThemeMode
        val streams = args[1] as List<StreamEntity>
        val notifs = args[2] as Map<String, List<com.adaptive.launcher.data.notifications.AppNotification>>
        val bj = args[3] as String?
        val bs = args[4] as String?
        val gmap = args[5] as Map<LauncherGesture, LauncherAction>
        val wids = args[6] as List<Int>
        val profile = DeviceProfileFactory.fromContext(context)
        val profilesLabel = profileManager.getProfiles().joinToString{ (if(it.isPrivate) "Private" else if(it.isWork) "Work" else "Personal") + "#" + it.serial }
        val enabled = try {
            val s = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            s.contains(context.packageName)
        } catch (_: Exception) { false }
        SettingsUi(
            deviceLabel = profile.label,
            refreshHz = profile.refreshRate.toInt(),
            swDp = profile.swDp,
            layoutMode = if(profile.isTablet) "Tablet" else if(profile.isTall) "TallPhone" else "Compact",
            profilesLabel = profilesLabel.ifEmpty{ "single profile"},
            themeMode = mode,
            gestureMap = gmap,
            streams = streams,
            notifications = notifs,
            widgetInfo = "host ready; ${try{ AppWidgetManager.getInstance(context).installedProviders.size }catch(_:Exception){0}} providers; ${wids.size} pinned",
            widgetIds = wids,
            backupJson = bj,
            backupStatus = bs,
            hasNotificationAccess = enabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUi())

    fun setTheme(m: ThemeMode){ viewModelScope.launch{ themeRepository.setMode(m)} }
    fun cycleGesture(g: LauncherGesture){
        viewModelScope.launch{
            val cur = gestureRepository.observe(g).first()
            val next = when(cur){
                LauncherAction.None -> LauncherAction.OpenSearch
                LauncherAction.OpenSearch -> LauncherAction.OpenAppList
                LauncherAction.OpenAppList -> LauncherAction.OpenSettings
                LauncherAction.OpenSettings -> LauncherAction.OpenNotifications
                LauncherAction.OpenNotifications -> LauncherAction.None
                is LauncherAction.LaunchApp -> LauncherAction.None
            }
            gestureRepository.set(g, next)
        }
    }
    fun setGestureDirect(g: LauncherGesture, a: LauncherAction){ viewModelScope.launch{ gestureRepository.set(g, a) } }
    fun createStream(name: String){ viewModelScope.launch{ streamRepository.create(name)} }
    fun deleteStream(id: String){ viewModelScope.launch{ streamRepository.delete(id)} }
    fun moveStream(id: String, dir: Int){
        viewModelScope.launch{
            val list = streamRepository.streams.first().toMutableList()
            val idx = list.indexOfFirst{ it.id==id }
            if(idx==-1) return@launch
            val to = (idx+dir).coerceIn(0, list.size-1)
            if(to==idx) return@launch
            val item = list.removeAt(idx)
            list.add(to, item)
            streamRepository.reorder(list.map{ it.id })
        }
    }
    fun onWidgetPicked(id: Int){ viewModelScope.launch{ if(id!=-1) widgetRepository.addWidgetId(id) } }
    fun removeWidget(id: Int){ viewModelScope.launch{ widgetRepository.removeWidgetId(id) } }
    fun requestWidgetPicker(ctx: Context){
        try{
            val id = widgetRepository.allocateId()
            if(id==-1) return
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply{ putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id) }
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }catch(_:Exception){}
    }
    fun setBackupJson(json: String){ _backupJson.value = json; _backupStatus.value = null }
    suspend fun exportBackup(){ _backupJson.value = backupRepository.export(); _backupStatus.value = "Exported schemaVersion 1 (${_backupJson.value?.length ?: 0} chars)" }
    suspend fun importBackup(){ val json = _backupJson.value ?: return; val ok = backupRepository.importBackup(json); _backupStatus.value = if(ok) "Imported OK" else "Import failed - invalid JSON" }
    suspend fun refresh(){
        _backupStatus.value = "Refreshed"
        // re-evaluate notification access by triggering a combine re-emit (touch _backupStatus)
    }
}
