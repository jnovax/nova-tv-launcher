package vn.familytv.parentalcontrol.platform

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar
import vn.familytv.parentalcontrol.MainActivity
import vn.familytv.parentalcontrol.audio.AudioFocusInterrupter
import vn.familytv.parentalcontrol.ipc.Mode
import vn.familytv.parentalcontrol.ipc.ModeBroadcastSender
import vn.familytv.parentalcontrol.ipc.ModeStateManager
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore
import vn.familytv.parentalcontrol.screentime.ScreenTimeTracker
import vn.familytv.parentalcontrol.security.SecurityStore
import vn.familytv.parentalcontrol.timelock.TimeLockOverlayManager

/**
 * ForegroundMonitorService monitors foreground package switches, enforces
 * daily screen time limits in KID mode, pauses third-party audio, and presents
 * a time-lock overlay when the limit is reached.
 */
class ForegroundMonitorService : AccessibilityService() {
    companion object {
        const val TAG = "FAMILY_TV"
        private const val PREFS_NAME = "familytv_screentime"
        const val ACTION_SET_SCREENTIME = "vn.familytv.parentalcontrol.ACTION_SET_SCREENTIME"
        const val ACTION_TRIGGER_TIMELOCK = "vn.familytv.parentalcontrol.ACTION_TRIGGER_TIMELOCK"
        const val ACTION_TRIGGER_QUIZ = "vn.familytv.parentalcontrol.ACTION_TRIGGER_QUIZ"

        var connected = false
            private set
    }

    private lateinit var screenTimeStore: ScreenTimeStore
    private lateinit var securityStore: SecurityStore
    private lateinit var policy: ScreenTimePolicy
    private lateinit var tracker: ScreenTimeTracker
    private lateinit var audioFocusInterrupter: AudioFocusInterrupter
    private lateinit var modeBroadcastSender: ModeBroadcastSender
    private lateinit var timeLockOverlayManager: TimeLockOverlayManager

    private val modeManager = ModeStateManager.instance
    private val handler = Handler(Looper.getMainLooper())
    private var isReceiverRegistered = false
    private var isDebugReceiverRegistered = false
    private var currentForegroundPackage: String? = null
    private var lastSavedUsedSec: Int = -1

    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_SET_SCREENTIME -> {
                    if (::tracker.isInitialized && ::screenTimeStore.isInitialized) {
                        val calendar = Calendar.getInstance()
                        val totalLimit = screenTimeStore.getTotalLimitSec(calendar, policy)
                        val targetUsed: Int
                        val remaining: Int
                        if (intent.hasExtra("used_min")) {
                            targetUsed = intent.getIntExtra("used_min", 0) * 60
                            remaining = (totalLimit - targetUsed).coerceAtLeast(0)
                        } else if (intent.hasExtra("used_sec")) {
                            targetUsed = intent.getIntExtra("used_sec", 0).coerceAtLeast(0)
                            remaining = (totalLimit - targetUsed).coerceAtLeast(0)
                        } else {
                            remaining = intent.getIntExtra("remaining_sec", 5)
                            targetUsed = (totalLimit - remaining).coerceAtLeast(0)
                        }
                        Log.i(TAG, "DEBUG_SET_SCREENTIME used=$targetUsed, remaining=$remaining")
                        tracker.restoreUsedAndLimit(used = targetUsed, limit = totalLimit)
                        modeManager.setScreenTimeRemainingSec(remaining)
                        lastSavedUsedSec = targetUsed
                        screenTimeStore.setUsedSec(targetUsed)

                        if (remaining > 0 && ::timeLockOverlayManager.isInitialized && timeLockOverlayManager.isShowing()) {
                            timeLockOverlayManager.dismiss()
                            audioFocusInterrupter.releaseFocus()
                            modeManager.setQuizActive(false)
                        } else if (remaining <= 0 && modeManager.getMode() == Mode.KID) {
                            onLimitReached()
                        }
                        broadcastState()
                    }
                }
                ACTION_TRIGGER_TIMELOCK, ACTION_TRIGGER_QUIZ -> {
                    val showExtension = intent.getBooleanExtra("show_extension", false)
                    Log.i(TAG, "DEBUG_TRIGGER_TIMELOCK showExtension=$showExtension")
                    onLimitReached()
                    if (showExtension && ::timeLockOverlayManager.isInitialized) {
                        timeLockOverlayManager.showExtensionSelector()
                    }
                }
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(TAG, "SCREEN_OFF")
                    tracker.setScreenOn(false)
                    modeManager.onScreenOff()
                    if (::screenTimeStore.isInitialized && ::tracker.isInitialized) {
                        lastSavedUsedSec = tracker.getUsedSec()
                        screenTimeStore.setUsedSec(lastSavedUsedSec)
                    }
                    broadcastState()
                }
                Intent.ACTION_SCREEN_ON -> {
                    Log.i(TAG, "SCREEN_ON")
                    tracker.setScreenOn(true)
                    broadcastState()
                }
            }
        }
    }

    private val modeListener: (Mode) -> Unit = { mode ->
        tracker.setMode(mode)
        if (mode == Mode.PARENT) {
            audioFocusInterrupter.releaseFocus()
            if (::timeLockOverlayManager.isInitialized) {
                timeLockOverlayManager.dismiss()
            }
            modeManager.setQuizActive(false)
        }
        broadcastState()
    }

    private val tickerRunnable = object : Runnable {
        override fun run() {
            val nowMs = System.currentTimeMillis()
            modeManager.tick(nowMs)

            val calendar = Calendar.getInstance()
            // Midnight rollover check
            if (::screenTimeStore.isInitialized && screenTimeStore.checkAndResetDay(calendar, policy)) {
                val newLimit = screenTimeStore.getTotalLimitSec(calendar, policy)
                tracker.restoreUsedAndLimit(used = 0, limit = newLimit)
                modeManager.setScreenTimeRemainingSec(newLimit)
                lastSavedUsedSec = 0
                if (::timeLockOverlayManager.isInitialized && timeLockOverlayManager.isShowing()) {
                    timeLockOverlayManager.dismiss()
                    audioFocusInterrupter.releaseFocus()
                    modeManager.setQuizActive(false)
                }
                broadcastState()
            } else if (::screenTimeStore.isInitialized) {
                // Ensure tracker has latest limit configured by parent
                val currentTotalLimit = screenTimeStore.getTotalLimitSec(calendar, policy)
                tracker.setTotalLimitSec(currentTotalLimit)
            }

            tracker.setMode(modeManager.getMode())
            tracker.tickSecond()
            val remaining = tracker.getRemainingSec()
            val used = tracker.getUsedSec()
            modeManager.setScreenTimeRemainingSec(remaining)

            // Periodically save state (every 10 seconds of active usage increment)
            if (used != lastSavedUsedSec && used % 10 == 0) {
                lastSavedUsedSec = used
                screenTimeStore.setUsedSec(used)
            }

            if (remaining <= 0 && modeManager.getMode() == Mode.KID && !timeLockOverlayManager.isShowing()) {
                val pkg = currentForegroundPackage
                if (pkg != null && !policy.excludedPackages.contains(pkg)) {
                    onLimitReached()
                }
            }

            handler.postDelayed(this, 1000L)
        }
    }

    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        screenTimeStore = ScreenTimeStore(prefs)
        securityStore = SecurityStore.fromContext(this)
        policy = ScreenTimePolicy()
        audioFocusInterrupter = AudioFocusInterrupter(this)
        modeBroadcastSender = ModeBroadcastSender(this)
        timeLockOverlayManager = TimeLockOverlayManager(this, screenTimeStore, securityStore)

        tracker = ScreenTimeTracker(policy = policy) {
            onLimitReached()
        }

        val calendar = Calendar.getInstance()
        val totalDailyLimit = screenTimeStore.getTotalLimitSec(calendar, policy)
        val savedUsedSec = screenTimeStore.getUsedSec()
        tracker.restoreUsedAndLimit(used = savedUsedSec, limit = totalDailyLimit)
        val initialRemaining = tracker.getRemainingSec()
        modeManager.setScreenTimeRemainingSec(initialRemaining)
        lastSavedUsedSec = savedUsedSec

        modeManager.addListener(modeListener)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        isReceiverRegistered = true

        val debugFilter = IntentFilter().apply {
            addAction(ACTION_SET_SCREENTIME)
            addAction(ACTION_TRIGGER_TIMELOCK)
            addAction(ACTION_TRIGGER_QUIZ)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(debugReceiver, debugFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(debugReceiver, debugFilter)
        }
        isDebugReceiverRegistered = true

        handler.post(tickerRunnable)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
        Log.i(TAG, "MONITOR_CONNECTED")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            Log.d(TAG, "FOREGROUND package=$pkg")
            currentForegroundPackage = pkg
            if (::tracker.isInitialized) {
                tracker.setForegroundPackage(pkg)
            }
        }
    }

    private fun onLimitReached() {
        Log.i(TAG, "SCREEN_TIME_LIMIT_REACHED")
        showTimeLockOverlay()
    }

    private fun showTimeLockOverlay() {
        if (timeLockOverlayManager.isShowing()) return
        audioFocusInterrupter.acquireFocus()
        modeManager.setQuizActive(true)
        val calendar = Calendar.getInstance()
        val todayHours = screenTimeStore.getDailyLimitHours(calendar, policy)
        timeLockOverlayManager.showTimeLock(
            todayHours = todayHours,
            onExtended = { addedSec ->
                audioFocusInterrupter.releaseFocus()
                screenTimeStore.addExtensionSec(addedSec)
                val newLimit = screenTimeStore.getTotalLimitSec(calendar, policy)
                tracker.setTotalLimitSec(newLimit)
                val newRemaining = tracker.getRemainingSec()
                modeManager.setScreenTimeRemainingSec(newRemaining)
                modeManager.setQuizActive(false)
                broadcastState()
            },
            onOpenParentConfig = {
                audioFocusInterrupter.releaseFocus()
                modeManager.setQuizActive(false)
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
        )
        broadcastState()
    }

    private fun broadcastState() {
        if (!::modeBroadcastSender.isInitialized) return
        val now = System.currentTimeMillis()
        modeBroadcastSender.sendModeChanged(
            mode = modeManager.getMode(),
            sessionRemainingSec = modeManager.getSessionRemainingSec(now),
            isQuizActive = timeLockOverlayManager.isShowing()
        )
    }

    override fun onInterrupt() {
        Log.i(TAG, "MONITOR_INTERRUPTED")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        connected = false
        Log.i(TAG, "MONITOR_UNBOUND")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        connected = false
        Log.i(TAG, "MONITOR_DESTROYED")

        handler.removeCallbacks(tickerRunnable)

        if (isReceiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            isReceiverRegistered = false
        }

        if (isDebugReceiverRegistered) {
            runCatching { unregisterReceiver(debugReceiver) }
            isDebugReceiverRegistered = false
        }

        modeManager.removeListener(modeListener)

        if (::timeLockOverlayManager.isInitialized) {
            timeLockOverlayManager.dismiss()
        }

        if (::audioFocusInterrupter.isInitialized) {
            audioFocusInterrupter.releaseFocus()
        }

        if (::screenTimeStore.isInitialized && ::tracker.isInitialized) {
            screenTimeStore.setUsedSec(tracker.getUsedSec())
        }

        super.onDestroy()
    }
}
