package com.appletv.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;

import com.appletv.contract.StatusProviderContract;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages Kid Mode vs Parent Mode state, whitelisted apps, and IPC integration
 * with tv-parental-control (vn.familytv.parentalcontrol).
 *
 * Fail-safe principle: Default mode is always KID unless explicitly validated as PARENT.
 */
public final class ModeManager {

    public static final String MODE_KID = "KID";
    public static final String MODE_PARENT = "PARENT";

    public static final String TARGET_PACKAGE = "vn.familytv.parentalcontrol";
    public static final String ACTION_MODE_CHANGED = "vn.familytv.parentalcontrol.ACTION_MODE_CHANGED";
    public static final String ACTION_AUTHENTICATE = "vn.familytv.parentalcontrol.ACTION_AUTHENTICATE";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_ALLOWED_PACKAGES = "extra_allowed_packages";

    private static final String PREFS_NAME = "mode_preferences";
    private static final String KEY_CURRENT_MODE = "current_mode";
    private static final String KEY_WHITELIST = "whitelist_packages";

    private static final Set<String> DEFAULT_KID_WHITELIST;

    static {
        Set<String> set = new HashSet<>();
        set.add("com.google.android.youtube.tvkids");
        set.add("org.smarttube.stable");
        set.add("org.schabi.newpipe");
        set.add("org.videolan.vlc");
        set.add("org.khanacademy.android");
        set.add("com.duolingo");
        set.add("vn.familytv.parentalcontrol");
        DEFAULT_KID_WHITELIST = Collections.unmodifiableSet(set);
    }

    private ModeManager() {}

    /** Normalizes raw mode string with fail-safe default to KID mode. */
    public static String normalizeMode(String raw) {
        if (MODE_PARENT.equalsIgnoreCase(raw)) {
            return MODE_PARENT;
        }
        return MODE_KID;
    }

    /** Gets the current mode for the app. Defaults to MODE_KID. */
    public static String getMode(Context context) {
        if (context == null) return MODE_KID;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return normalizeMode(prefs.getString(KEY_CURRENT_MODE, MODE_KID));
    }

    /** Sets the current mode. Normalizes input so invalid modes always become KID. */
    public static void setMode(Context context, String mode) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_MODE, normalizeMode(mode)).apply();
    }

    /** Pure helper to test parental control detection logic. */
    public static boolean isParentalControlInstalled(Set<String> installedPackages, boolean hasAuthHandler) {
        if (installedPackages != null && installedPackages.contains(TARGET_PACKAGE)) {
            return true;
        }
        return hasAuthHandler;
    }

    /**
     * Checks if a compatible parental control service is installed on the TV.
     * Looks for vn.familytv.parentalcontrol or any activity responding to ACTION_AUTHENTICATE.
     */
    public static boolean isParentalControlAvailable(Context context) {
        if (context == null) return false;
        android.content.pm.PackageManager pm = context.getPackageManager();
        if (pm == null) return false;

        try {
            pm.getPackageInfo(TARGET_PACKAGE, 0);
            return true;
        } catch (Exception ignored) {}

        try {
            android.content.Intent intent = new android.content.Intent(ACTION_AUTHENTICATE);
            java.util.List<android.content.pm.ResolveInfo> list = pm.queryIntentActivities(intent, 0);
            return list != null && !list.isEmpty();
        } catch (Exception ignored) {}

        return false;
    }

    /**
     * Returns true if current mode is KID.
     * If no compatible parental control service is installed on the TV, Kid Mode is NOT engaged,
     * so the launcher behaves normally and shows all apps without restriction.
     */
    public static boolean isKidMode(Context context) {
        if (context == null) return false;
        if (!isParentalControlAvailable(context)) {
            return false;
        }
        return MODE_KID.equals(getMode(context));
    }

    /**
     * Checks if a package belongs to Family Time / Parental Control which must remain hidden from kids.
     */
    public static boolean isFamilyTimePackage(String packageName) {
        if (packageName == null) return false;
        return TARGET_PACKAGE.equalsIgnoreCase(packageName)
                || packageName.toLowerCase().contains("familytv")
                || packageName.toLowerCase().contains("parentalcontrol");
    }

    /** Returns the unmodifiable default whitelist of kid-safe apps. */
    public static Set<String> getDefaultKidWhitelist() {
        return DEFAULT_KID_WHITELIST;
    }

    /** Returns true if the package is permitted to run under Kid Mode. */
    public static boolean isAppAllowedInKidMode(String packageName, Set<String> customWhitelist) {
        if (packageName == null || packageName.isEmpty()) return false;

        // Family Time is strictly hidden in Kid Mode
        if (isFamilyTimePackage(packageName)) {
            return false;
        }

        // Custom whitelist provided by tv-parental-control or user preferences if explicitly specified
        if (customWhitelist != null && !customWhitelist.isEmpty()) {
            return customWhitelist.contains(packageName);
        }

        // All other apps are allowed in Kid Mode by default
        return true;
    }

    /** Updates the custom whitelist in SharedPreferences. */
    public static void setCustomWhitelist(Context context, Set<String> packages) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (packages == null || packages.isEmpty()) {
            prefs.edit().remove(KEY_WHITELIST).apply();
        } else {
            prefs.edit().putStringSet(KEY_WHITELIST, new HashSet<>(packages)).apply();
        }
    }

    /** Gets active custom whitelist if stored, or null if using defaults. */
    public static Set<String> getCustomWhitelist(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_WHITELIST, null);
    }

    /**
     * Determines the pill text color based on traffic light remaining time thresholds:
     * - > 5 minutes (> 300s): White (#FFFFFFFF)
     * - 2 to 5 minutes (121s to 300s): Warning Gold (#FFFFD60A)
     * - <= 2 minutes (<= 120s): Alert Red (#FFFF453A)
     */
    public static int getTimePillColor(long remainingSeconds) {
        if (remainingSeconds > 300) {
            return 0xFFFFFFFF; // White
        } else if (remainingSeconds > 120) {
            return 0xFFFFD60A; // Warning Gold
        } else {
            return 0xFFFF453A; // Alert Red
        }
    }

    /**
     * Creates a ColorStateList that renders black when focused (for the white tvOS pill highlight)
     * and the specified unfocused color otherwise.
     */
    public static ColorStateList getPillColorStateList(int unfocusedColor) {
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_focused },
            new int[] {}
        };
        int[] colors = new int[] {
            0xFF000000,
            unfocusedColor
        };
        return new ColorStateList(states, colors);
    }

    /**
     * Queries remaining screen time seconds from StatusProvider (tv-parental-control).
     * Returns -1 if provider is unavailable or query fails.
     */
    public static long getRemainingSecondsFromProvider(Context context) {
        if (context == null) return -1;
        try (Cursor cursor = context.getContentResolver().query(
                Uri.parse(StatusProviderContract.CONTENT_URI_STRING),
                new String[]{StatusProviderContract.COLUMN_REMAINING_SECONDS},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(StatusProviderContract.COLUMN_REMAINING_SECONDS);
                if (idx >= 0) return cursor.getLong(idx);
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
