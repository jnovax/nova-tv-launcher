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
    public void defaultWhitelistAllowsKidApps() {
        assertTrue(ModeManager.isAppAllowedInKidMode("com.google.android.youtube.tvkids", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("org.smarttube.stable", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("org.schabi.newpipe", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("org.videolan.vlc", null));
        assertTrue(ModeManager.isAppAllowedInKidMode("org.khanacademy.android", null));
    }

    @Test
    public void defaultWhitelistBlocksAdultAndSystemApps() {
        assertFalse(ModeManager.isAppAllowedInKidMode("com.android.settings", null));
        assertFalse(ModeManager.isAppAllowedInKidMode("com.google.android.tv.settings", null));
        assertFalse(ModeManager.isAppAllowedInKidMode("com.android.chrome", null));
        assertFalse(ModeManager.isAppAllowedInKidMode("com.google.android.youtube.tv", null)); // stock uncurated youtube
        assertFalse(ModeManager.isAppAllowedInKidMode("com.netflix.ninja", null));
        assertFalse(ModeManager.isAppAllowedInKidMode("com.android.vending", null)); // Play Store
    }

    @Test
    public void customWhitelistOverridesDefaults() {
        Set<String> custom = new HashSet<>();
        custom.add("com.custom.learning.game");

        assertTrue(ModeManager.isAppAllowedInKidMode("com.custom.learning.game", custom));
        assertFalse(ModeManager.isAppAllowedInKidMode("org.smarttube.stable", custom));
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
