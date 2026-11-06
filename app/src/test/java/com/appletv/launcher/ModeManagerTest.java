package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ModeManagerTest {

    @Test
    public void defaultModeIsAlwaysKidMode() {
        assertEquals(ModeManager.MODE_KID, ModeManager.normalizeMode(null));
        assertEquals(ModeManager.MODE_KID, ModeManager.normalizeMode(""));
        assertEquals(ModeManager.MODE_KID, ModeManager.normalizeMode("UNKNOWN"));
        assertEquals(ModeManager.MODE_PARENT, ModeManager.normalizeMode("PARENT"));
    }

    @Test
    public void kidModeHidesFamilyTimeByDefault() {
        assertFalse(ModeManager.isAppAllowedInKidMode("vn.familytv.parentalcontrol", null));
        assertFalse(ModeManager.isAppAllowedInKidMode("com.familytv.another", null));
    }

    @Test
    public void kidModeAllowsOtherAppsByDefault() {
        assertTrue(ModeManager.isAppAllowedInKidMode("com.google.android.youtube.tvkids", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("org.smarttube.stable", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("com.spotify.tv.android", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("vn.mytvnet.b2cott", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("org.courville.nova", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("com.vocatv.app", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("com.netflix.ninja", null));
    }

    @Test
    public void customWhitelistOverridesDefaultsButStillHidesFamilyTime() {
        Set<String> custom = new HashSet<>();
        custom.add("com.custom.learning.game");
        custom.add("vn.familytv.parentalcontrol"); // even if mistakenly added

        assertTrue(ModeManager.isAppAllowedInKidMode("com.custom.learning.game", custom));
        assertFalse(ModeManager.isAppAllowedInKidMode("org.smarttube.stable", custom));
        // Family Time must remain hidden in Kid Mode even if in custom list
        assertFalse(ModeManager.isAppAllowedInKidMode("vn.familytv.parentalcontrol", custom));
    }

    @Test
    public void parentalControlDetectionIdentifiesTargetOrHandler() {
        assertFalse(ModeManager.isParentalControlInstalled(null, false));
        assertFalse(ModeManager.isParentalControlInstalled(Collections.emptySet(), false));
        assertFalse(ModeManager.isParentalControlInstalled(Collections.singleton("com.other.app"), false));

        // Detected by package name
        assertTrue(ModeManager.isParentalControlInstalled(Collections.singleton(ModeManager.TARGET_PACKAGE), false));

        // Detected by intent handler
        assertTrue(ModeManager.isParentalControlInstalled(Collections.emptySet(), true));
    }
}
