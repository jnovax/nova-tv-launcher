package vn.familytv.parentalcontrol.ipc

import java.util.concurrent.CopyOnWriteArrayList

class ModeStateManager(private val sessionDurationSec: Int = 300) {
    private var currentMode: Mode = Mode.KID
    private var expiryTimeMs: Long = 0L
    private var isQuizActive: Boolean = false
    private var screenTimeRemainingSec: Int = 900
    private val listeners = CopyOnWriteArrayList<(Mode) -> Unit>()

    @Volatile
    var isAuthInProgress: Boolean = false

    @Synchronized
    fun getMode(): Mode = currentMode

    @Synchronized
    fun onParentAuthenticated(nowMs: Long) {
        currentMode = Mode.PARENT
        expiryTimeMs = nowMs + (sessionDurationSec * 1000L)
        isQuizActive = false
        notifyListeners(Mode.PARENT)
    }

    @Synchronized
    fun extendSession(nowMs: Long) {
        if (currentMode == Mode.PARENT) {
            expiryTimeMs = nowMs + (sessionDurationSec * 1000L)
        }
    }

    @Synchronized
    fun tick(nowMs: Long) {
        if (currentMode == Mode.PARENT && nowMs >= expiryTimeMs) {
            expireSession()
        }
    }

    @Synchronized
    fun expireSession() {
        if (currentMode == Mode.PARENT) {
            currentMode = Mode.KID
            expiryTimeMs = 0L
            notifyListeners(Mode.KID)
        }
    }

    @Synchronized
    fun onScreenOff() {
        expireSession()
    }

    @Synchronized
    fun setQuizActive(active: Boolean) {
        isQuizActive = active
    }

    @Synchronized
    fun isQuizActive(): Boolean = isQuizActive

    @Synchronized
    fun getSessionRemainingSec(nowMs: Long): Int {
        if (currentMode != Mode.PARENT) return 0
        val diff = expiryTimeMs - nowMs
        return if (diff > 0) ((diff + 999) / 1000).toInt() else 0
    }

    @Synchronized
    fun setScreenTimeRemainingSec(sec: Int) {
        screenTimeRemainingSec = sec
    }

    @Synchronized
    fun getScreenTimeRemainingSec(): Int = screenTimeRemainingSec

    fun addListener(listener: (Mode) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Mode) -> Unit) {
        listeners.remove(listener)
    }

    @Synchronized
    fun resetForTesting() {
        currentMode = Mode.KID
        expiryTimeMs = 0L
        isQuizActive = false
        isAuthInProgress = false
        screenTimeRemainingSec = 900
        listeners.clear()
    }

    private fun notifyListeners(mode: Mode) {
        listeners.forEach { it(mode) }
    }

    companion object {
        val instance = ModeStateManager()
    }
}
