package vn.familytv.parentalcontrol.ipc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeStateManagerTest {
    @Test
    fun testDefaultModeIsKid() {
        val manager = ModeStateManager(sessionDurationSec = 300)
        assertEquals(Mode.KID, manager.getMode())
        assertEquals(0, manager.getSessionRemainingSec(nowMs = 1000L))
        assertFalse(manager.isQuizActive())
    }

    @Test
    fun testParentAuthenticationStarts5MinSession() {
        val manager = ModeStateManager(sessionDurationSec = 300)
        val now = 100_000L
        manager.onParentAuthenticated(nowMs = now)

        assertEquals(Mode.PARENT, manager.getMode())
        assertEquals(300, manager.getSessionRemainingSec(nowMs = now))

        // Check sliding extension
        manager.extendSession(nowMs = now + 60_000L)
        assertEquals(300, manager.getSessionRemainingSec(nowMs = now + 60_000L))

        // Fast forward 300s -> expires to KID
        manager.tick(nowMs = now + 361_000L)
        assertEquals(Mode.KID, manager.getMode())
        assertEquals(0, manager.getSessionRemainingSec(nowMs = now + 361_000L))
    }

    @Test
    fun testScreenOffImmediatelyLocksToKidMode() {
        val manager = ModeStateManager(sessionDurationSec = 300)
        manager.onParentAuthenticated(nowMs = 100_000L)
        assertEquals(Mode.PARENT, manager.getMode())

        manager.onScreenOff()
        assertEquals(Mode.KID, manager.getMode())
    }

    @Test
    fun testQuizActiveFlag() {
        val manager = ModeStateManager(sessionDurationSec = 300)
        manager.setQuizActive(true)
        assertTrue(manager.isQuizActive())
        manager.setQuizActive(false)
        assertFalse(manager.isQuizActive())
    }

    @Test
    fun testScreenTimeRemaining() {
        val manager = ModeStateManager(sessionDurationSec = 300)
        assertEquals(900, manager.getScreenTimeRemainingSec())
        manager.setScreenTimeRemainingSec(450)
        assertEquals(450, manager.getScreenTimeRemainingSec())
    }

    @Test
    fun testModeChangeListener() {
        val manager = ModeStateManager(sessionDurationSec = 300)
        val transitions = mutableListOf<Mode>()
        val listener: (Mode) -> Unit = { transitions.add(it) }
        manager.addListener(listener)

        manager.onParentAuthenticated(nowMs = 100_000L)
        manager.expireSession()

        assertEquals(listOf(Mode.PARENT, Mode.KID), transitions)

        // Test removeListener
        manager.removeListener(listener)
        manager.onParentAuthenticated(nowMs = 200_000L)
        assertEquals(2, transitions.size)
    }

    @Test
    fun testAuthInProgressFlag() {
        val manager = ModeStateManager(sessionDurationSec = 300)
        assertFalse(manager.isAuthInProgress)
        manager.isAuthInProgress = true
        assertTrue(manager.isAuthInProgress)
        manager.isAuthInProgress = false
        assertFalse(manager.isAuthInProgress)
    }

    @Test
    fun testResetForTesting() {
        val manager = ModeStateManager.instance
        manager.onParentAuthenticated(nowMs = 100_000L)
        manager.setQuizActive(true)
        manager.isAuthInProgress = true
        manager.setScreenTimeRemainingSec(100)

        manager.resetForTesting()
        assertEquals(Mode.KID, manager.getMode())
        assertFalse(manager.isQuizActive())
        assertFalse(manager.isAuthInProgress)
        assertEquals(900, manager.getScreenTimeRemainingSec())
        assertEquals(0, manager.getSessionRemainingSec(nowMs = 100_000L))
    }
}
