package com.appletv.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class AppAdapterModeFilterTest {

    private final String ownPkg = "com.appletv.launcher";
    private final Set<String> hidden = Collections.singleton("com.hidden.app");

    @Test
    public void parentModeAllowsAllNonHiddenAppsIncludingFamilyTime() {
        boolean kidMode = false;
        assertTrue(AppAdapter.shouldIncludePackage("vn.familytv.parentalcontrol", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("org.smarttube.stable", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.spotify.tv.android", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.netflix.ninja", ownPkg, hidden, kidMode, null));
        assertFalse(AppAdapter.shouldIncludePackage("com.hidden.app", ownPkg, hidden, kidMode, null));
        assertFalse(AppAdapter.shouldIncludePackage(ownPkg, ownPkg, hidden, kidMode, null));
    }

    @Test
    public void kidModeHidesFamilyTimeAndShowsOtherApps() {
        boolean kidMode = true;
        // Family Time must be hidden in Kid Mode
        assertFalse(AppAdapter.shouldIncludePackage("vn.familytv.parentalcontrol", ownPkg, hidden, kidMode, null));

        // Other apps show up
        assertTrue(AppAdapter.shouldIncludePackage("org.smarttube.stable", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.spotify.tv.android", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("vn.mytvnet.b2cott", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("org.courville.nova", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.vocatv.app", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.netflix.ninja", ownPkg, hidden, kidMode, null));

        // Hidden apps & own package still hidden
        assertFalse(AppAdapter.shouldIncludePackage("com.hidden.app", ownPkg, hidden, kidMode, null));
        assertFalse(AppAdapter.shouldIncludePackage(ownPkg, ownPkg, hidden, kidMode, null));
    }

    @Test
    public void kidModeRespectsCustomWhitelist() {
        boolean kidMode = true;
        Set<String> custom = new HashSet<>();
        custom.add("com.special.kids.app");

        assertTrue(AppAdapter.shouldIncludePackage("com.special.kids.app", ownPkg, hidden, kidMode, custom));
        assertFalse(AppAdapter.shouldIncludePackage("org.smarttube.stable", ownPkg, hidden, kidMode, custom));
    }
}
