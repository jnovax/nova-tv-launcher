package vn.familytv.parentalcontrol.security

class ParentSessionManager(
    private val sessionDurationMs: Long = 300_000L,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var sessionExpiresEpochMs: Long = 0L

    fun startSession(durationMs: Long = sessionDurationMs) {
        sessionExpiresEpochMs = timeProvider() + durationMs
    }

    fun touchSession() {
        if (isParentModeActive()) {
            sessionExpiresEpochMs = timeProvider() + sessionDurationMs
        }
    }

    fun invalidateSession() {
        sessionExpiresEpochMs = 0L
    }

    fun isParentModeActive(): Boolean {
        return timeProvider() < sessionExpiresEpochMs
    }

    fun remainingSessionSeconds(): Long {
        val diff = sessionExpiresEpochMs - timeProvider()
        return if (diff > 0) (diff + 999) / 1000 else 0L
    }
}
