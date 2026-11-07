package vn.familytv.parentalcontrol.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {
    private var simulatedTime = 1000000L
    private val limiter = RateLimiter(timeProvider = { simulatedTime })

    @Test
    fun testLockoutAfter5Failures() {
        assertEquals(5, limiter.remainingAttemptsBeforeLock())
        assertFalse(limiter.isLocked())

        repeat(4) {
            limiter.recordFailure()
            assertFalse(limiter.isLocked())
        }
        assertEquals(1, limiter.remainingAttemptsBeforeLock())

        // 5th failure triggers 60s lock
        limiter.recordFailure()
        assertTrue(limiter.isLocked())
        assertEquals(60L, limiter.remainingLockSeconds())

        // Advance 30s
        simulatedTime += 30000L
        assertTrue(limiter.isLocked())
        assertEquals(30L, limiter.remainingLockSeconds())

        // Advance 31s -> lock expires
        simulatedTime += 31000L
        assertFalse(limiter.isLocked())

        // Failure immediately after lockout triggers 300s lock
        limiter.recordFailure()
        assertTrue(limiter.isLocked())
        assertEquals(300L, limiter.remainingLockSeconds())

        // Success resets everything
        limiter.recordSuccess()
        assertFalse(limiter.isLocked())
        assertEquals(5, limiter.remainingAttemptsBeforeLock())
    }
}
