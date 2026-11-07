package vn.familytv.parentalcontrol.platform

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.provider.Settings
import vn.familytv.parentalcontrol.onboarding.PermissionSnapshot

class PermissionReader(private val context: Context) {
    @Suppress("DEPRECATION")
    fun read(): PermissionSnapshot {
        val resolver = context.contentResolver
        val target = ComponentName(context, ForegroundMonitorService::class.java)
        val accessibilityEnabled = Settings.Secure.getInt(
            resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0,
        ) == 1 && Settings.Secure.getString(
            resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().split(':').any { ComponentName.unflattenFromString(it) == target }
        val ops = context.getSystemService(AppOpsManager::class.java)
        val usageAccess = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
        ) == AppOpsManager.MODE_ALLOWED
        return PermissionSnapshot(accessibilityEnabled, ForegroundMonitorService.connected, usageAccess)
    }
}
