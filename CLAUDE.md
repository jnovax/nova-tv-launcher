# CLAUDE.md — Operating Guidelines for Claude Code & AI Assistants

This repository contains **Apple TV Launcher**, an ultra-lightweight (~218 KB) Android TV / Google TV launcher and safety debloat suite.

## 🛠️ Unified Tool Suite (Always Use These, Never Invent Custom ADB Scripts)

1. **`./tools/audit.sh [TV_IP]`**:
   - 100% Read-Only TV inspection (hardware, packages, Home owner, HDMI inputs).
   - Captures Day-1 safety baseline in `backups/<device>_original/`.
   - Generates `reports/report_<ts>.md` and editable `reports/plan_<ts>.txt`.

2. **`./tools/apply.sh <plan_file> [TV_IP]`** (or `bash reports/apply_<ts>.sh`):
   - Executes approved plan with atomic safety:
     - Pre-flight APK checks
     - Timestamped safety snapshot in `backups/<device>_<ts>/`
     - Apple TV Launcher install & launch test (`am start -W`)
     - Auto-grants `GET_USAGE_STATS` (Recent Apps) & enables `HomeKeyService`
     - Verified Home handover to `com.appletv.launcher` with auto-rollback
     - Debloats approved packages (strictly skipping `tools/protected.txt` & HDMI inputs)
     - SmartTube install & stock YouTube disable

3. **`./tools/restore.sh <snapshot_dir> [TV_IP]`**:
   - 100% rollback: re-enables all system packages, restores stock launcher, reverts animations.
   - Cleans up SmartTube and Apple TV Launcher on request.

4. **`./tools/sync_wallpaper.sh <image_path> [TV_IP]`**:
   - Normalizes wallpapers to 4K UHD (`3840x2160`, MozJPEG q=92) and syncs via `/data/local/tmp` staging.
   - Detailed specifications: [`docs/WALLPAPERS.md`](docs/WALLPAPERS.md).
   - Remote mapping details: [`docs/REMOTE_MAPPING.md`](docs/REMOTE_MAPPING.md).

## 💻 Build & Development Environment

- **Java JDK:** `JAVA_HOME=/opt/jdk17` (OpenJDK 17.0.21)
- **Android SDK:** `ANDROID_HOME=/opt/android-sdk`
- **Local SDK Configuration:** `local.properties` contains `sdk.dir=/opt/android-sdk`
- **Environment PATH:** `$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH`
- **Build Command:** `./gradlew assembleRelease`
- **Detailed Reference:** [`docs/ENVIRONMENT.md`](docs/ENVIRONMENT.md)

## 🛡️ Critical Guardrails for AI Assistants

- **Autonomous Approval Ban:** You must NEVER execute `./tools/setup.sh -y` or `./tools/apply.sh` autonomously without the user explicitly stating "Approved" or "Proceed" in chat.
- **Protected Packages SSOT:** Honor `tools/protected.txt` at all times (Chromecast, Assistant, Gboard, Bluetooth, Wi-Fi, Settings, System UI, GMS, Setup Wizards).
- **Stock Launcher Protection:** Stock launchers (`launcherx`, `tvlauncher`, `leanbacklauncher`, `mitv.tvhome`) must NEVER be disabled via generic debloat; they are handled exclusively by Phase 3 handover in `apply.sh`.
- **Preserve Remote Repeat Timers:** Volume keys must NEVER be intercepted by accessibility services to preserve Android's native Linux `InputDispatcher` repeat rate.
