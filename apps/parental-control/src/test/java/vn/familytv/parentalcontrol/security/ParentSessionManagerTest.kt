package vn.familytv.parentalcontrol.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentSessionManagerTest {
    private var simulatedTime = 1000000L
    private val sessionManager = ParentSessionManager(timeProvider = { simulatedTime })

    @Test
    fun testSessionLifecycleAndSlidingTimeout() {
        assertFalse(sessionManager.isParentModeActive())

        sessionManager.startSession()
        assertTrue(sessionManager.isParentModeActive())
        assertEquals(300L, sessionManager.remainingSessionSeconds())

        // Advance 200 seconds
        simulatedTime += 200000L
        assertTrue(sessionManager.isParentModeActive())
        assertEquals(100L, sessionManager.remainingSessionSeconds())

        // User interacts (D-pad touch) -> extends session back to 300s
        sessionManager.touchSession()
        assertEquals(300L, sessionManager.remainingSessionSeconds())

        // Advance 301 seconds -> expires
        simulatedTime += 301000L
        assertFalse(sessionManager.isParentModeActive())
        assertEquals(0L, sessionManager.remainingSessionSeconds())

        // Invalidate drops immediately
        sessionManager.startSession()
        assertTrue(sessionManager.isParentModeActive())
        sessionManager.invalidateSession()
        assertFalse(sessionManager.isParentModeActive())

        // Start session with custom remaining duration (e.g. 120s synced from ModeStateManager)
        sessionManager.startSession(120_000L)
        assertTrue(sessionManager.isParentModeActive())
        assertEquals(120L, sessionManager.remainingSessionSeconds())
    }
}
