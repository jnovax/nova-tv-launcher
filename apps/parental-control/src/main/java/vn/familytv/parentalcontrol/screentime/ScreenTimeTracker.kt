package vn.familytv.parentalcontrol.screentime

import vn.familytv.parentalcontrol.ipc.Mode

class ScreenTimeTracker(
    val policy: ScreenTimePolicy = ScreenTimePolicy(),
    private val onLimitReached: () -> Unit
) {
    private var isScreenOn: Boolean = true
    private var isDreaming: Boolean = false
    private var currentMode: Mode = Mode.KID
    private var foregroundPackage: String? = null
    private var usedSec: Int = 0
    private var totalLimitSec: Int = policy.defaultLimitSec

    @Synchronized
    fun setScreenOn(isOn: Boolean) {
        isScreenOn = isOn
    }

    @Synchronized
    fun isScreenOn(): Boolean = isScreenOn

    @Synchronized
    fun setDreaming(dreaming: Boolean) {
        isDreaming = dreaming
    }

    @Synchronized
    fun isDreaming(): Boolean = isDreaming

    @Synchronized
    fun setMode(mode: Mode) {
        currentMode = mode
    }

    @Synchronized
    fun getMode(): Mode = currentMode

    @Synchronized
    fun setForegroundPackage(pkg: String?) {
        foregroundPackage = pkg
    }

    @Synchronized
    fun getForegroundPackage(): String? = foregroundPackage

    @Synchronized
    fun setTotalLimitSec(limit: Int) {
        totalLimitSec = limit
    }

    @Synchronized
    fun getTotalLimitSec(): Int = totalLimitSec

    @Synchronized
    fun getUsedSec(): Int = usedSec

    @Synchronized
    fun setUsedSec(used: Int) {
        usedSec = used.coerceAtLeast(0)
    }

    @Synchronized
    fun getRemainingSec(): Int = (totalLimitSec - usedSec).coerceAtLeast(0)

    @Synchronized
    fun tickSecond() {
        if (!isScreenOn || isDreaming || currentMode != Mode.KID) return
        val pkg = foregroundPackage ?: return
        if (!policy.isTrackedPackage(pkg)) return

        if (getRemainingSec() > 0) {
            usedSec++
            if (getRemainingSec() == 0) {
                onLimitReached()
            }
        }
    }

    @Synchronized
    fun reset() {
        usedSec = 0
    }

    @Synchronized
    fun restoreState(remainingSec: Int) {
        if (remainingSec > totalLimitSec - usedSec) {
            totalLimitSec = usedSec + remainingSec
        } else {
            usedSec = (totalLimitSec - remainingSec).coerceAtLeast(0)
        }
    }

    @Synchronized
    fun restoreUsedAndLimit(used: Int, limit: Int) {
        totalLimitSec = limit
        usedSec = used.coerceAtLeast(0)
    }
}
