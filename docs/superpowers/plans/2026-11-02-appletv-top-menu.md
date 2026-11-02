# Apple TV Top Menu & Control Center Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the tvOS 18 Top Menu and interactive Control Center popups from the web preview prototype into the native Android TV launcher codebase, and deploy to TV.

**Architecture:** Refactor the top menu bar to standard tvOS order (Clock | Inputs | Wi-Fi | Settings) with Wallpaper moved into Settings. Implement authentic tvOS frosted-glass Control Center popups for Settings (featuring Brightness slider, Volume slider, System Settings, Wallpaper picker, Launcher Settings, and Sleep/Restart actions) and Wi-Fi (status, IP, signal, and quick settings link), while enhancing the existing Inputs dropdown.

**Tech Stack:** Android Java (SDK 28-34), Android XML Layouts, Vector Drawables, StateListAnimators, TV D-Pad Focus & Audio/Brightness APIs, MozJPEG, ADB deployment.

**Spec:** [`docs/preview/index.html`](file:///home/lamnh/projects/nova-tv-launcher/docs/preview/index.html) (interactive demo and design reference).

## Global Constraints
- Target Android SDK 34, minSdk 28.
- Never intercept volume or navigation keys in accessibility service.
- Maintain D-Pad focus traversal and smooth tvOS 18 spring/ease animations.
- Ensure all popups dismiss gracefully with Back key (`popups.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT))`).
- Must compile cleanly with `./gradlew assembleRelease` and install on TV (`192.168.1.10:5555`).

---

### Task 1: Reorganize Top Menu Bar Layout

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml:190-230`
- Modify: `app/src/main/res/layout-iw/activity_main.xml:190-230`
- Modify: `app/src/main/java/com/appletv/launcher/MainActivity.java:335-385`

**Interfaces:**
- Consumes: `activity_main.xml` layout elements
- Produces: Clean 3-button menu bar platter (`btnInputs`, `btnWifi`, `btnSettings`) with `btnWallpaper` hidden from the platter.

- [ ] **Step 1: Hide `btnWallpaper` from top bar in `activity_main.xml` and `activity_main.xml (iw)`**
Set `android:visibility="gone"` on `btnWallpaper` so the platter displays only `[Clock] | Inputs | Wi-Fi | Settings`.

- [ ] **Step 2: Update focus navigation in `MainActivity.java`**
Update `pillKeys` in `MainActivity.java` so that D-Pad left/right limits correctly navigate across `btnInputs`, `btnWifi`, `btnSettings`.

- [ ] **Step 3: Verify build**
Run `./gradlew assembleRelease` to confirm syntax and resource references are valid.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/res/layout-iw/activity_main.xml app/src/main/java/com/appletv/launcher/MainActivity.java
git commit -m "feat(ui): streamline menu bar platter to Inputs, Wi-Fi, and Settings"
```

---

### Task 2: Create tvOS Control Center Sliders & Popup Layouts

**Files:**
- Create: `app/src/main/res/layout/popup_settings.xml`
- Create: `app/src/main/res/layout/popup_wifi.xml`
- Create: `app/src/main/res/drawable/tvos_slider_track.xml`
- Create: `app/src/main/res/drawable/tvos_slider_thumb.xml`
- Create: `app/src/main/res/drawable/tvos_slider_progress.xml`
- Create: `app/src/main/res/drawable/tvos_slider_bg.xml`
- Create: `app/src/main/res/drawable/badge_bg.xml`
- Create: `app/src/main/res/drawable/action_circle_btn.xml`

**Interfaces:**
- Consumes: `@drawable/glass_panel`, `@style/TopButton`
- Produces: Visual popup cards matching `docs/preview/index.html` specs.

- [ ] **Step 1: Create slider drawables and styles**
Build custom progress drawables for TV brightness & volume sliders (pill thumb, soft glowing track on focus).

- [ ] **Step 2: Create `popup_settings.xml`**
Implement the Control Center card layout:
  - Header: "Settings" title + "Control Center" badge
  - Brightness Card: Sun icon, "Brightness", percentage label, SeekBar
  - Volume Card: Speaker icon, "Volume", percentage label, SeekBar
  - Menu Items: TV System Settings, Wallpaper, Picture & Display
  - Footer: "Launcher Settings" button + Sleep button + Restart button

- [ ] **Step 3: Create `popup_wifi.xml`**
Implement the Wi-Fi card layout:
  - Header: "Wi-Fi & Network" title + "Connected" green badge
  - Active network info card: Wi-Fi icon, SSID, IP address, link speed
  - Shortcut row: "Wi-Fi Settings..." with gear/arrow icon

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/layout/popup_settings.xml app/src/main/res/layout/popup_wifi.xml app/src/main/res/drawable/
git commit -m "feat(ui): add layout and drawables for tvOS Control Center popups"
```

---

### Task 3: Implement Settings & Wi-Fi Popup Logic in MainActivity

**Files:**
- Modify: `app/src/main/java/com/appletv/launcher/MainActivity.java`

**Interfaces:**
- Consumes: `popup_settings.xml`, `popup_wifi.xml`, `AudioManager`, `Settings.System`
- Produces: `showControlCenterSettings()` and `showWifiPopup()` methods triggered on clicking `btnSettings` and `btnWifi`.

- [ ] **Step 1: Implement `showControlCenterSettings()`**
Inflate `popup_settings.xml`, wire up:
  - Brightness slider (`SeekBar`) with current system/window brightness, onKey/onProgress update
  - Volume slider (`SeekBar`) with `AudioManager.STREAM_MUSIC`, onKey/onProgress update
  - TV System Settings row click -> `systemIntent(Settings.ACTION_SETTINGS)`
  - Wallpaper row click -> dismiss popup & `showWallpaperPickerGrid()`
  - Picture & Display row click -> toast resolution info (4K UHD 60Hz)
  - Launcher Settings button click -> `startActivityForResult(LauncherSettingsActivity.class)`
  - Sleep button click -> send standby/sleep command
  - Restart button click -> show confirmation AlertDialog, restart on confirm

- [ ] **Step 2: Implement `showWifiPopup()`**
Inflate `popup_wifi.xml`, populate current Wi-Fi SSID, IP address (`192.168.1.10`), link speed, and click action to open system Wi-Fi settings.

- [ ] **Step 3: Connect `btnSettings` and `btnWifi` click listeners**
Update `btnSettings.setOnClickListener(v -> showControlCenterSettings())` and `btnWifi.setOnClickListener(v -> showWifiPopup())`.

- [ ] **Step 4: Verify build with `./gradlew assembleRelease`**
Verify zero compilation or lint errors.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/appletv/launcher/MainActivity.java
git commit -m "feat(control-center): implement Settings and Wi-Fi popups in MainActivity"
```

---

### Task 4: Verification & TV Deployment

**Files:**
- Verify: `app/build/outputs/apk/release/app-release.apk`
- Target: TV `192.168.1.10:5555`

- [ ] **Step 1: Build release APK**
Run `./gradlew assembleRelease`.

- [ ] **Step 2: Deploy to TV via ADB**
Run `adb -s 192.168.1.10:5555 install -r app/build/outputs/apk/release/app-release.apk`.

- [ ] **Step 3: Launch and capture TV screenshots**
Trigger `am start` and send D-pad keys to open Settings and Wi-Fi popups, capturing screenshots via `screencap` to verify aesthetic quality.
