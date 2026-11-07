package vn.familytv.parentalcontrol.timelock

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.util.Log
import android.view.WindowManager
import vn.familytv.parentalcontrol.MainActivity
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore
import vn.familytv.parentalcontrol.security.SecurityStore

class TimeLockOverlayManager(
    private val service: AccessibilityService,
    private val screenTimeStore: ScreenTimeStore,
    private val securityStore: SecurityStore
) {
    companion object {
        private const val TAG = "FamilyTime_TimeLock"
    }

    private val windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayLayout: TimeLockOverlayLayout? = null

    fun isShowing(): Boolean = overlayLayout != null

    fun showExtensionSelector() {
        overlayLayout?.showExtensionSelectorView()
    }

    @Suppress("DEPRECATION")
    fun showTimeLock(
        todayHours: Int,
        onExtended: (Int) -> Unit,
        onOpenParentConfig: (() -> Unit)? = null,
        onGoHome: (() -> Unit)? = null
    ) {
        if (overlayLayout != null) return

        val panel = TimeLockOverlayLayout(
            context = service,
            securityStore = securityStore,
            onExtended = { addedSec ->
                dismiss()
                onExtended(addedSec)
            },
            onOpenParentConfig = {
                dismiss()
                if (onOpenParentConfig != null) {
                    onOpenParentConfig()
                } else {
                    val intent = Intent(service, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    service.startActivity(intent)
                }
            },
            onGoHome = {
                dismiss()
                if (onGoHome != null) {
                    onGoHome()
                } else {
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    service.startActivity(homeIntent)
                }
            }
        )
        panel.setTodayLimitHours(todayHours)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            title = "FamilyTime TimeLock"
        }

        try {
            windowManager.addView(panel, params)
            overlayLayout = panel
            Log.i(TAG, "TIMELOCK_OVERLAY_SHOWN")
        } catch (e: Exception) {
            Log.e(TAG, "TIMELOCK_OVERLAY_FAILED", e)
        }
    }

    fun dismiss() {
        overlayLayout?.let { layout ->
            try {
                windowManager.removeView(layout)
                Log.i(TAG, "TIMELOCK_OVERLAY_DISMISSED")
            } catch (e: Exception) {
                Log.e(TAG, "TIMELOCK_DISMISS_FAILED", e)
            }
            overlayLayout = null
        }
    }
}
