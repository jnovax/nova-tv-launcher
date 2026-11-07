package vn.familytv.parentalcontrol.onboarding

enum class AccessibilityState { DISABLED, ENABLED_DISCONNECTED, CONNECTED }

data class PermissionSnapshot(
    val accessibilityEnabled: Boolean,
    val serviceConnected: Boolean,
    val usageAccess: Boolean,
) {
    val accessibility: AccessibilityState
        get() = when {
            !accessibilityEnabled -> AccessibilityState.DISABLED
            serviceConnected -> AccessibilityState.CONNECTED
            else -> AccessibilityState.ENABLED_DISCONNECTED
        }

    val ready: Boolean
        get() = accessibility == AccessibilityState.CONNECTED && usageAccess
}

enum class InitialSetupStep {
    PERMISSIONS,
    PARENT_OTP,
    RECOVERY_CODES;

    companion object {
        fun fromStored(value: String?): InitialSetupStep = entries.firstOrNull { it.name == value } ?: PERMISSIONS
    }
}

object OnboardingFlow {
    fun restore(
        saved: InitialSetupStep,
        permissions: PermissionSnapshot,
        isEnrolled: Boolean
    ): InitialSetupStep? {
        if (isEnrolled) return null
        if (!permissions.ready) return InitialSetupStep.PERMISSIONS
        return saved
    }

    fun next(
        current: InitialSetupStep,
        permissions: PermissionSnapshot,
        isOtpVerified: Boolean = false
    ): InitialSetupStep? {
        if (!permissions.ready) return InitialSetupStep.PERMISSIONS
        return when (current) {
            InitialSetupStep.PERMISSIONS -> if (permissions.ready) InitialSetupStep.PARENT_OTP else current
            InitialSetupStep.PARENT_OTP -> if (isOtpVerified) InitialSetupStep.RECOVERY_CODES else current
            InitialSetupStep.RECOVERY_CODES -> null
        }
    }
}
