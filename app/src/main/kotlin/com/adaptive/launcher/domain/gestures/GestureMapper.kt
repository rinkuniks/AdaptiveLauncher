package com.adaptive.launcher.domain.gestures

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureMapper @Inject constructor(@ApplicationContext private val context: Context){
    fun perform(action: LauncherAction, onOpenSearch: ()->Unit = {}, onOpenAppList: ()-> Unit = {}) {
        when(action){
            LauncherAction.None -> Unit
            LauncherAction.OpenSearch -> onOpenSearch()
            LauncherAction.OpenAppList -> onOpenAppList()
            LauncherAction.OpenSettings -> try{ context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }catch(_:Exception){}
            LauncherAction.OpenNotifications -> try{ context.startActivity(Intent("android.intent.action.MAIN").apply{ addCategory("android.intent.category.NOTIFICATION_PREFERENCES"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)}) }catch(_:Exception){}
            is LauncherAction.LaunchApp -> try{ context.startActivity(context.packageManager.getLaunchIntentForPackage(action.packageName)?.apply{ addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)}) }catch(_:Exception){}
        }
    }
}
