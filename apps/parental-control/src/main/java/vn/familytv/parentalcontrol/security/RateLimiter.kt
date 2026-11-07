package vn.familytv.parentalcontrol.security

class RateLimiter(
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    var consecutiveFailures: Int = 0
        private set
    var lockedUntilEpochMs: Long = 0L
        private set

    fun isLocked(): Boolean {
        return timeProvider() < lockedUntilEpochMs
    }

    fun remainingLockSeconds(): Long {
        val diff = lockedUntilEpochMs - timeProvider()
        return if (diff > 0) (diff + 999) / 1000 else 0L
    }

    fun remainingAttemptsBeforeLock(): Int {
        if (isLocked()) return 0
        return (5 - (consecutiveFailures % 5)).coerceIn(1, 5)
    }

    fun recordFailure(): Long {
        consecutiveFailures++
        val now = timeProvider()
        if (consecutiveFailures >= 5) {
            val lockoutDurationMs = if (consecutiveFailures == 5) 60_000L else 300_000L
            lockedUntilEpochMs = now + lockoutDurationMs
            return lockedUntilEpochMs
        }
        return 0L
    }

    fun recordSuccess() {
        consecutiveFailures = 0
        lockedUntilEpochMs = 0L
    }

    fun restore(failures: Int, lockedUntil: Long) {
        consecutiveFailures = failures
        lockedUntilEpochMs = lockedUntil
    }
}
