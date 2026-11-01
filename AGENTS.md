# Agent Runbook: Autonomous Android TV Transformation

This document is the single source of truth for AI agents (Antigravity, Claude Code, Cursor, Devin, Roo Code, Aider, Windsurf) executing tasks in this repository.

---

## 🎯 Primary Goal
Transform any connected Android TV or Google TV (Sony, TCL, Xiaomi, OnePlus, Hisense, Shield, Chromecast, Fire TV) into an ultra-clean, ad-free Apple TV experience with:
1. Pure pitch-black tvOS 18 leanback grid with dynamic ambient RGB glow.
2. Native Apple TV Recent Apps Switcher (`RecentAppsActivity`) with double-press Home gesture.
3. 100% hardware-speed volume hold acceleration preserved (zero accessibility lag).
4. Official Ad-Free SmartTube replacing stock YouTube.
5. Safe, reversible multi-brand debloating.

---

## ⚡ Standard Execution Workflow (Audit First)

When a user asks you to set up their TV at an IP address:

```bash
# Step 1: Run read-only audit (ZERO changes to TV)
./tools/audit.sh <TV_IP>

# Step 2: Present summary from reports/report_<ts>.md to user and get approval.

# Step 3: Run approved apply script
bash reports/apply_<ts>.sh
```

*(Optional 1-Click Interactive Mode: `./tools/setup.sh <TV_IP>`)*
## 🛠️ Tool Suite Reference

The repository provides a single, unified tool suite. Always use these tools rather than manual raw ADB commands:

1. **`./tools/audit.sh [TV_IP]`**:
   - 100% Read-Only inspection of TV specs, running packages, Home owner, and HDMI inputs.
   - Saves Day-1 baseline snapshot in `backups/<device>_original/`.
   - Generates `reports/report_<ts>.md` and editable `reports/plan_<ts>.txt`.

2. **`./tools/apply.sh <plan_file> [TV_IP]`** (or `bash reports/apply_<ts>.sh`):
   - Executes approved plan with atomic safety:
     - Pre-flight APK download & verification
     - Safety state snapshot in `backups/<device>_<ts>/`
     - Apple TV Launcher install & launch test (`am start -W`)
     - Home handover to `com.appletv.launcher` with auto-rollback
     - Debloat approved packages (strictly skipping `tools/protected.txt`)
     - SmartTube installation & stock YouTube disable

3. **`./tools/restore.sh <snapshot_dir> [TV_IP]`**:
   - Restores original stock launcher as default Home.
   - Re-enables all original system packages.
   - Resets animation scales back to original values.
   - Re-enables stock YouTube.
   - Offers clean uninstallation of Apple TV Launcher and SmartTube.

4. **`./tools/sync_wallpaper.sh <image_path> [TV_IP]`**:
   - Normalizes any input image to native 4K UHD (`3840x2160`, center-crop, MozJPEG quality 92).
   - Syncs safely via `/data/local/tmp` staging to bypass Android TV FUSE `fchown` restriction.
   - Destination on TV: `/sdcard/Android/data/com.appletv.launcher/files/wallpapers/`.
   - Verified via MD5 checksum matching. See [`docs/WALLPAPERS.md`](docs/WALLPAPERS.md) for full TV specs and constraints.

---

## 🖼️ TV Profile & Wallpaper Specifications

- **Default TV Target**: `192.168.1.10:5555` (TCL Smart TV Pro `G08`, Android 14, Board: `merak`).
- **Display**: Physical 4K UHD (`3840x2160`), UI Override `1920x1080` (320 dpi).
- **Code Constraints**:
  - `WallpaperSize.MAX_PIXELS = 8_400_000L`: Any image > 8.4M pixels (e.g. 4072x2280) is forced to `inSampleSize = 2` (1080p downscaled decode).
  - Target format is strictly `.jpg` (required by `rotate_daily.sh` daemon).
  - Always stage through `/data/local/tmp` before copying to `/sdcard/...` to avoid ADB `fchown: Operation not permitted`.
  - Detailed reference: [`docs/WALLPAPERS.md`](docs/WALLPAPERS.md).
  - Remote mapping reference: [`docs/REMOTE_MAPPING.md`](docs/REMOTE_MAPPING.md).

---

## 💻 Build & Development Environment

- **Java Development Kit:** `JAVA_HOME=/opt/jdk17` (OpenJDK 17.0.21)
- **Android SDK:** `ANDROID_HOME=/opt/android-sdk`
- **Local SDK Configuration:** `local.properties` contains `sdk.dir=/opt/android-sdk`
- **System PATH:** `$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH`
- **Gradle Release Build:** `./gradlew assembleRelease`
- **Detailed Reference:** [`docs/ENVIRONMENT.md`](docs/ENVIRONMENT.md)

---

## 🛡️ Critical Guardrails for Agents

1. **NEVER Disable Protected System Services:**
   - Always honor `tools/protected.txt` (Chromecast, Assistant, Gboard, Wi-Fi, System UI, GMS, setup wizards).
   - Never disable stock launchers via generic debloat; launcher handover is exclusively handled by `apply.sh`.
2. **Preserve Linux `InputDispatcher` Repeat Rate (Volume Keys):**
   - Accessibility services (`HomeKeyService`, `InputKeyService`) exist exclusively for Home double-press and TV input switching.
   - Volume and navigation keys must NEVER be intercepted or consumed in accessibility services to preserve Android's native repeat queue and hold acceleration.
3. **Always Ensure Reversibility:**
   - Always verify that a snapshot exists before touching the TV.
   - To restore: `./tools/restore.sh backups/<device_snapshot_directory> [TV_IP]`.
4. **AI Agent Autonomous Approval Ban:**
   - AI agents must NEVER execute `./tools/setup.sh -y` or `./tools/apply.sh` autonomously without explicit user approval ("Approved" / "Proceed") in chat.
