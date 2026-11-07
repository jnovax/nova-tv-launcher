package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppAdapterYouTubeOverrideTest {

    @Test
    public void tizentubeAndSmarttubeAreIdentifiedAsYouTubePackages() {
        assertTrue(AppAdapter.isYouTubePackage("io.gh.reisxd.tizentube.cobalt"));
        assertTrue(AppAdapter.isYouTubePackage("org.smarttube.stable"));
        assertFalse(AppAdapter.isYouTubePackage("com.spotify.tv.android"));
        assertFalse(AppAdapter.isYouTubePackage("vn.mytvnet.b2cott"));
    }

    @Test
    public void tizentubeLabelIsOverriddenToYouTube() {
        assertEquals("YouTube", AppAdapter.resolveAppLabel("io.gh.reisxd.tizentube.cobalt", "TizenTube"));
        assertEquals("YouTube", AppAdapter.resolveAppLabel("io.gh.reisxd.tizentube.cobalt", "Cobalt"));
        assertEquals("YouTube", AppAdapter.resolveAppLabel("org.smarttube.stable", "SmartTube"));
        assertEquals("Spotify", AppAdapter.resolveAppLabel("com.spotify.tv.android", "Spotify"));
    }

    @Test
    public void tizentubeUsesCustomYouTubeBanner() {
        assertTrue(AppAdapter.shouldUseCustomYouTubeBanner("io.gh.reisxd.tizentube.cobalt"));
        assertFalse(AppAdapter.shouldUseCustomYouTubeBanner("com.spotify.tv.android"));
    }
}
