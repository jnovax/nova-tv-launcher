package vn.familytv.parentalcontrol.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStateTest {
    private val disconnected = PermissionSnapshot(true, false, true)
    private val noUsage = PermissionSnapshot(true, true, false)
    private val ready = PermissionSnapshot(true, true, true)

    @Test
    fun permissionsNotReadyKeepsPermissionsStep() {
        assertEquals(AccessibilityState.ENABLED_DISCONNECTED, disconnected.accessibility)
        assertFalse(disconnected.ready)
        assertEquals(InitialSetupStep.PERMISSIONS, OnboardingFlow.next(InitialSetupStep.PERMISSIONS, disconnected))

        assertEquals(AccessibilityState.CONNECTED, noUsage.accessibility)
        assertFalse(noUsage.ready)
        assertEquals(InitialSetupStep.PERMISSIONS, OnboardingFlow.next(InitialSetupStep.PERMISSIONS, noUsage))
    }

    @Test
    fun readyPermissionsAdvancesToParentOtp() {
        assertTrue(ready.ready)
        assertEquals(InitialSetupStep.PARENT_OTP, OnboardingFlow.next(InitialSetupStep.PERMISSIONS, ready))
    }

    @Test
    fun otpProgressionOnlyWhenVerified() {
        // Unverified stays on PARENT_OTP
        assertEquals(
            InitialSetupStep.PARENT_OTP,
            OnboardingFlow.next(InitialSetupStep.PARENT_OTP, ready, isOtpVerified = false)
        )
        // Verified advances to RECOVERY_CODES
        assertEquals(
            InitialSetupStep.RECOVERY_CODES,
            OnboardingFlow.next(InitialSetupStep.PARENT_OTP, ready, isOtpVerified = true)
        )
    }

    @Test
    fun completingRecoveryCodesReturnsNull() {
        assertNull(OnboardingFlow.next(InitialSetupStep.RECOVERY_CODES, ready))
    }

    @Test
    fun alreadyEnrolledRestoresToNull() {
        // Enrolled user must NEVER enter onboarding
        assertNull(OnboardingFlow.restore(InitialSetupStep.PERMISSIONS, ready, isEnrolled = true))
        assertNull(OnboardingFlow.restore(InitialSetupStep.PARENT_OTP, ready, isEnrolled = true))
        assertNull(OnboardingFlow.restore(InitialSetupStep.RECOVERY_CODES, ready, isEnrolled = true))
    }

    @Test
    fun revokedPermissionsDuringOnboardingRegressesToPermissions() {
        assertEquals(
            InitialSetupStep.PERMISSIONS,
            OnboardingFlow.restore(InitialSetupStep.PARENT_OTP, disconnected, isEnrolled = false)
        )
        assertEquals(
            InitialSetupStep.PERMISSIONS,
            OnboardingFlow.restore(InitialSetupStep.RECOVERY_CODES, noUsage, isEnrolled = false)
        )
    }

    @Test
    fun reopeningKeepsCurrentStepWhenPermissionsReady() {
        assertEquals(
            InitialSetupStep.PARENT_OTP,
            OnboardingFlow.restore(InitialSetupStep.PARENT_OTP, ready, isEnrolled = false)
        )
        assertEquals(
            InitialSetupStep.RECOVERY_CODES,
            OnboardingFlow.restore(InitialSetupStep.RECOVERY_CODES, ready, isEnrolled = false)
        )
    }

    @Test
    fun invalidOrMissingSavedProgressDefaultsToPermissions() {
        assertEquals(InitialSetupStep.PERMISSIONS, InitialSetupStep.fromStored(null))
        assertEquals(InitialSetupStep.PERMISSIONS, InitialSetupStep.fromStored("unknown"))
        assertEquals(InitialSetupStep.PARENT_OTP, InitialSetupStep.fromStored("PARENT_OTP"))
        assertEquals(InitialSetupStep.RECOVERY_CODES, InitialSetupStep.fromStored("RECOVERY_CODES"))
    }
}
