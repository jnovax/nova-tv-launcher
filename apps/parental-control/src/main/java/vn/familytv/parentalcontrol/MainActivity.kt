package vn.familytv.parentalcontrol

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import vn.familytv.parentalcontrol.onboarding.AccessibilityState
import vn.familytv.parentalcontrol.onboarding.PermissionSnapshot
import vn.familytv.parentalcontrol.platform.PermissionReader
import vn.familytv.parentalcontrol.security.AuthMode
import vn.familytv.parentalcontrol.security.GeneratedRecoverySet
import vn.familytv.parentalcontrol.security.ParentSessionManager
import vn.familytv.parentalcontrol.security.RateLimiter
import vn.familytv.parentalcontrol.security.RecoveryCodeManager
import vn.familytv.parentalcontrol.security.SecurityStore
import vn.familytv.parentalcontrol.security.TotpEngine
import vn.familytv.parentalcontrol.ui.TvViews
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView
import vn.familytv.parentalcontrol.ui.auth.QrCodeRenderer
import vn.familytv.parentalcontrol.ui.auth.RecoveryCodesView
import java.util.Calendar
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy
import vn.familytv.parentalcontrol.screentime.ScreenTimeStore
import vn.familytv.parentalcontrol.ipc.Mode
import vn.familytv.parentalcontrol.ipc.ModeBroadcastSender
import vn.familytv.parentalcontrol.ipc.ModeStateManager

class MainActivity : Activity() {
    private lateinit var reader: PermissionReader
    private lateinit var views: TvViews
    private lateinit var securityStore: SecurityStore
    private lateinit var screenTimeStore: ScreenTimeStore
    private val screenTimePolicy = ScreenTimePolicy()

    private val rateLimiter = RateLimiter()
    private val sessionManager = ParentSessionManager()
    private val broadcastSender by lazy { ModeBroadcastSender(this) }

    private var renderedState: Any? = null
    private var renderedParentMode: Boolean? = null

    private var activePinPad: DpadPinPadView? = null
    private var isRecoveryMode: Boolean = false
    private var verifyErrorMessage: String? = null

    private var liveTimerView: TextView? = null
    private var verifyStatusView: TextView? = null

    private var isChangingPin: Boolean = false
    private var isEnrollingTotp: Boolean = false
    private var isViewingRecoveryInParentMode: Boolean = false

    private var pendingSecret: String? = null
    private var pendingRecoverySet: GeneratedRecoverySet? = null

    private val handler = Handler(Looper.getMainLooper())
    private val kidGateTimeoutRunnable = Runnable {
        if (!sessionManager.isParentModeActive()) {
            finishAffinity()
        }
    }

    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateScreen()
            handler.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = TvViews(this)
        reader = PermissionReader(this)
        securityStore = SecurityStore.fromContext(this)
        screenTimeStore = ScreenTimeStore(getSharedPreferences("familytv_screentime", MODE_PRIVATE))

        val modeManager = ModeStateManager.instance
        if (modeManager.getMode() == Mode.PARENT) {
            val remSec = modeManager.getSessionRemainingSec(System.currentTimeMillis())
            if (remSec > 0) {
                sessionManager.startSession(remSec * 1000L)
            }
        }

        if (intent?.getBooleanExtra("debug_enter_parent_mode", false) == true) {
            isChangingPin = false
            isEnrollingTotp = false
            isViewingRecoveryInParentMode = false
            sessionManager.startSession()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val modeManager = ModeStateManager.instance
        if (modeManager.getMode() == Mode.PARENT) {
            val remSec = modeManager.getSessionRemainingSec(System.currentTimeMillis())
            if (remSec > 0) {
                sessionManager.startSession(remSec * 1000L)
                renderedState = null
                updateScreen()
            }
        }
        if (intent?.getBooleanExtra("debug_enter_parent_mode", false) == true) {
            isChangingPin = false
            isEnrollingTotp = false
            isViewingRecoveryInParentMode = false
            sessionManager.startSession()
            renderedState = null
            updateScreen()
        }
    }

    override fun onResume() {
        super.onResume()
        val modeManager = ModeStateManager.instance
        if (modeManager.getMode() == Mode.PARENT && !sessionManager.isParentModeActive()) {
            val remSec = modeManager.getSessionRemainingSec(System.currentTimeMillis())
            if (remSec > 0) {
                sessionManager.startSession(remSec * 1000L)
                renderedState = null
            }
        }
        handler.removeCallbacks(refreshRunnable)
        refreshRunnable.run()
        resetKidGateTimeout()
    }

    override fun onPause() {
        handler.removeCallbacks(refreshRunnable)
        handler.removeCallbacks(kidGateTimeoutRunnable)
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (sessionManager.isParentModeActive()) {
                sessionManager.touchSession()
                ModeStateManager.instance.extendSession(System.currentTimeMillis())
            }
            resetKidGateTimeout()

            activePinPad?.let { pinPad ->
                if (event.keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
                    val digit = '0' + (event.keyCode - KeyEvent.KEYCODE_0)
                    pinPad.appendDigit(digit)
                    return true
                }
                if (event.keyCode == KeyEvent.KEYCODE_DEL) {
                    pinPad.deleteDigit()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (sessionManager.isParentModeActive()) {
            when {
                isChangingPin -> {
                    isChangingPin = false
                    renderedState = null
                    updateScreen()
                }
                isEnrollingTotp -> {
                    isEnrollingTotp = false
                    renderedState = null
                    updateScreen()
                }
                isViewingRecoveryInParentMode -> {
                    isViewingRecoveryInParentMode = false
                    renderedState = null
                    updateScreen()
                }
                else -> {
                    sessionManager.invalidateSession()
                    finishAffinity()
                }
            }
        } else {
            // In Lock Gate -> exit back to TV Home
            finishAffinity()
        }
    }

    private fun resetKidGateTimeout() {
        handler.removeCallbacks(kidGateTimeoutRunnable)
        if (!sessionManager.isParentModeActive()) {
            handler.postDelayed(kidGateTimeoutRunnable, 30_000)
        }
    }

    private fun updateScreen() {
        val permissions = reader.read()
        val isParent = sessionManager.isParentModeActive()
        val configKey = screenTimeStore.getWeekdayHours() to screenTimeStore.getWeekendHours()
        val authMode = securityStore.getAuthMode()
        val isCustomPin = securityStore.isCustomPin()

        val currentState = listOf(
            isParent,
            isChangingPin,
            isEnrollingTotp,
            isViewingRecoveryInParentMode,
            permissions.ready,
            configKey,
            authMode,
            isCustomPin
        )

        if (currentState == renderedState) {
            updateLiveTimers()
            return
        }

        if (renderedParentMode == true && !isParent) {
            ModeStateManager.instance.expireSession()
            broadcastSender.sendModeChanged(Mode.KID, 0, false)
        }

        renderedState = currentState
        renderedParentMode = isParent
        val oldFocus = currentFocus?.id

        if (isParent) {
            when {
                isChangingPin -> renderChangePinScreen()
                isEnrollingTotp -> renderTotpEnrollmentScreen()
                isViewingRecoveryInParentMode -> renderRecoveryCodesScreen()
                else -> renderParentConfiguration(permissions, oldFocus)
            }
        } else {
            renderKidModeGate(oldFocus)
        }
    }

    private fun updateLiveTimers() {
        if (!sessionManager.isParentModeActive()) {
            if (rateLimiter.isLocked()) {
                val sec = rateLimiter.remainingLockSeconds()
                verifyStatusView?.text = getString(R.string.parent_verify_locked, sec)
                verifyStatusView?.visibility = View.VISIBLE
                activePinPad?.setKeypadEnabled(false)
            } else if (verifyStatusView?.text?.toString()?.startsWith("Đã nhập sai") == true) {
                verifyStatusView?.text = ""
                verifyStatusView?.visibility = View.GONE
                activePinPad?.setKeypadEnabled(true)
            }
        } else if (!isChangingPin && !isEnrollingTotp && !isViewingRecoveryInParentMode) {
            val remSec = sessionManager.remainingSessionSeconds()
            val min = remSec / 60
            val sec = remSec % 60
            liveTimerView?.text = getString(R.string.parent_session_remaining, min, sec)
        }
    }

    /**
     * UNIFIED LOCK GATE:
     * Displayed whenever parent mode is inactive.
     * Supports default PIN 000000 (with hint), custom PIN, or Google Authenticator TOTP.
     */
    private fun renderKidModeGate(oldFocus: Int?) {
        liveTimerView = null
        val authMode = securityStore.getAuthMode()
        val isCustomPin = securityStore.isCustomPin()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(views.dp(36), views.dp(12), views.dp(36), views.dp(12))
        }

        // App Logo
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_pctv)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(views.dp(56), views.dp(56)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = views.dp(8)
        })

        // Title & Description
        val titleText = if (authMode == AuthMode.TOTP) {
            if (isRecoveryMode) getString(R.string.enter_recovery_title) else "Xác thực Google Authenticator"
        } else {
            getString(R.string.kid_gate_title)
        }
        root.addView(views.title(titleText).apply {
            textSize = 20f
            gravity = Gravity.CENTER_HORIZONTAL
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, views.dp(2))
        })

        if (authMode == AuthMode.PIN && !isCustomPin) {
            val hintTv = views.text(getString(R.string.default_pin_hint), 15f, R.color.accent_amber).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, views.dp(6))
            }
            root.addView(hintTv)
        } else {
            val bodyText = if (authMode == AuthMode.TOTP) {
                if (isRecoveryMode) getString(R.string.enter_recovery_body) else "Nhập mã 6 chữ số từ ứng dụng Google Authenticator để mở khóa:"
            } else {
                "Nhập mã PIN 6 số của phụ huynh để mở phần Cài đặt:"
            }
            root.addView(views.text(bodyText, 13f, R.color.text_secondary).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, 0, 0, views.dp(6))
            })
        }

        // Status / Error / Lock text
        val statusTv = views.text("", 14f, R.color.warning).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, views.dp(6))
            visibility = View.GONE
        }
        verifyStatusView = statusTv
        root.addView(statusTv)

        if (rateLimiter.isLocked()) {
            val sec = rateLimiter.remainingLockSeconds()
            statusTv.text = getString(R.string.parent_verify_locked, sec)
            statusTv.visibility = View.VISIBLE
        } else if (verifyErrorMessage != null) {
            statusTv.text = verifyErrorMessage
            statusTv.visibility = View.VISIBLE
        }

        // PIN Pad
        val pinPad = DpadPinPadView(this) { enteredPin ->
            handlePinSubmitted(enteredPin)
        }
        activePinPad = pinPad
        pinPad.setKeypadEnabled(!rateLimiter.isLocked())
        root.addView(pinPad)

        // Actions row below PIN Pad
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, views.dp(8), 0, 0)
        }

        views.button(actionRow, getString(R.string.kid_gate_exit), R.id.primary_action) {
            finishAffinity()
        }.apply {
            textSize = 13f
            minimumHeight = views.dp(34)
            setPadding(views.dp(14), views.dp(4), views.dp(14), views.dp(4))
            (layoutParams as? LinearLayout.LayoutParams)?.apply {
                width = LinearLayout.LayoutParams.WRAP_CONTENT
                topMargin = 0
            }
        }

        if (authMode == AuthMode.TOTP) {
            val switchModeLabel = if (isRecoveryMode) "Dùng mã OTP Authenticator" else getString(R.string.use_recovery_code)
            views.button(actionRow, switchModeLabel, R.id.recovery_action) {
                isRecoveryMode = !isRecoveryMode
                verifyErrorMessage = null
                renderedState = null
                updateScreen()
            }.apply {
                textSize = 13f
                minimumHeight = views.dp(34)
                setPadding(views.dp(14), views.dp(4), views.dp(14), views.dp(4))
                (layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    topMargin = 0
                    marginStart = views.dp(12)
                }
            }
        }
        root.addView(actionRow)

        setContentView(root)
        val preserved = oldFocus?.let { findViewById<View>(it) }?.takeIf { it.isEnabled && it.isFocusable }
        views.focusLater(preserved ?: pinPad.firstButton)
    }

    /**
     * PARENT ACTIVITY CONFIGURATION:
     * Full parental control dashboard, only visible during active parent session (5-minute sliding).
     * Single screen, zero scrolling needed on 1080p TV.
     */
    private fun renderParentConfiguration(permissions: PermissionSnapshot, oldFocus: Int?) {
        activePinPad = null
        verifyStatusView = null

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(views.dp(36), views.dp(20), views.dp(36), views.dp(20))
        }

        // Top Header Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, views.dp(10))
        }

        val logoImg = ImageView(this).apply {
            setImageResource(R.drawable.ic_pctv)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        topBar.addView(logoImg, LinearLayout.LayoutParams(views.dp(40), views.dp(40)).apply {
            gravity = Gravity.CENTER_VERTICAL
            marginEnd = views.dp(14)
        })

        val titlesCol = views.column()
        titlesCol.addView(views.title(getString(R.string.parent_config_title)).apply {
            textSize = 20f
            setPadding(0, 0, 0, views.dp(2))
        })

        val remSec = sessionManager.remainingSessionSeconds()
        val min = remSec / 60
        val sec = remSec % 60
        val liveTimer = views.text(getString(R.string.parent_session_remaining, min, sec), 13f, R.color.accent_amber)
        liveTimerView = liveTimer
        titlesCol.addView(liveTimer)
        topBar.addView(titlesCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val lockExitBtn = views.button(topBar, getString(R.string.lock_and_exit), R.id.lock_action) {
            sessionManager.invalidateSession()
            ModeStateManager.instance.expireSession()
            broadcastSender.sendModeChanged(Mode.KID, 0, false)
            finishAffinity()
        }.apply {
            textSize = 14f
            minimumHeight = views.dp(38)
            setPadding(views.dp(14), views.dp(6), views.dp(14), views.dp(6))
            nextFocusDownId = R.id.weekday_4h
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(topBar)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        }
        root.addView(scroll)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(content)

        // 1. Status Card
        val statusCard = views.column().apply {
            background = getDrawable(R.drawable.status_card)
            setPadding(views.dp(16), views.dp(10), views.dp(16), views.dp(10))
        }
        val cal = Calendar.getInstance()
        val isWeekend = cal.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)
        val dayName = if (isWeekend) {
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) "Thứ Bảy" else "Chủ Nhật"
        } else {
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Thứ Hai"
                Calendar.TUESDAY -> "Thứ Ba"
                Calendar.WEDNESDAY -> "Thứ Tư"
                Calendar.THURSDAY -> "Thứ Năm"
                else -> "Thứ Sáu"
            }
        }
        val currentLimitH = if (isWeekend) screenTimeStore.getWeekendHours() else screenTimeStore.getWeekdayHours()
        statusCard.addView(views.text("Hôm nay ($dayName) • Giới hạn $currentLimitH Giờ", 14f, R.color.accent_cyan))

        val usedSec = screenTimeStore.getUsedSec()
        val usedMin = usedSec / 60
        val totalLimitSec = screenTimeStore.getTotalLimitSec(cal, screenTimePolicy)
        val remainingSec = (totalLimitSec - usedSec).coerceAtLeast(0)
        val remMin = remainingSec / 60
        val timerSummary = "Đã xem: $usedMin phút  |  Còn lại: $remMin phút  (Tự động đặt lại lúc 00:00)"
        statusCard.addView(views.text(timerSummary, 16f, R.color.success).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, views.dp(4), 0, 0)
        })
        content.addView(statusCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = views.dp(8) })

        // 2. Settings Card (Prerequisites Gate: enabled only when permissions.ready == true)
        val configCard = views.column().apply {
            background = getDrawable(R.drawable.status_card)
            setPadding(views.dp(16), views.dp(10), views.dp(16), views.dp(10))
        }
        configCard.addView(views.text("Cài đặt giới hạn thời gian xem TV", 16f, R.color.text_primary).apply {
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        if (!permissions.ready) {
            configCard.addView(views.text(
                getString(R.string.permissions_required_for_screentime),
                13f,
                R.color.warning
            ).apply { setPadding(0, views.dp(4), 0, views.dp(8)) })
        } else {
            configCard.addView(views.text(
                "Khi hết thời lượng, TV sẽ tự động hiển thị màn hình khóa và ngắt toàn bộ âm thanh.",
                13f,
                R.color.text_secondary
            ).apply { setPadding(0, views.dp(2), 0, views.dp(6)) })
        }

        val weekdayHours = screenTimeStore.getWeekdayHours()
        val weekendHours = screenTimeStore.getWeekendHours()

        configCard.addView(views.text("Ngày trong tuần (Thứ 2 – Thứ 6)", 13f, R.color.accent_cyan).apply {
            setPadding(0, views.dp(2), 0, views.dp(4))
        })

        val weekdayRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, views.dp(4), 0, views.dp(10))
        }
        val weekdayIds = listOf(R.id.weekday_1h, R.id.weekday_2h, R.id.weekday_3h, R.id.weekday_4h)
        val weekendIds = listOf(R.id.weekend_1h, R.id.weekend_2h, R.id.weekend_3h, R.id.weekend_4h)

        for (h in 1..4) {
            val isSelected = (h == weekdayHours)
            val label = if (isSelected) "✓ $h Giờ (Đang chọn)" else "$h Giờ"
            val btn = views.button(weekdayRow, label, weekdayIds[h - 1], enabled = permissions.ready) {
                screenTimeStore.setWeekdayHours(h)
                Toast.makeText(this, "Đã lưu Thứ 2 - Thứ 6: $h giờ/ngày", Toast.LENGTH_SHORT).show()
                renderedState = null
                updateScreen()
            }.apply {
                textSize = 14f
                minimumHeight = views.dp(40)
                setPadding(views.dp(8), views.dp(6), views.dp(8), views.dp(6))
                if (!permissions.ready) {
                    alpha = 0.4f
                }
            }
            btn.nextFocusUpId = R.id.lock_action
            btn.nextFocusDownId = weekendIds[h - 1]
            btn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 0
                if (h > 1) marginStart = views.dp(8)
            }
        }
        configCard.addView(weekdayRow)

        configCard.addView(views.text("Cuối tuần (Thứ 7 & Chủ Nhật)", 13f, R.color.accent_cyan).apply {
            setPadding(0, views.dp(2), 0, views.dp(4))
        })

        val weekendRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, views.dp(4), 0, views.dp(4))
        }
        for (h in 1..4) {
            val isSelected = (h == weekendHours)
            val label = if (isSelected) "✓ $h Giờ (Đang chọn)" else "$h Giờ"
            val btn = views.button(weekendRow, label, weekendIds[h - 1], enabled = permissions.ready) {
                screenTimeStore.setWeekendHours(h)
                Toast.makeText(this, "Đã lưu Cuối tuần: $h giờ/ngày", Toast.LENGTH_SHORT).show()
                renderedState = null
                updateScreen()
            }.apply {
                textSize = 14f
                minimumHeight = views.dp(40)
                setPadding(views.dp(8), views.dp(6), views.dp(8), views.dp(6))
                if (!permissions.ready) {
                    alpha = 0.4f
                }
            }
            btn.nextFocusUpId = weekdayIds[h - 1]
            btn.nextFocusDownId = if (!permissions.ready && h <= 2) R.id.permission_fix_action else R.id.change_pin_action
            btn.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 0
                if (h > 1) marginStart = views.dp(8)
            }
        }
        configCard.addView(weekendRow)

        content.addView(configCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = views.dp(8) })

        // 3. Compact Bottom System & Security Row
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Left Card: System Status
        val cardLeft = views.column().apply {
            background = getDrawable(R.drawable.status_card)
            setPadding(views.dp(14), views.dp(8), views.dp(14), views.dp(8))
        }
        cardLeft.addView(views.text(getString(R.string.system_status_title), 12f, R.color.text_secondary))
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = views.dp(34)
        }
        if (permissions.ready) {
            statusRow.addView(views.text(
                getString(R.string.system_status_ready),
                14f,
                R.color.success
            ), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        } else {
            statusRow.addView(views.text(
                getString(R.string.system_status_unready),
                14f,
                R.color.warning
            ), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            views.button(statusRow, getString(R.string.fix_permission_action), R.id.permission_fix_action) {
                if (permissions.accessibility != AccessibilityState.CONNECTED) {
                    openSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } else {
                    openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }.apply {
                textSize = 13f
                minimumHeight = views.dp(34)
                setPadding(views.dp(12), views.dp(4), views.dp(12), views.dp(4))
                nextFocusUpId = R.id.weekend_1h
                nextFocusRightId = R.id.change_pin_action
                (layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    topMargin = 0
                }
            }
        }
        cardLeft.addView(statusRow)
        bottomRow.addView(cardLeft, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            marginEnd = views.dp(8)
        })

        // Right Card: Security Settings (PIN or TOTP)
        val cardRight = views.column().apply {
            background = getDrawable(R.drawable.status_card)
            setPadding(views.dp(14), views.dp(8), views.dp(14), views.dp(8))
        }
        cardRight.addView(views.text(getString(R.string.security_card_title), 12f, R.color.text_secondary))
        val secRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = views.dp(34)
        }

        val authMode = securityStore.getAuthMode()
        if (authMode == AuthMode.PIN) {
            val pinStatusLabel = if (securityStore.isCustomPin()) "Mã PIN riêng" else "Mã PIN mặc định (000000)"
            secRow.addView(views.text(pinStatusLabel, 13f, R.color.success), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            views.button(secRow, getString(R.string.change_pin_action), R.id.change_pin_action) {
                isChangingPin = true
                renderedState = null
                updateScreen()
            }.apply {
                textSize = 13f
                minimumHeight = views.dp(34)
                setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4))
                nextFocusUpId = R.id.weekend_3h
                nextFocusRightId = R.id.enable_google_auth_action
                if (!permissions.ready) {
                    nextFocusLeftId = R.id.permission_fix_action
                }
                (layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    topMargin = 0
                    marginEnd = views.dp(6)
                }
            }

            views.button(secRow, getString(R.string.enable_google_auth_action), R.id.enable_google_auth_action) {
                isEnrollingTotp = true
                pendingSecret = TotpEngine.generateSecret()
                pendingRecoverySet = RecoveryCodeManager.generateSet()
                renderedState = null
                updateScreen()
            }.apply {
                textSize = 13f
                minimumHeight = views.dp(34)
                setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4))
                nextFocusUpId = R.id.weekend_4h
                nextFocusLeftId = R.id.change_pin_action
                (layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    topMargin = 0
                }
            }
        } else {
            secRow.addView(views.text(getString(R.string.security_totp_mode), 13f, R.color.success), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            views.button(secRow, getString(R.string.view_recovery_codes), R.id.recovery_action) {
                isViewingRecoveryInParentMode = true
                renderedState = null
                updateScreen()
            }.apply {
                textSize = 13f
                minimumHeight = views.dp(34)
                setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4))
                nextFocusUpId = R.id.weekend_3h
                nextFocusRightId = R.id.disable_google_auth_action
                if (!permissions.ready) {
                    nextFocusLeftId = R.id.permission_fix_action
                }
                (layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    topMargin = 0
                    marginEnd = views.dp(6)
                }
            }

            views.button(secRow, getString(R.string.disable_google_auth_action), R.id.disable_google_auth_action) {
                securityStore.disableTotp("000000")
                Toast.makeText(this, "Đã chuyển về mã PIN mặc định 000000", Toast.LENGTH_SHORT).show()
                renderedState = null
                updateScreen()
            }.apply {
                textSize = 13f
                minimumHeight = views.dp(34)
                setPadding(views.dp(10), views.dp(4), views.dp(10), views.dp(4))
                nextFocusUpId = R.id.weekend_4h
                nextFocusLeftId = R.id.recovery_action
                (layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    topMargin = 0
                }
            }
        }

        cardRight.addView(secRow)
        bottomRow.addView(cardRight, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
            marginStart = views.dp(8)
        })

        content.addView(bottomRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        setContentView(root)
        val selectedWeekdayId = listOf(R.id.weekday_1h, R.id.weekday_2h, R.id.weekday_3h, R.id.weekday_4h)[screenTimeStore.getWeekdayHours().coerceIn(1, 4) - 1]
        val preserved = oldFocus?.let { findViewById<View>(it) }?.takeIf { it.isEnabled && it.isFocusable }
        views.focusLater(preserved ?: (if (permissions.ready) findViewById(selectedWeekdayId) else findViewById(R.id.permission_fix_action)) ?: lockExitBtn)
    }

    /**
     * SCREEN: Change PIN
     */
    private fun renderChangePinScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(views.dp(48), views.dp(24), views.dp(48), views.dp(24))
        }

        root.addView(views.title(getString(R.string.enter_new_pin_title)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        })
        views.body(root, getString(R.string.enter_new_pin_body))

        val pinPad = DpadPinPadView(this) { enteredPin ->
            if (enteredPin.length == 6) {
                securityStore.saveCustomPin(enteredPin)
                Toast.makeText(this, getString(R.string.pin_changed_success), Toast.LENGTH_LONG).show()
                isChangingPin = false
                renderedState = null
                updateScreen()
            }
        }
        activePinPad = pinPad
        root.addView(pinPad)

        val backBtn = views.button(root, getString(R.string.back_action), R.id.back_action) {
            isChangingPin = false
            renderedState = null
            updateScreen()
        }
        (backBtn.layoutParams as? LinearLayout.LayoutParams)?.apply {
            topMargin = views.dp(16)
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER_HORIZONTAL
        }

        setContentView(root)
        views.focusLater(pinPad.firstButton)
    }

    /**
     * SCREEN: Google Authenticator Enrollment (Optional Upgrade)
     */
    private fun renderTotpEnrollmentScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(views.dp(36), views.dp(20), views.dp(36), views.dp(20))
        }

        root.addView(views.title("Nâng cấp: Bật Google Authenticator").apply {
            textSize = 22f
            setPadding(0, 0, 0, views.dp(8))
        })
        root.addView(views.text(
            "Quét mã QR bằng ứng dụng Google Authenticator trên điện thoại, sau đó nhập mã OTP 6 số để kích hoạt:",
            14f,
            R.color.text_secondary
        ).apply { setPadding(0, 0, 0, views.dp(12)) })

        val twoCol = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val secret = pendingSecret ?: TotpEngine.generateSecret().also { pendingSecret = it }
        val recoverySet = pendingRecoverySet ?: RecoveryCodeManager.generateSet().also { pendingRecoverySet = it }

        // Left Col: QR Code
        val leftCol = views.column().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(views.dp(16), views.dp(12), views.dp(16), views.dp(12))
            background = getDrawable(R.drawable.status_card)
        }
        val uri = TotpEngine.generateOtpAuthUri(secret, "TCL TV", "FamilyTime")
        val qrBitmap = QrCodeRenderer.renderQrBitmap(uri, views.dp(180))
        val qrView = ImageView(this).apply {
            setImageBitmap(qrBitmap)
            background = Color.WHITE.let { android.graphics.drawable.ColorDrawable(it) }
            setPadding(views.dp(8), views.dp(8), views.dp(8), views.dp(8))
        }
        leftCol.addView(qrView, LinearLayout.LayoutParams(views.dp(180), views.dp(180)))
        leftCol.addView(views.text("Mã khóa: $secret", 12f, R.color.accent_cyan).apply {
            setPadding(0, views.dp(8), 0, 0)
        })
        twoCol.addView(leftCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = views.dp(16)
        })

        // Right Col: PIN Pad to verify & confirm
        val rightCol = views.column().apply {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val statusTv = views.text("", 14f, R.color.warning).apply {
            visibility = View.GONE
            setPadding(0, 0, 0, views.dp(6))
        }
        verifyStatusView = statusTv
        rightCol.addView(statusTv)

        val pinPad = DpadPinPadView(this) { enteredOtp ->
            val epochSeconds = System.currentTimeMillis() / 1000L
            val valid = TotpEngine.verifyCode(secret, enteredOtp, epochSeconds)
            if (valid) {
                securityStore.enableTotp(secret, recoverySet.saltBase64, recoverySet.records)
                Toast.makeText(this, "Đã bật Google Authenticator thành công!", Toast.LENGTH_LONG).show()
                isEnrollingTotp = false
                renderedState = null
                updateScreen()
            } else {
                statusTv.text = "Mã OTP không chính xác. Vui lòng thử lại."
                statusTv.visibility = View.VISIBLE
                activePinPad?.clear()
            }
        }
        activePinPad = pinPad
        rightCol.addView(pinPad)
        twoCol.addView(rightCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f))

        root.addView(twoCol)

        val backBtn = views.button(root, getString(R.string.back_action), R.id.back_action) {
            isEnrollingTotp = false
            renderedState = null
            updateScreen()
        }
        (backBtn.layoutParams as? LinearLayout.LayoutParams)?.apply {
            topMargin = views.dp(14)
            width = LinearLayout.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER_HORIZONTAL
        }

        setContentView(root)
        views.focusLater(pinPad.firstButton)
    }

    /**
     * SCREEN: Recovery Codes View (Only for Google Authenticator mode)
     */
    private fun renderRecoveryCodesScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#090D16"))
            setPadding(views.dp(36), views.dp(24), views.dp(36), views.dp(24))
        }

        root.addView(views.title(getString(R.string.recovery_codes_title)))
        views.body(root, "Danh sách 6 mã phục hồi khẩn cấp cho Google Authenticator của bạn:")

        val recoveryView = RecoveryCodesView(this)
        val codes = securityStore.getRecoveryRecords().map { it.hash.take(6) }
        recoveryView.setCodes(codes)
        root.addView(recoveryView)

        val closeBtn = views.button(root, "Đóng mã phục hồi", R.id.primary_action) {
            isViewingRecoveryInParentMode = false
            renderedState = null
            updateScreen()
        }
        (closeBtn.layoutParams as? LinearLayout.LayoutParams)?.apply {
            topMargin = views.dp(20)
            width = LinearLayout.LayoutParams.WRAP_CONTENT
        }

        setContentView(root)
        views.focusLater(closeBtn)
    }

    private fun onAuthSuccess(nowMs: Long, message: String) {
        rateLimiter.recordSuccess()
        sessionManager.startSession()
        ModeStateManager.instance.onParentAuthenticated(nowMs)
        broadcastSender.sendModeChanged(
            Mode.PARENT,
            ModeStateManager.instance.getSessionRemainingSec(nowMs),
            ModeStateManager.instance.isQuizActive()
        )
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        renderedState = null
        updateScreen()
    }

    private fun handlePinSubmitted(pin: String) {
        if (rateLimiter.isLocked()) {
            activePinPad?.clear()
            return
        }

        val authMode = securityStore.getAuthMode()
        val nowMs = System.currentTimeMillis()

        if (authMode == AuthMode.PIN) {
            val valid = securityStore.verifyPin(pin)
            if (valid) {
                onAuthSuccess(nowMs, "Xác thực phụ huynh thành công!")
            } else {
                handleAuthFailure()
            }
        } else {
            if (isRecoveryMode) {
                val salt = securityStore.getRecoverySalt() ?: ""
                val records = securityStore.getRecoveryRecords()
                val (success, updatedRecords) = RecoveryCodeManager.verifyAndConsume(pin, salt, records, nowMs)
                if (success) {
                    securityStore.saveRecoverySet(salt, updatedRecords)
                    isRecoveryMode = false
                    onAuthSuccess(nowMs, "Mở khóa bằng mã phục hồi thành công!")
                } else {
                    handleAuthFailure()
                }
            } else {
                val secret = securityStore.getTotpSecret() ?: ""
                val epochSeconds = nowMs / 1000L
                val valid = TotpEngine.verifyCode(secret, pin, epochSeconds)

                if (valid) {
                    onAuthSuccess(nowMs, "Xác thực phụ huynh thành công!")
                } else {
                    handleAuthFailure()
                }
            }
        }
    }

    private fun handleAuthFailure() {
        val lockedUntil = rateLimiter.recordFailure()
        activePinPad?.clear()
        if (lockedUntil > 0) {
            activePinPad?.setKeypadEnabled(false)
            verifyStatusView?.text = getString(R.string.parent_verify_locked, rateLimiter.remainingLockSeconds())
            verifyStatusView?.visibility = View.VISIBLE
        } else {
            verifyErrorMessage = getString(R.string.parent_verify_error, getString(R.string.parent_verify_attempts_left, rateLimiter.remainingAttemptsBeforeLock()))
            verifyStatusView?.text = verifyErrorMessage
            verifyStatusView?.visibility = View.VISIBLE
        }
    }

    private fun openSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }
}
