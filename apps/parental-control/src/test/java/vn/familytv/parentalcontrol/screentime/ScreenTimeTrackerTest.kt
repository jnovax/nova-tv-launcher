package vn.familytv.parentalcontrol.screentime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.familytv.parentalcontrol.ipc.Mode

class ScreenTimeTrackerTest {
    private var limitReached = false
    private val policy = ScreenTimePolicy(defaultLimitSec = 10)
    private val tracker = ScreenTimeTracker(policy = policy, onLimitReached = { limitReached = true })

    @Test
    fun testDoesNotTickWhenScreenIsOff() {
        tracker.setScreenOn(false)
        tracker.setMode(Mode.KID)
        tracker.setForegroundPackage("com.google.android.youtube.tv")

        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())
        assertFalse(limitReached)
    }

    @Test
    fun testDoesNotTickWhenDreamingOrScreenSaverIsActive() {
        tracker.setScreenOn(true)
        tracker.setDreaming(true)
        tracker.setMode(Mode.KID)
        tracker.setForegroundPackage("com.google.android.youtube.tv")

        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())
        assertFalse(limitReached)

        // Exit screensaver
        tracker.setDreaming(false)
        tracker.tickSecond()
        assertEquals(9, tracker.getRemainingSec())
    }

    @Test
    fun testDoesNotTickWhenInParentMode() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.PARENT)
        tracker.setForegroundPackage("com.google.android.youtube.tv")

        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())
        assertFalse(limitReached)
    }

    @Test
    fun testDoesNotTickOnLauncherOrFamilyTime() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)

        tracker.setForegroundPackage("com.appletv.launcher")
        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())

        tracker.setForegroundPackage("vn.familytv.parentalcontrol")
        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())
        assertFalse(limitReached)
    }

    @Test
    fun testDoesNotTickOnScreensaverOrAmbientPackages() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)

        tracker.setForegroundPackage("com.google.android.backdrop")
        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())

        tracker.setForegroundPackage("com.tcl.ambient")
        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())

        tracker.setForegroundPackage("com.android.systemui")
        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())
        assertFalse(limitReached)
    }

    @Test
    fun testDoesNotTickOnUntrackedApps() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)

        tracker.setForegroundPackage("com.netflix.ninja")
        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())

        tracker.setForegroundPackage("com.vocatv.app")
        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())
        assertFalse(limitReached)
    }

    @Test
    fun testTicksOnDesignatedEntertainmentApps() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)

        // 1. YouTube / SmartTube
        tracker.setForegroundPackage("io.gh.reisxd.tizentube.cobalt")
        tracker.tickSecond()
        assertEquals(9, tracker.getRemainingSec())

        // 2. MyTV
        tracker.setForegroundPackage("vn.mytvnet.b2cott")
        tracker.tickSecond()
        assertEquals(8, tracker.getRemainingSec())

        // 3. Nova Video Player
        tracker.setForegroundPackage("org.courville.nova")
        tracker.tickSecond()
        assertEquals(7, tracker.getRemainingSec())

        // 4. Spotify
        tracker.setForegroundPackage("com.spotify.tv.android")
        tracker.tickSecond()
        assertEquals(6, tracker.getRemainingSec())
    }

    @Test
    fun testTicksOnThirdPartyAppAndReachesLimit() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)
        tracker.setForegroundPackage("io.gh.reisxd.tizentube.cobalt")

        for (i in 1..9) {
            tracker.tickSecond()
            assertEquals(10 - i, tracker.getRemainingSec())
            assertFalse(limitReached)
        }

        tracker.tickSecond()
        assertEquals(0, tracker.getRemainingSec())
        assertTrue(limitReached)
    }

    @Test
    fun testResetRestoresLimit() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)
        tracker.setForegroundPackage("vn.mytvnet.b2cott")

        tracker.tickSecond()
        tracker.tickSecond()
        assertEquals(8, tracker.getRemainingSec())

        tracker.reset()
        assertEquals(10, tracker.getRemainingSec())
    }

    @Test
    fun testRestoreState() {
        tracker.restoreState(5)
        assertEquals(5, tracker.getRemainingSec())
    }

    @Test
    fun testDoesNotTickWhenForegroundPackageIsNull() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)
        tracker.setForegroundPackage(null)

        tracker.tickSecond()
        assertEquals(10, tracker.getRemainingSec())
        assertFalse(limitReached)
    }

    @Test
    fun testDoesNotTickBelowZero() {
        tracker.setScreenOn(true)
        tracker.setMode(Mode.KID)
        tracker.setForegroundPackage("vn.mytvnet.b2cott")
        tracker.restoreState(1)

        tracker.tickSecond()
        assertEquals(0, tracker.getRemainingSec())
        assertTrue(limitReached)

        limitReached = false
        tracker.tickSecond()
        assertEquals(0, tracker.getRemainingSec())
        assertFalse(limitReached)
    }
}
