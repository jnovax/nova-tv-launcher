package vn.familytv.parentalcontrol.screentime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimePolicyTest {

    private val policy = ScreenTimePolicy()

    @Test
    fun testYouTubePackagesAreTracked() {
        assertTrue(policy.isTrackedPackage("io.gh.reisxd.tizentube.cobalt"))
        assertTrue(policy.isTrackedPackage("org.smarttube.stable"))
        assertTrue(policy.isTrackedPackage("org.smarttube.beta"))
        assertTrue(policy.isTrackedPackage("com.teamsmart.videomanager.tv"))
        assertTrue(policy.isTrackedPackage("com.liskovsoft.smarttubetv.beta"))
        assertTrue(policy.isTrackedPackage("com.liskovsoft.videomanager"))
        assertTrue(policy.isTrackedPackage("com.google.android.youtube.tv"))
        assertTrue(policy.isTrackedPackage("com.google.android.youtube.tvkids"))
        assertTrue(policy.isTrackedPackage("com.google.android.youtube"))
        assertTrue(policy.isTrackedPackage("org.schabi.newpipe"))
    }

    @Test
    fun testMyTVPackagesAreTracked() {
        assertTrue(policy.isTrackedPackage("vn.mytvnet.b2cott"))
        assertTrue(policy.isTrackedPackage("vn.mytvnet.b2c"))
        assertTrue(policy.isTrackedPackage("vn.vnpt.media.mytv"))
    }

    @Test
    fun testNovaVideoPlayerIsTracked() {
        assertTrue(policy.isTrackedPackage("org.courville.nova"))
    }

    @Test
    fun testSpotifyPackagesAreTracked() {
        assertTrue(policy.isTrackedPackage("com.spotify.tv.android"))
        assertTrue(policy.isTrackedPackage("com.spotify.music"))
    }

    @Test
    fun testScreenSaverAndAmbientPackagesAreNotTracked() {
        assertFalse(policy.isTrackedPackage("com.google.android.backdrop"))
        assertFalse(policy.isTrackedPackage("com.google.android.apps.tv.dreamx"))
        assertFalse(policy.isTrackedPackage("com.tcl.ambient"))
        assertFalse(policy.isTrackedPackage("com.android.systemui"))
    }

    @Test
    fun testLauncherAndSettingsAndOtherAppsAreNotTracked() {
        assertFalse(policy.isTrackedPackage("com.appletv.launcher"))
        assertFalse(policy.isTrackedPackage("vn.familytv.parentalcontrol"))
        assertFalse(policy.isTrackedPackage("com.android.tv.settings"))
        assertFalse(policy.isTrackedPackage("com.netflix.ninja"))
        assertFalse(policy.isTrackedPackage("com.vocatv.app"))
        assertFalse(policy.isTrackedPackage(null))
        assertFalse(policy.isTrackedPackage(""))
        assertFalse(policy.isTrackedPackage("   "))
    }
}
