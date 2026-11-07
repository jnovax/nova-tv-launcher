package com.appletv.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Handles remote shortcut keys:
 * - Direct app remaps for streaming buttons (YouTube: 190, VTVgo: 4168, Netflix: 4062).
 * - Settings button (QuickSettings or System Settings).
 * - Recent Apps button (BUTTON_2 or scancode 150).
 * - Home button on remotes emitting KEYCODE_ALL_APPS.
 *
 * All navigation and volume keys strictly pass through untouched to preserve native
 * repeat acceleration.
 */
public class HomeKeyService extends AccessibilityService {

    private int[] settingsScancodes = new int[0];
    private int[] settingsKeycodes = new int[0];
    private boolean settingsPanel;
    private Config config;
    private long lastConfigLoadTime = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        getConfig();
    }

    private synchronized Config getConfig() {
        long now = System.currentTimeMillis();
        if (config == null || (now - lastConfigLoadTime > 4000)) {
            config = Config.load(this);
            lastConfigLoadTime = now;
            settingsKeycodes = config.settingsKeycodes;
            settingsPanel = config.settingsKeyPanel;
        }
        return config;
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS || event.getKeyCode() == 83) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        for (int code : settingsKeycodes) if (event.getKeyCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        android.util.Log.i("AppleTv.HomeKey", "onKeyEvent: keyCode=" + event.getKeyCode() + " scanCode=" + event.getScanCode() + " action=" + event.getAction());
        android.util.Log.e("HomeKeyTest", "key: " + event.getKeyCode() + " action: " + event.getAction());

        // 1. Direct App Shortcuts & Actions (YouTube, Netflix, VTVgo, Inputs, Wallpaper, etc.)
        Config cfg = getConfig();
        String target = cfg.remoteKeys.get(event.getKeyCode());
        if (target == null && event.getScanCode() > 0) {
            target = cfg.remoteKeys.get(event.getScanCode());
        }
        if (target != null) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                launchTarget(target);
            }
            return true;
        }

        // 2. Settings button
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = new Intent(this, QuickSettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                try {
                    startActivity(open);
                } catch (Exception e) {
                    android.util.Log.e("HomeKeyService", "Failed to start settings", e);
                }
            }
            return true;
        }

        // 3. Recent Apps switcher
        if (event.getKeyCode() == KeyEvent.KEYCODE_BUTTON_2 || event.getScanCode() == 150) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = new Intent(this, RecentAppsActivity.class);
                startActivity(open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            return true;
        }

        // 4. Xiaomi Home / KEYCODE_ALL_APPS
        if (event.getKeyCode() != KeyEvent.KEYCODE_ALL_APPS) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
            Intent home = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setPackage(getPackageName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(home);
        }
        return true;
    }

    static String getActionType(String target) {
        if (target == null) return null;
        String t = target.trim();
        if ("quick_settings".equalsIgnoreCase(t)) return "quick_settings";
        if ("recent_apps".equalsIgnoreCase(t)) return "recent_apps";
        if ("inputs".equalsIgnoreCase(t)) return "inputs";
        if ("wallpaper_cycle".equalsIgnoreCase(t)) return "wallpaper_cycle";
        if ("wallpaper_picker".equalsIgnoreCase(t)) return "wallpaper_picker";
        return "app";
    }

    private void launchTarget(String target) {
        String type = getActionType(target);
        if ("quick_settings".equals(type)) {
            Intent intent = new Intent(this, QuickSettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("HomeKeyService", "Failed to start settings", e);
            }
            return;
        }

        if ("recent_apps".equals(type)) {
            Intent intent = new Intent(this, RecentAppsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try {
                startActivity(intent);
            } catch (Exception e) {
                android.util.Log.e("HomeKeyService", "Failed to start recent apps", e);
            }
            return;
        }

        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if ("inputs".equals(type)) {
            intent.setAction(MainActivity.ACTION_SHOW_INPUTS);
        } else if ("wallpaper_picker".equals(type)) {
            intent.setAction(MainActivity.ACTION_SHOW_WALLPAPERS);
        } else if ("wallpaper_cycle".equals(type)) {
            intent.setAction(MainActivity.ACTION_CYCLE_WALLPAPER);
        } else {
            intent.setAction(MainActivity.ACTION_LAUNCH_APP);
            intent.putExtra(MainActivity.EXTRA_PACKAGE_NAME, target);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("HomeKeyService", "Failed to start intent for " + target, e);
        }
    }

    private void launchPackage(String pkg) {
        try {
            Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
            if (intent == null) {
                intent = getPackageManager().getLaunchIntentForPackage(pkg);
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(intent);
            } else {
                android.util.Log.w("HomeKeyService", "No launch intent found for package: " + pkg);
            }
        } catch (Exception e) {
            android.util.Log.e("HomeKeyService", "Failed to launch package: " + pkg, e);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
