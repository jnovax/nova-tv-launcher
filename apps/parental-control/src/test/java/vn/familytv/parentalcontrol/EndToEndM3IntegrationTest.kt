package vn.familytv.parentalcontrol

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.familytv.parentalcontrol.ipc.Mode
import vn.familytv.parentalcontrol.ipc.ModeStateManager
import vn.familytv.parentalcontrol.screentime.ScreenTimePolicy
import vn.familytv.parentalcontrol.screentime.ScreenTimeTracker

class EndToEndM3IntegrationTest {

    @Test
    fun testFullDailyScreenTimeLimitAndExtensionFlow() {
        var audioMuted = false
        var overlayShown = false

        val modeManager = ModeStateManager(sessionDurationSec = 300)
        val policy = ScreenTimePolicy(defaultLimitSec = 3)
        val tracker = ScreenTimeTracker(policy = policy) {
            audioMuted = true
            overlayShown = true
            modeManager.setQuizActive(true)
        }

        // 1. Kid watches TV on 3rd-party app
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)
        tracker.setForegroundPackage("com.google.android.youtube.tv")

        tracker.tickSecond()
        tracker.tickSecond()
        assertFalse(audioMuted)
        assertFalse(overlayShown)

        // 2. 3rd second hits daily limit
        tracker.tickSecond()
        assertTrue(audioMuted)
        assertTrue(overlayShown)
        assertTrue(modeManager.isQuizActive())

        // 3. Parent unlocks and grants 30-minute extension (1800 seconds)
        val extensionSec = 1800
        val currentRem = tracker.getRemainingSec().coerceAtLeast(0)
        val newRemaining = currentRem + extensionSec
        tracker.restoreState(newRemaining)

        audioMuted = false
        overlayShown = false
        modeManager.setQuizActive(false)

        assertEquals(1800, tracker.getRemainingSec())
        assertFalse(audioMuted)
        assertFalse(overlayShown)
        assertFalse(modeManager.isQuizActive())

        // Kid continues watching: tick 10 seconds
        repeat(10) { tracker.tickSecond() }
        assertEquals(1790, tracker.getRemainingSec())
    }

    @Test
    fun testWeekdayAndWeekendPolicyCalculations() {
        val policy = ScreenTimePolicy(defaultWeekdayHours = 2, defaultWeekendHours = 3)

        // Monday (Weekday)
        val monday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2) // Monday
        }
        assertFalse(policy.isWeekend(monday))
        assertEquals(7200, policy.getLimitSecForDay(policy.isWeekend(monday), 2, 3))

        // Saturday (Weekend)
        val saturday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 7) // Saturday
        }
        assertTrue(policy.isWeekend(saturday))
        assertEquals(10800, policy.getLimitSecForDay(policy.isWeekend(saturday), 2, 3))

        // Boundary checks (1 to 4 hours)
        assertEquals(3600, policy.getLimitSecForDay(false, weekdayHours = 0)) // coerced to 1h
        assertEquals(14400, policy.getLimitSecForDay(false, weekdayHours = 5)) // coerced to 4h
    }

    @Test
    fun testParentModeSuspendsScreenTimeDeduction() {
        val modeManager = ModeStateManager(sessionDurationSec = 300)
        val policy = ScreenTimePolicy(defaultLimitSec = 10)
        var limitHit = false
        val tracker = ScreenTimeTracker(policy = policy) {
            limitHit = true
        }

        tracker.setScreenOn(true)
        tracker.setForegroundPackage("com.google.android.youtube.tv")

        // In kid mode, 5 seconds elapsed
        tracker.setMode(modeManager.getMode())
        repeat(5) { tracker.tickSecond() }
        assertEquals(5, tracker.getRemainingSec())

        // Parent authenticates
        modeManager.onParentAuthenticated(1000L)
        tracker.setMode(modeManager.getMode())
        assertEquals(Mode.PARENT, modeManager.getMode())

        // In parent mode, ticking doesn't reduce kid screen time
        repeat(5) { tracker.tickSecond() }
        assertEquals(5, tracker.getRemainingSec())
        assertFalse(limitHit)

        // Parent session expires after 300s
        modeManager.tick(1000L + 300_000L)
        assertEquals(Mode.KID, modeManager.getMode())
        tracker.setMode(modeManager.getMode())

        // Now ticking resumes in Kid mode
        repeat(5) { tracker.tickSecond() }
        assertEquals(0, tracker.getRemainingSec())
        assertTrue(limitHit)
    }
}
