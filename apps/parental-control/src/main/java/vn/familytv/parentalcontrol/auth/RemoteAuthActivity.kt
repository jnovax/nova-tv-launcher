package vn.familytv.parentalcontrol.auth

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.Window
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import vn.familytv.parentalcontrol.R
import vn.familytv.parentalcontrol.ipc.Mode
import vn.familytv.parentalcontrol.ipc.ModeBroadcastSender
import vn.familytv.parentalcontrol.ipc.ModeStateManager
import vn.familytv.parentalcontrol.security.AuthMode
import vn.familytv.parentalcontrol.security.RateLimiter
import vn.familytv.parentalcontrol.security.RecoveryCodeManager
import vn.familytv.parentalcontrol.security.SecurityStore
import vn.familytv.parentalcontrol.security.TotpEngine
import vn.familytv.parentalcontrol.ui.TvViews
import vn.familytv.parentalcontrol.ui.auth.DpadPinPadView

class RemoteAuthActivity : Activity() {
    companion object {
        const val ACTION_AUTHENTICATE = "vn.familytv.parentalcontrol.ACTION_AUTHENTICATE"
        val rateLimiter = RateLimiter()
    }

    private val securityStore by lazy { SecurityStore.fromContext(this) }
    private val broadcastSender by lazy { ModeBroadcastSender(this) }
    private val views by lazy { TvViews(this) }
    private lateinit var pinPad: DpadPinPadView
    private var isRecoveryMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ModeStateManager.instance.isAuthInProgress = true
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(Color.argb(220, 10, 14, 24)))

        renderAuthUi()
    }

    private fun renderAuthUi() {
        val authMode = securityStore.getAuthMode()
        val isCustomPin = securityStore.isCustomPin()
        val secret = securityStore.getTotpSecret()

        if (authMode == AuthMode.TOTP && secret.isNullOrEmpty()) {
            Toast.makeText(this, "Chưa thiết lập bảo vệ phụ huynh trên FamilyTime", Toast.LENGTH_LONG).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(views.dp(36), views.dp(16), views.dp(36), views.dp(16))
        }

        val logoImg = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_pctv)
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        root.addView(logoImg, LinearLayout.LayoutParams(views.dp(44), views.dp(44)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = views.dp(6)
        })

        val title = TextView(this).apply {
            text = getString(R.string.kid_gate_title)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, views.dp(4))
        }
        root.addView(title)

        val sub = TextView(this).apply {
            if (authMode == AuthMode.PIN && !isCustomPin) {
                text = getString(R.string.default_pin_hint)
                textSize = 15f
                setTextColor(Color.parseColor("#F59E0B"))
                typeface = Typeface.DEFAULT_BOLD
            } else if (authMode == AuthMode.PIN) {
                text = "Nhập mã PIN 6 số của phụ huynh để mở khóa:"
                textSize = 14f
                setTextColor(Color.LTGRAY)
            } else if (isRecoveryMode) {
                text = getString(R.string.enter_recovery_body)
                textSize = 14f
                setTextColor(Color.parseColor("#F59E0B"))
            } else {
                text = "Nhập mã 6 chữ số từ ứng dụng Google Authenticator:"
                textSize = 14f
                setTextColor(Color.LTGRAY)
            }
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, views.dp(8))
        }
        root.addView(sub)

        pinPad = DpadPinPadView(this) { pin ->
            val nowSec = System.currentTimeMillis() / 1000L
            val isLocked = rateLimiter.isLocked()
            if (isLocked) {
                val waitSec = rateLimiter.remainingLockSeconds()
                Toast.makeText(this@RemoteAuthActivity, "Thử sai nhiều lần! Vui lòng chờ $waitSec giây", Toast.LENGTH_SHORT).show()
                pinPad.clear()
                return@DpadPinPadView
            }

            val valid = when (authMode) {
                AuthMode.PIN -> securityStore.verifyPin(pin)
                AuthMode.TOTP -> {
                    if (isRecoveryMode) {
                        val salt = securityStore.getRecoverySalt() ?: ""
                        val records = securityStore.getRecoveryRecords()
                        val (success, updated) = RecoveryCodeManager.verifyAndConsume(pin, salt, records, System.currentTimeMillis())
                        if (success) {
                            securityStore.saveRecoverySet(salt, updated)
                        }
                        success
                    } else {
                        secret?.let { TotpEngine.verifyCode(it, pin, nowSec) } ?: false
                    }
                }
            }

            if (valid) {
                rateLimiter.recordSuccess()
                val manager = ModeStateManager.instance
                val nowMs = System.currentTimeMillis()
                manager.onParentAuthenticated(nowMs)
                broadcastSender.sendModeChanged(
                    Mode.PARENT,
                    manager.getSessionRemainingSec(nowMs),
                    manager.isQuizActive()
                )
                Toast.makeText(this@RemoteAuthActivity, "Đã mở khóa Chế độ Phụ huynh!", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            } else {
                rateLimiter.recordFailure()
                pinPad.clear()
                val msg = if (authMode == AuthMode.PIN) "Mã PIN không chính xác" else "Mã xác thực không chính xác"
                Toast.makeText(this@RemoteAuthActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(pinPad)

        // Action row (Cancel & Recovery toggle)
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, views.dp(8), 0, 0)
        }

        views.button(actionRow, getString(R.string.back_action), R.id.primary_action) {
            setResult(RESULT_CANCELED)
            finish()
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
            val recoveryBtnText = if (isRecoveryMode) "Dùng mã TOTP" else getString(R.string.use_recovery_code)
            views.button(actionRow, recoveryBtnText, R.id.recovery_action) {
                isRecoveryMode = !isRecoveryMode
                renderAuthUi()
            }.apply {
                textSize = 13f
                minimumHeight = views.dp(34)
                setPadding(views.dp(14), views.dp(4), views.dp(14), views.dp(4))
                (layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = LinearLayout.LayoutParams.WRAP_CONTENT
                    marginStart = views.dp(12)
                    topMargin = 0
                }
            }
        }

        root.addView(actionRow)
        setContentView(root)
        pinPad.requestFocus()
        pinPad.firstButton.requestFocus()
    }

    override fun onDestroy() {
        ModeStateManager.instance.isAuthInProgress = false
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (::pinPad.isInitialized) {
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
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            setResult(RESULT_CANCELED)
            finish()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        setResult(RESULT_CANCELED)
        finish()
    }
}
