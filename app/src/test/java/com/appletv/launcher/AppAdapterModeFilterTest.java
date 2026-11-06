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
    public void parentModeAllowsAllNonHiddenApps() {
        boolean kidMode = false;
        assertTrue(AppAdapter.shouldIncludePackage("org.smarttube.stable", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.netflix.ninja", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.android.settings", ownPkg, hidden, kidMode, null));
        assertFalse(AppAdapter.shouldIncludePackage("com.hidden.app", ownPkg, hidden, kidMode, null));
        assertFalse(AppAdapter.shouldIncludePackage(ownPkg, ownPkg, hidden, kidMode, null));
    }

    @Test
    public void kidModeOnlyAllowsWhitelistedApps() {
        boolean kidMode = true;
        // Allowed
        assertTrue(AppAdapter.shouldIncludePackage("org.smarttube.stable", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("com.google.android.youtube.tvkids", ownPkg, hidden, kidMode, null));
        assertTrue(AppAdapter.shouldIncludePackage("org.khanacademy.android", ownPkg, hidden, kidMode, null));

        // Blocked
        assertFalse(AppAdapter.shouldIncludePackage("com.netflix.ninja", ownPkg, hidden, kidMode, null));
        assertFalse(AppAdapter.shouldIncludePackage("com.android.settings", ownPkg, hidden, kidMode, null));
        assertFalse(AppAdapter.shouldIncludePackage("com.android.chrome", ownPkg, hidden, kidMode, null));
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
