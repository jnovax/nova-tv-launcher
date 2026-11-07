package vn.familytv.parentalcontrol.timelock

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import vn.familytv.parentalcontrol.R
import vn.familytv.parentalcontrol.security.AuthMode
import vn.familytv.parentalcontrol.security.ParentSessionManager
import vn.familytv.parentalcontrol.security.RateLimiter
import vn.familytv.parentalcontrol.security.RecoveryCodeManager
import vn.familytv.parentalcontrol.security.SecurityStore
import vn.familytv.parentalcontrol.security.TotpEngine
import vn.familytv.parentalcontrol.ui.TvViews
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView

class TimeLockOverlayLayout(
    context: Context,
    private val securityStore: SecurityStore,
    private val onExtended: (Int) -> Unit,
    private val onOpenParentConfig: () -> Unit,
    private val onGoHome: () -> Unit
) : FrameLayout(context) {

    private val views = TvViews(context)
    private val rateLimiter = RateLimiter()
    private val sessionManager = ParentSessionManager()
    private var todayLimitHours: Int = 2

    private val container: LinearLayout
    private var activePinPad: DpadPinPadView? = null

    init {
        setBackgroundColor(Color.parseColor("#F2090D16")) // Deep dark frosted overlay
        isFocusable = true
        isFocusableInTouchMode = true

        container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(views.dp(48), views.dp(36), views.dp(48), views.dp(36))
        }

        addView(container, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
            gravity = Gravity.CENTER
        })

        showNoticeView()
    }

    fun setTodayLimitHours(hours: Int) {
        todayLimitHours = hours
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
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
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                // Back key on lock screen returns home
                onGoHome()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    fun showNoticeView() {
        activePinPad = null
        container.removeAllViews()

        // Clock Icon & Headline
        val iconTv = TextView(context).apply {
            text = "⏰ 🔇"
            textSize = 44f
            gravity = Gravity.CENTER
        }
        container.addView(iconTv)

        val titleTv = views.title(context.getString(R.string.timelock_title)).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, views.dp(10), 0, views.dp(8))
        }
        container.addView(titleTv)

        val bodyTv = views.text(
            context.getString(R.string.timelock_body, todayLimitHours),
            18f,
            R.color.text_secondary
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, views.dp(28))
        }
        container.addView(bodyTv)

        // Action Buttons Row
        val actionsRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val unlockBtn = views.button(actionsRow, context.getString(R.string.timelock_parent_unlock), R.id.primary_action) {
            showParentAuthView()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 0
            }
            nextFocusRightId = R.id.continue_action
        }

        val homeBtn = views.button(actionsRow, context.getString(R.string.timelock_go_home), R.id.continue_action) {
            onGoHome()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 0
                marginStart = views.dp(16)
            }
            nextFocusLeftId = R.id.primary_action
        }

        container.addView(actionsRow)
        views.focusLater(unlockBtn)
    }

    private fun showParentAuthView() {
        container.removeAllViews()

        val titleTv = views.title(context.getString(R.string.kid_gate_title)).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        container.addView(titleTv)

        val authMode = securityStore.getAuthMode()
        val isCustomPin = securityStore.isCustomPin()

        val bodyText = if (authMode == AuthMode.TOTP) {
            "Nhập 6 chữ số mã OTP Google Authenticator (hoặc mã phục hồi) để mở khóa:"
        } else {
            "Nhập mã PIN 6 số của phụ huynh để mở khóa gia hạn thời gian:"
        }
        val bodyTv = views.text(
            bodyText,
            16f,
            R.color.text_secondary
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, views.dp(4), 0, views.dp(12))
        }
        container.addView(bodyTv)

        val statusTv = views.text("", 16f, R.color.warning).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 0, 0, views.dp(8))
            visibility = View.GONE
        }
        container.addView(statusTv)

        val pinPad = DpadPinPadView(context) { enteredPin ->
            handlePinSubmitted(enteredPin, statusTv)
        }
        activePinPad = pinPad
        container.addView(pinPad)

        if (authMode == AuthMode.PIN && !isCustomPin) {
            val hintTv = views.text(context.getString(R.string.default_pin_hint), 15f, R.color.accent_cyan).apply {
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, views.dp(10), 0, 0)
            }
            container.addView(hintTv)
        }

        val backBtn = views.button(container, context.getString(R.string.back_action), R.id.back_action) {
            showNoticeView()
        }
        (backBtn.layoutParams as? LinearLayout.LayoutParams)?.topMargin = views.dp(16)

        views.focusLater(pinPad.firstButton)
    }

    private fun handlePinSubmitted(pin: String, statusTv: TextView) {
        if (rateLimiter.isLocked()) {
            activePinPad?.clear()
            statusTv.text = context.getString(R.string.parent_verify_locked, rateLimiter.remainingLockSeconds())
            statusTv.visibility = View.VISIBLE
            return
        }

        val authMode = securityStore.getAuthMode()
        val nowMs = System.currentTimeMillis()

        val valid: Boolean
        if (authMode == AuthMode.PIN) {
            valid = securityStore.verifyPin(pin)
        } else {
            val epochSeconds = nowMs / 1000L
            val secret = securityStore.getTotpSecret() ?: ""
            var isValid = TotpEngine.verifyCode(secret, pin, epochSeconds)
            if (!isValid) {
                val salt = securityStore.getRecoverySalt() ?: ""
                val records = securityStore.getRecoveryRecords()
                val (success, updatedRecords) = RecoveryCodeManager.verifyAndConsume(pin, salt, records, nowMs)
                if (success) {
                    securityStore.saveRecoverySet(salt, updatedRecords)
                    isValid = true
                }
            }
            valid = isValid
        }

        if (valid) {
            rateLimiter.recordSuccess()
            showExtensionSelectorView()
        } else {
            val lockedUntil = rateLimiter.recordFailure()
            activePinPad?.clear()
            statusTv.visibility = View.VISIBLE
            if (lockedUntil > 0) {
                statusTv.text = context.getString(R.string.parent_verify_locked, rateLimiter.remainingLockSeconds())
                activePinPad?.setKeypadEnabled(false)
            } else {
                statusTv.text = context.getString(R.string.parent_verify_error,
                    context.getString(R.string.parent_verify_attempts_left, rateLimiter.remainingAttemptsBeforeLock()))
            }
        }
    }

    fun showExtensionSelectorView() {
        activePinPad = null
        container.removeAllViews()

        val titleTv = views.title(context.getString(R.string.extension_title)).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        container.addView(titleTv)

        val bodyTv = views.text(
            context.getString(R.string.extension_body),
            16f,
            R.color.text_secondary
        ).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, views.dp(4), 0, views.dp(24))
        }
        container.addView(bodyTv)

        // 4 Options Grid (2x2) inside a centered container with balanced width
        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(views.dp(640), LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val row1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, views.dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val btn15m = views.button(row1, "+ 15 Phút", R.id.primary_action) {
            onExtended(900)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 0
            }
            nextFocusRightId = R.id.continue_action
            nextFocusDownId = R.id.unlock_action
        }
        val btn30m = views.button(row1, "+ 30 Phút", R.id.continue_action) {
            onExtended(1800)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 0
                marginStart = views.dp(16)
            }
            nextFocusLeftId = R.id.primary_action
            nextFocusDownId = R.id.lock_action
        }
        grid.addView(row1)

        val row2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, views.dp(20))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val btn1h = views.button(row2, "+ 1 Giờ", R.id.unlock_action) {
            onExtended(3600)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 0
            }
            nextFocusUpId = R.id.primary_action
            nextFocusRightId = R.id.lock_action
            nextFocusDownId = R.id.recovery_action
        }
        val btnAllDay = views.button(row2, "Xem hết hôm nay", R.id.lock_action) {
            onExtended(86400)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                topMargin = 0
                marginStart = views.dp(16)
            }
            nextFocusUpId = R.id.continue_action
            nextFocusLeftId = R.id.unlock_action
            nextFocusDownId = R.id.recovery_action
        }
        grid.addView(row2)

        container.addView(grid)

        // Advanced Config button
        val configBtn = views.button(container, "Vào trang Cấu hình đầy đủ", R.id.recovery_action) {
            onOpenParentConfig()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(views.dp(360), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = views.dp(12)
            }
            nextFocusUpId = R.id.unlock_action
        }

        views.focusLater(btn15m)
    }
}
