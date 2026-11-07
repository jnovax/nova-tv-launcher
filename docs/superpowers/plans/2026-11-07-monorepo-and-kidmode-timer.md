# Monorepo & Kid Mode Dynamic Time Pill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Chuyển đổi hệ thống `nova-tv-launcher` và `tv-parental-control` thành Monorepo Gradle Multi-Project với module `core/contract` dùng chung, hỗ trợ build/deploy độc lập từng app qua `tools/deploy.sh`, và tích hợp Dynamic Time Pill (`[ 🧒 12m ]`) hiển thị thời gian còn lại trong Kid Mode trên màn hình chính.

**Architecture:** Sử dụng kiến trúc Gradle Multi-Project với `apps/launcher`, `apps/parental-control` và thư viện Java thuần `core/contract` đóng vai trò Single Source of Truth cho các hằng số IPC và helper logic. Launcher lắng nghe trạng thái qua Broadcast và truy vấn ContentProvider của Parental Control khi `onResume()`, hiển thị đếm ngược thời gian với màu sắc traffic-light (>5m trắng, 3-5m vàng, <3m đỏ).

**Tech Stack:** Java 17, Android SDK 34 (minSdk 28), Gradle 8.5.2, Android Gradle Plugin, JUnit 4, Bash ADB tooling.

**Spec:** [docs/superpowers/specs/2026-11-07-monorepo-and-kidmode-timer-design.md](file:///home/lamnh/projects/nova-tv-launcher/docs/superpowers/specs/2026-11-07-monorepo-and-kidmode-timer-design.md)

## Global Constraints
- Target device: TCL Smart TV Pro (`192.168.1.7:5555`, Android 14 SDK 34, Board `merak`).
- JDK / Android Home: `JAVA_HOME=/opt/jdk17`, `ANDROID_HOME=/opt/android-sdk`.
- Backward compatibility: Không phá vỡ chức năng launcher hiện tại, giữ nguyên Remote Mapping và wallpaper settings.
- Independent deploy: Luôn cho phép build/deploy riêng từng ứng dụng hoặc toàn bộ.

---

### Task 1: Monorepo Scaffolding & Git Migration

**Files:**
- Move: `app/` ➔ `apps/launcher/`
- Copy/Merge: `/home/lamnh/projects/tv-parental-control/app/` ➔ `apps/parental-control/`
- Modify: `settings.gradle`
- Modify: `build.gradle`

**Interfaces:**
- Produces: Cấu trúc thư mục Gradle Multi-Project với 2 app modules `:apps:launcher` và `:apps:parental-control`.

- [ ] **Step 1: Move existing launcher app to `apps/launcher`**
```bash
mkdir -p apps
git mv app apps/launcher
```

- [ ] **Step 2: Copy `tv-parental-control` app into `apps/parental-control`**
```bash
mkdir -p apps/parental-control
cp -r /home/lamnh/projects/tv-parental-control/app/* apps/parental-control/
```

- [ ] **Step 3: Update `settings.gradle` to include both apps**
Edit `settings.gradle`:
```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nova-tv"
include ':apps:launcher'
include ':apps:parental-control'
```

- [ ] **Step 4: Update root `build.gradle` for Kotlin and Android plugin management**
Edit `build.gradle`:
```groovy
plugins {
    id 'com.android.application' version '8.5.2' apply false
    id 'com.android.library' version '8.5.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false
}
```

- [ ] **Step 5: Verify multi-project build setup**
Run: `./gradlew projects`
Expected: Lists `:apps:launcher` and `:apps:parental-control`.

- [ ] **Step 6: Commit Monorepo scaffolding**
```bash
git add settings.gradle build.gradle apps/
git commit -m "refactor(monorepo): scaffold multi-project structure with apps/launcher and apps/parental-control"
```

---

### Task 2: Shared Contract Module (`core/contract`)

**Files:**
- Create: `core/contract/build.gradle`
- Create: `core/contract/src/main/java/com/appletv/contract/ModeContract.java`
- Create: `core/contract/src/main/java/com/appletv/contract/StatusProviderContract.java`
- Create: `core/contract/src/main/java/com/appletv/contract/TimeFormatter.java`
- Test: `core/contract/src/test/java/com/appletv/contract/TimeFormatterTest.java`
- Modify: `settings.gradle`
- Modify: `apps/launcher/build.gradle`
- Modify: `apps/parental-control/build.gradle.kts`

**Interfaces:**
- Produces: `com.appletv.contract.TimeFormatter.formatShortRemaining(long seconds) -> String`
- Produces: `com.appletv.contract.ModeContract` constants
- Produces: `com.appletv.contract.StatusProviderContract` constants

- [ ] **Step 1: Write failing test for `TimeFormatter`**
Create `core/contract/src/test/java/com/appletv/contract/TimeFormatterTest.java`:
```java
package com.appletv.contract;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TimeFormatterTest {

    @Test
    public void formatsZeroAndNegativeSeconds() {
        assertEquals("0s", TimeFormatter.formatShortRemaining(0));
        assertEquals("0s", TimeFormatter.formatShortRemaining(-10));
    }

    @Test
    public void formatsSecondsUnderOneMinute() {
        assertEquals("45s", TimeFormatter.formatShortRemaining(45));
        assertEquals("1s", TimeFormatter.formatShortRemaining(1));
        assertEquals("59s", TimeFormatter.formatShortRemaining(59));
    }

    @Test
    public void formatsMinutesRoundingUp() {
        assertEquals("1m", TimeFormatter.formatShortRemaining(60));
        assertEquals("2m", TimeFormatter.formatShortRemaining(61));
        assertEquals("12m", TimeFormatter.formatShortRemaining(720));
        assertEquals("15m", TimeFormatter.formatShortRemaining(900));
    }
}
```

- [ ] **Step 2: Create `core/contract/build.gradle` and include in `settings.gradle`**
Create `core/contract/build.gradle`:
```groovy
plugins {
    id 'java-library'
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    testImplementation 'junit:junit:4.13.2'
}
```
Update `settings.gradle` to include `:core:contract`.

- [ ] **Step 3: Run test to verify it fails (RED)**
Run: `./gradlew :core:contract:test`
Expected: FAIL (cannot find symbol TimeFormatter).

- [ ] **Step 4: Implement `TimeFormatter`, `ModeContract`, and `StatusProviderContract`**
Create `core/contract/src/main/java/com/appletv/contract/TimeFormatter.java`:
```java
package com.appletv.contract;

public final class TimeFormatter {
    private TimeFormatter() {}

    public static String formatShortRemaining(long seconds) {
        if (seconds <= 0) return "0s";
        if (seconds < 60) return seconds + "s";
        long minutes = (seconds + 59) / 60;
        return minutes + "m";
    }
}
```

Create `core/contract/src/main/java/com/appletv/contract/ModeContract.java`:
```java
package com.appletv.contract;

public final class ModeContract {
    private ModeContract() {}

    public static final String MODE_KID = "KID";
    public static final String MODE_PARENT = "PARENT";

    public static final String TARGET_PACKAGE = "vn.familytv.parentalcontrol";
    public static final String ACTION_MODE_CHANGED = "vn.familytv.parentalcontrol.ACTION_MODE_CHANGED";
    public static final String ACTION_AUTHENTICATE = "vn.familytv.parentalcontrol.ACTION_AUTHENTICATE";

    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_ALLOWED_PACKAGES = "extra_allowed_packages";
    public static final String EXTRA_REMAINING_SECONDS = "extra_remaining_seconds";
}
```

Create `core/contract/src/main/java/com/appletv/contract/StatusProviderContract.java`:
```java
package com.appletv.contract;

public final class StatusProviderContract {
    private StatusProviderContract() {}

    public static final String AUTHORITY = "vn.familytv.parentalcontrol.provider";
    public static final String STATUS_PATH = "status";
    public static final String CONTENT_URI_STRING = "content://" + AUTHORITY + "/" + STATUS_PATH;

    public static final String COLUMN_CURRENT_MODE = "current_mode";
    public static final String COLUMN_REMAINING_SECONDS = "remaining_seconds";
    public static final String COLUMN_SESSION_LIMIT_SECONDS = "session_limit_seconds";
    public static final String COLUMN_WHITELIST_PACKAGES = "whitelist_packages";
}
```

- [ ] **Step 5: Run test to verify it passes (GREEN)**
Run: `./gradlew :core:contract:test`
Expected: PASS.

- [ ] **Step 6: Add `:core:contract` dependency to `:apps:launcher` and `:apps:parental-control`**
In `apps/launcher/build.gradle`:
```groovy
dependencies {
    implementation project(':core:contract')
    ...
```
In `apps/parental-control/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":core:contract"))
    ...
```

- [ ] **Step 7: Commit shared contract module**
```bash
git add core/ apps/ settings.gradle
git commit -m "feat(core): add shared contract module with ModeContract, StatusProviderContract, and TimeFormatter"
```

---

### Task 3: Selective Deployment Tool (`tools/deploy.sh`)

**Files:**
- Create: `tools/deploy.sh`
- Test: Local execution check

**Interfaces:**
- Produces: Executable `./tools/deploy.sh <launcher|parental-control|all> [TARGET_DEV]`

- [ ] **Step 1: Write `tools/deploy.sh` script**
Create `tools/deploy.sh`:
```bash
#!/usr/bin/env bash
# tools/deploy.sh — Selective APK builder and deployer
# Usage: ./tools/deploy.sh <launcher|parental-control|all> [TARGET_DEV]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

TARGET="${1:-all}"
TARGET_DEV="${2:-}"

if [ -z "$TARGET_DEV" ]; then
    TARGET_DEV=$(adb devices | grep -E '\b(device)\b' | awk '{print $1}' | head -n 1 || true)
fi

if [ -z "$TARGET_DEV" ]; then
    echo "Error: No ADB device connected." >&2
    exit 1
fi

echo "=========================================================="
echo "  🚀 Deploying Target: $TARGET to $TARGET_DEV"
echo "=========================================================="

deploy_launcher() {
    echo "==> Building Launcher APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:launcher:assembleRelease)
    local apk="$ROOT_DIR/apps/launcher/build/outputs/apk/release/apps-launcher-release.apk"
    if [ ! -f "$apk" ]; then
        apk="$ROOT_DIR/apps/launcher/build/outputs/apk/release/launcher-release.apk"
    fi
    if [ ! -f "$apk" ]; then
        apk=$(find "$ROOT_DIR/apps/launcher/build/outputs/apk/release" -name "*.apk" | head -n 1)
    fi
    echo "==> Installing Launcher to $TARGET_DEV..."
    adb -s "$TARGET_DEV" install -r "$apk"
    echo "==> Restarting Launcher..."
    adb -s "$TARGET_DEV" shell am start -n com.appletv.launcher/.MainActivity
}

deploy_parental() {
    echo "==> Building Parental Control APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:parental-control:assembleRelease)
    local apk=$(find "$ROOT_DIR/apps/parental-control/build/outputs/apk/release" -name "*.apk" | head -n 1)
    echo "==> Installing Parental Control to $TARGET_DEV..."
    adb -s "$TARGET_DEV" install -r "$apk"
}

case "$TARGET" in
    launcher)
        deploy_launcher
        ;;
    parental-control|parental)
        deploy_parental
        ;;
    all)
        deploy_parental
        deploy_launcher
        ;;
    *)
        echo "Error: Unknown target '$TARGET'. Use: launcher | parental-control | all" >&2
        exit 1
        ;;
esac

echo "✅ Deploy completed successfully!"
```

- [ ] **Step 2: Make `tools/deploy.sh` executable**
```bash
chmod +x tools/deploy.sh
```

- [ ] **Step 3: Test `tools/deploy.sh` argument validation**
Run: `./tools/deploy.sh invalid_target 2>&1 || true`
Expected: Output containing "Unknown target".

- [ ] **Step 4: Commit `tools/deploy.sh`**
```bash
git add tools/deploy.sh
git commit -m "feat(tools): add selective deploy.sh script for launcher and parental control"
```

---

### Task 4: Parental Control ContentProvider Integration

**Files:**
- Modify/Create: `apps/parental-control/src/main/java/vn/familytv/parentalcontrol/provider/StatusProvider.kt`
- Modify: `apps/parental-control/src/main/AndroidManifest.xml`
- Test: `apps/parental-control/src/test/java/vn/familytv/parentalcontrol/provider/StatusProviderTest.kt`

**Interfaces:**
- Produces: ContentProvider responding to `StatusProviderContract.CONTENT_URI_STRING` with `COLUMN_CURRENT_MODE` and `COLUMN_REMAINING_SECONDS`.

- [ ] **Step 1: Check existing status provider in `apps/parental-control`**
Verify whether `StatusProvider` exists or needs schema alignment with `StatusProviderContract`.

- [ ] **Step 2: Implement or update `StatusProvider` to return `remaining_seconds`**
Ensure it returns cursor with:
- `COLUMN_CURRENT_MODE`: "KID" / "PARENT"
- `COLUMN_REMAINING_SECONDS`: remaining seconds from active ScreenTime timer
- `COLUMN_SESSION_LIMIT_SECONDS`: 900
- `COLUMN_WHITELIST_PACKAGES`: comma-separated whitelist

- [ ] **Step 3: Register provider in `apps/parental-control/src/main/AndroidManifest.xml`**
```xml
<provider
    android:name=".provider.StatusProvider"
    android:authorities="vn.familytv.parentalcontrol.provider"
    android:exported="true" />
```

- [ ] **Step 4: Run unit tests for `apps/parental-control`**
Run: `./gradlew :apps:parental-control:testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit Parental Control changes**
```bash
git add apps/parental-control/
git commit -m "feat(parental-control): expose remaining screen time via StatusProvider"
```

---

### Task 5: Dynamic Time Pill in Launcher (`MainActivity` & `ModeManager`)

**Files:**
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/ModeManager.java`
- Modify: `apps/launcher/src/main/java/com/appletv/launcher/MainActivity.java`
- Test: `apps/launcher/src/test/java/com/appletv/launcher/ModeManagerTimeTest.java`

**Interfaces:**
- Consumes: `com.appletv.contract.TimeFormatter`
- Consumes: `com.appletv.contract.StatusProviderContract`
- Produces: Dynamic label update on `btnMode` (`[ 🧒 12m ]`) with color styling.

- [ ] **Step 1: Write failing test for `ModeManager.queryRemainingSeconds`**
Create `apps/launcher/src/test/java/com/appletv/launcher/ModeManagerTimeTest.java`:
```java
package com.appletv.launcher;

import static org.junit.Assert.assertEquals;
import com.appletv.contract.TimeFormatter;
import org.junit.Test;

public class ModeManagerTimeTest {

    @Test
    public void pillTextFormattedCorrectlyForKidMode() {
        assertEquals("15m", TimeFormatter.formatShortRemaining(900));
        assertEquals("12m", TimeFormatter.formatShortRemaining(720));
        assertEquals("45s", TimeFormatter.formatShortRemaining(45));
    }

    @Test
    public void trafficLightColorThresholds() {
        assertEquals(0xFFFFFFFF, ModeManager.getTimePillColor(600)); // > 5m: white
        assertEquals(0xFFFFD60A, ModeManager.getTimePillColor(240)); // 2-5m: gold
        assertEquals(0xFFFF453A, ModeManager.getTimePillColor(90));  // < 2m: red
    }
}
```

- [ ] **Step 2: Run test to verify it fails (RED)**
Run: `./gradlew :apps:launcher:testDebugUnitTest --tests com.appletv.launcher.ModeManagerTimeTest`
Expected: FAIL (cannot find symbol getTimePillColor).

- [ ] **Step 3: Implement helper methods in `ModeManager`**
In `ModeManager.java`:
```java
public static int getTimePillColor(long remainingSeconds) {
    if (remainingSeconds > 300) {
        return 0xFFFFFFFF; // White
    } else if (remainingSeconds > 120) {
        return 0xFFFFD60A; // Warning Gold
    } else {
        return 0xFFFF453A; // Alert Red
    }
}

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
```

- [ ] **Step 4: Run test to verify it passes (GREEN)**
Run: `./gradlew :apps:launcher:testDebugUnitTest --tests com.appletv.launcher.ModeManagerTimeTest`
Expected: PASS.

- [ ] **Step 5: Integrate Dynamic Time Pill in `MainActivity.java`**
In `MainActivity.java`:
- Add `private final Handler timerHandler = new Handler(Looper.getMainLooper());`
- Add `private final Runnable timerRunnable = this::updateTimePillTick;`
- In `updateModeUI()`:
  - If `isKidMode`:
    - Query `remaining = ModeManager.getRemainingSecondsFromProvider(this)`.
    - If `remaining >= 0`:
      - Set text: `btnMode.setText(TimeFormatter.formatShortRemaining(remaining));`
      - Set text/icon tint: `btnMode.setTextColor(ModeManager.getTimePillColor(remaining));`
    - Else fallback to `btnMode.setText(R.string.mode_kid)`.
  - If `isParentMode`:
    - Set text: `btnMode.setText(R.string.mode_parent);`
    - Set text color: `0xFFFFFFFF`.
- In `onResume()`: Start `timerHandler.post(timerRunnable)`.
- In `onPause()`: Stop `timerHandler.removeCallbacks(timerRunnable)`.

- [ ] **Step 6: Run all unit tests**
Run: `./gradlew test`
Expected: All tests pass across `:core:contract`, `:apps:launcher`, and `:apps:parental-control`.

- [ ] **Step 7: Commit Launcher dynamic time pill changes**
```bash
git add apps/launcher/
git commit -m "feat(launcher): implement dynamic time pill with countdown and traffic light colors in Kid Mode"
```

---

### Task 6: Full Verification & TV Deployment

**Files:**
- Full repository verification

- [ ] **Step 1: Build release APKs for all targets**
Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL with 0 errors.

- [ ] **Step 2: Deploy Parental Control to TV**
Run: `./tools/deploy.sh parental-control 192.168.1.7:5555`
Expected: Streamed install Success.

- [ ] **Step 3: Deploy Launcher to TV**
Run: `./tools/deploy.sh launcher 192.168.1.7:5555`
Expected: Streamed install Success, launcher restarts.

- [ ] **Step 4: Capture screenshot to visually verify on TV**
Run: `adb -s 192.168.1.7:5555 shell screencap -p /data/local/tmp/monorepo_test.png && adb -s 192.168.1.7:5555 pull /data/local/tmp/monorepo_test.png /tmp/monorepo_test.png`
Verify: Top bar displays `[ 🧒 12m ]` or `[ 🧒 15m ]`.
