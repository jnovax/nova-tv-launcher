# Apple TV Launcher for Android TV

<p align="center">
  <a href="https://github.com/Fardin7798/apple-tv-launcher/releases/latest"><img src="https://img.shields.io/github/v/release/Fardin7798/apple-tv-launcher?color=success&label=Release" alt="Release"></a>
  <a href="https://github.com/Fardin7798/apple-tv-launcher/releases/latest/download/app-release.apk"><img src="https://img.shields.io/badge/Download-APK%20(218%20KB)-brightgreen?logo=android" alt="Download APK"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-green.svg" alt="License"></a>
  <a href="https://developer.android.com/about/versions/pie"><img src="https://img.shields.io/badge/Android%20TV-9.0%2B-blue.svg" alt="Android TV"></a>
  <img src="https://img.shields.io/badge/APK%20Size-218%20KB-success.svg" alt="Size">
</p>

An ultra-lightweight, bloat-free Android TV launcher featuring a native **Apple TV-style Recent Apps Switcher**, visual 4K UHD wallpaper picker, remote double-press navigation, and 100% hardware-speed volume acceleration.

---

<p align="center">
  <img src="docs/Homescreen.png" alt="Apple TV Home Screen" width="850">
</p>
<p align="center"><em>Minimalist Pitch-Black Leanback Grid with Symmetrical Focus Scaling</em></p>

---

## ✨ Features

- 🍎 **tvOS 18-Inspired Interface:** Minimalist pitch-black home screen, borderless symmetrical icon focus scaling, uniform dock platter padding & gap geometry matching Apple tvOS specifications.
- 🖼️ **Native 4K UHD Wallpaper Engine:** Visual wallpaper grid picker with LruCache memory protection, remote 1-click wallpaper cycling with 400ms crossfade, top-left frosted glass banner indicator, and `ambient_glow` toggle for pure uncorrupted wallpaper colors.
- 🔄 **Apple TV Recent Apps Switcher:** Smooth horizontal carousel of recently used apps with live task switching, authentic usage stats tracking, tvOS empty state, and quick task controls via `RecentAppsActivity`.
- ⚡ **Zero Volume Lag (Hardware Preservation):** Unlike accessibility-based remappers that break Android's `InputDispatcher` repeat timer, our launcher preserves 100% hardware-speed volume hold acceleration.
- 🎮 **Dynamic Remote Button Mapping & Gestures:** Fully configurable key bindings via `config.json` for wallpaper switching, Quick Settings, TV inputs, and double-press Home gesture.
- 🛠️ **Automated 4K Wallpaper Pipeline (`sync_wallpaper.sh`):** Center-crop, MozJPEG q=92 normalization, FUSE-safe 2-stage staging push, and MD5 integrity verification.
- 🪶 **Ultra-Lightweight (~218 KB):** Pure Java, zero analytics, zero telemetry, zero background battery drain.
- 🧹 **Safe Debloat & Instant Rollback:** Verified multi-brand profiles (Sony, TCL, Xiaomi, OnePlus, Hisense, Shield, Chromecast, Fire TV) with Day-1 baseline snapshots and 1-click full restore.

---

## 📸 Screenshots

| | | |
|---|---|---|
| ![Recent Apps Switcher](docs/RecentTab.png) | ![Wallpaper Grid Picker](docs/WallpaperPicker.png) | ![Control Center](docs/Control_Center.png) |
| *Native Apple TV App Switcher (Double-Press Home)* | *tvOS 18 Visual 4K Wallpaper Grid Picker* | *Quick Settings & Control Center Platter* |
| ![TV Inputs Switcher](docs/Inputs.png) | ![Wallpaper Switch Banner](docs/WallpaperBanner.png) | ![tvOS Split-Pane Settings](docs/Settings.png) |
| *Quick TV Inputs Switcher (HDMI / AV)* | *Frosted Glass Wallpaper Indicator Banner* | *tvOS 18 Split-Pane Launcher Settings* |

---

## 🖼️ 4K UHD Wallpapers & Visual Picker

The launcher features an integrated 4K wallpaper ecosystem:
- **Visual Grid Picker (`SHOW_WALLPAPERS`):** High-speed horizontal carousel with LruCache thumbnail decoding (`RGB_565`), smooth tvOS focus ring, and persistent tracking that auto-scrolls to the active wallpaper.
- **Remote Cycling (`CYCLE_WALLPAPER`):** 1-button instant cycling between wallpapers with 400ms crossfade and top-left frosted glass banner.
- **Ambient Glow Toggle (`ambient_glow`):** Configurable in `config.xml` to prevent icon accent colors from tinting the background artwork.
- **Sync Pipeline:** Use `./tools/sync_wallpaper.sh <image_path> [TV_IP]` to normalize any image to 3840×2160 JPEG q=92, bypass Android FUSE `fchown` limits via `/data/local/tmp` staging, and verify with MD5 checksums.

👉 *See [`docs/WALLPAPERS.md`](docs/WALLPAPERS.md) for full TV hardware profile and technical constraints.*

---

## 🎮 Remote Button Remapping & Gestures

- **Double-Press Home:** Brings up the native Apple TV Recent Apps Switcher.
- **Dynamic Key Mappings:** Configure any remote button keycode in `config.json` to trigger `wallpaper_cycle`, `quick_settings`, `inputs`, or `recent_apps`.
- **Zero Accessibility Lag:** Non-mapped navigation and volume keys pass directly to Android's Linux `InputDispatcher` for 100% hardware-speed volume hold acceleration.

👉 *See [`docs/REMOTE_MAPPING.md`](docs/REMOTE_MAPPING.md) for keycodes, logcat discovery, and configuration examples.*

---

## 🚀 1-Click Universal TV Setup

Transform any Android TV or Google TV into an ultra-clean, ad-free Apple TV experience with automated debloating, SmartTube installation, and the 218 KB Apple TV launcher.

### 📋 Prerequisites & Requirements

Before running the setup, ensure:
1. 📶 **Same Wi-Fi Network:** Your computer (laptop/PC) and Android TV must be connected to the exact same Wi-Fi router / network.
2. 📺 **Android TV 9.0+:** Compatible with any TV, box, or stick running Android TV 9, 10, 11, 12, or 14 (Google TV).
3. ⚙️ **Enable USB Debugging on TV:**
   - Go to **Settings ➔ Device Preferences ➔ About**.
   - Scroll down to **Build** and click OK on your remote **7 times** (until you see *"You are now a developer"*).
   - Go back, open **Developer options**, and turn **USB debugging** ON (also turn **Network debugging** ON if present).
4. 💻 **ADB Tool:** Standard Android Platform Tools (`sudo apt install adb` on Linux, `brew install android-platform-tools` on macOS, or pre-bundled in PowerShell for Windows).

---

### 🤖 Option 1: Easiest Setup with Any AI Agent (Recommended for Beginners)

Don't want to deal with terminal commands? You can give this repository to any AI coding assistant (**Claude Code, Cursor, Devin, Antigravity, Roo Code, Windsurf, ChatGPT**):

| Step | Action |
|---|---|
| **1. On your TV** | Settings ➔ Device Preferences ➔ About ➔ Press OK on **Build** 7 times. Back ➔ Developer options ➔ Turn **USB Debugging** ON. |
| **2. In your AI Agent** | Copy and paste [`PROMPT.md`](PROMPT.md) into your assistant chat. |
| **3. Review & Approve** | Your assistant runs a read-only audit (`audit.sh`), shows you the hardware report & plan, and asks for your approval before touching anything! |

---

### 💻 Option 2: Terminal Workflow (For Power Users)

**1. Run Read-Only Audit (Zero changes made to TV):**
```bash
./tools/audit.sh <TV_IP>
```
*Outputs: `reports/report_<ts>.md` (Full audit) and `reports/plan_<ts>.txt` (Editable plan).*

**2. Review & Apply Approved Plan:**
```bash
bash reports/apply_<ts>.sh
```

*(Alternatively, run the interactive 1-click suite directly: `./tools/setup.sh <TV_IP>`)*

**What the automated setup does:**
- 🔍 **Auto-detects TV hardware & manufacturer** (Sony, TCL, Xiaomi, OnePlus, Hisense, Shield, Chromecast).
- 💾 **Creates an instant full-state safety snapshot** of all currently enabled packages, default Home launcher, animation scales, and system settings.
- 🧹 **Safely disables manufacturer bloatware, sponsored recommendation rows, and telemetry** via `pm disable-user`.
- 📺 **Replaces stock ad-heavy YouTube with official SmartTube** (Ad-free, SponsorBlock, TV-optimized).
- 🍎 **Installs and sets Apple TV Launcher** as the permanent default home activity.

---

### 🛠️ Option 3: Standalone Launcher Installation

If you only want the ultra-lightweight launcher without debloating:

**Build Environment Requirements:**
- **JDK:** OpenJDK 17 (`JAVA_HOME=/opt/jdk17`)
- **Android SDK:** `ANDROID_HOME=/opt/android-sdk` (ensure `local.properties` contains `sdk.dir=/opt/android-sdk`)
- **PATH:** `$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH`
- 👉 *See [`docs/ENVIRONMENT.md`](docs/ENVIRONMENT.md) for full environment setup.*

```bash
# 1. Build release APK (or use pre-built binary in app/build/outputs/apk/release/)
./gradlew assembleRelease

# 2. Connect to your TV over Wi-Fi
adb connect <TV_IP>:5555

# 3. Install the launcher
adb install -r app/build/outputs/apk/release/app-release.apk

# 4. Set as permanent default Home launcher
adb shell cmd package set-home-activity --user 0 com.appletv.launcher/.MainActivity

# 5. Grant usage access for Recent Apps Switcher
adb shell appops set com.appletv.launcher GET_USAGE_STATS allow
```

---

## 🛡️ Multi-Brand Debloat Profiles

Pre-configured, verified debloat profiles are organized in [`profiles/`](profiles/):

| Brand / Platform | Profile File | Target Capabilities Cleaned |
|---|---|---|
| **Xiaomi / Redmi** | [`profiles/xiaomi.txt`](profiles/xiaomi.txt) | PatchWall, Glance lockscreen ads, Mi Video, Mi Music, Mi Analytics |
| **Sony BRAVIA** | [`profiles/sony-bravia.txt`](profiles/sony-bravia.txt) | Samba TV ACR tracking, Sony Select promos, Sony Discover, Demo mode |
| **TCL** | [`profiles/tcl.txt`](profiles/tcl.txt) | TCL Channel, TCL TTVS tracker, TCL Assistant, recommendation feeds |
| **OnePlus TV** | [`profiles/oneplus.txt`](profiles/oneplus.txt) | OxygenPlay, OnePlus Store, HeyTap telemetry, preloaded browser |
| **Hisense** | [`profiles/hisense.txt`](profiles/hisense.txt) | Hisense telemetry, partner ad services, sponsored home rows |
| **NVIDIA SHIELD** | [`profiles/nvidia-shield.txt`](profiles/nvidia-shield.txt) | Shield recommendation rows, telemetry, partner promotions |
| **Chromecast with Google TV** | [`profiles/chromecast-gtv.txt`](profiles/chromecast-gtv.txt) | Google TV sponsored rows, partner auto-installs, feedback sender |
| **Amazon Fire TV** | [`profiles/amazon-fire-tv.txt`](profiles/amazon-fire-tv.txt) | Fire TV sponsored banners, forced promotions, metrics |
| **Universal Fallback** | [`profiles/universal-safe.txt`](profiles/universal-safe.txt) | Safe for any Android TV 9.0+ device |

> [!NOTE]
> **Safety Whitelist:** None of our profiles ever disable Chromecast built-in (`com.google.android.apps.mediashell`), Google Assistant voice search (`com.google.android.katniss`), Wi-Fi/Bluetooth stacks, or Gboard.

---

## ⏪ Instant Rollback & True Full-State Restore

Every modification is 100% reversible. A complete snapshot of your TV's original state (enabled packages, original default Home launcher, animation scales, and system settings) is preserved in `backups/<device>_original/` (Day-1 baseline) and `backups/<device>_<timestamp>/` (per-run).

> [!NOTE]
> **What is restored:** System packages, stock home launcher, animation scales, and system display settings are fully restored. (Private app data directories and saved Wi-Fi credentials cannot be accessed or modified over standard ADB without root access).

To restore your TV back to its exact untouched factory state:
```bash
./tools/restore.sh backups/<device_snapshot_directory>
```
*What the restore engine does:*
- 🔄 Re-enables all original system packages.
- 📺 Restores your original stock launcher as the default Home.
- ⚡ Reverts animation scales back to original values.
- 🍎 Offers interactive clean uninstallation of Apple TV Launcher and SmartTube.
---

## 🙏 Credits & Acknowledgments

This project is based on [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by [Gathin](https://github.com/Gathin23) and [Spider-Tv-Launcher](https://github.com/Fardin7798/Spider-Tv-Launcher) by [Fardin](https://github.com/Fardin7798). Parts of the launcher code (cards, grid navigation, sky/festival effects, settings) and the debloat workflow come from them, used under the MIT License.

New in Apple TV Launcher:
- Apple TV Recent Apps Switcher (`RecentAppsActivity`) with animated card carousel
- Remote double-press Home gesture & configurable dynamic key mapping
- Zero-lag volume hold acceleration (hardware preservation)
- tvOS 18 visual 4K wallpaper grid picker & 1-button remote cycling with frosted glass banner
- Symmetrical center-zoom icon focus & refined tvOS 18 dock platter geometry
- Configurable ambient glow toggle (`ambient_glow`) preserving true wallpaper colors
- Automated 4K wallpaper normalization & FUSE-safe sync pipeline (`tools/sync_wallpaper.sh`)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
