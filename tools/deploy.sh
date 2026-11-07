#!/usr/bin/env bash
# tools/deploy.sh — Selective APK builder and deployer
# Usage: ./tools/deploy.sh <launcher|parental-control|all> [TARGET_DEV]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Ensure Java and Android SDK environment if present
if [ -z "${JAVA_HOME:-}" ] && [ -d "/opt/jdk17" ]; then
    export JAVA_HOME="/opt/jdk17"
fi
if [ -z "${ANDROID_HOME:-}" ] && [ -d "/opt/android-sdk" ]; then
    export ANDROID_HOME="/opt/android-sdk"
fi
if [ -n "${JAVA_HOME:-}" ] || [ -n "${ANDROID_HOME:-}" ]; then
    export PATH="${JAVA_HOME:-/usr}/bin:${ANDROID_HOME:-/opt/android-sdk}/platform-tools:${ANDROID_HOME:-/opt/android-sdk}/cmdline-tools/latest/bin:$PATH"
fi

TARGET="${1:-all}"
TARGET_DEV="${2:-}"

# Validate target argument before device discovery
case "$TARGET" in
    launcher|parental-control|parental|all)
        ;;
    *)
        echo "Error: Unknown target '$TARGET'. Use: launcher | parental-control | all" >&2
        exit 1
        ;;
esac

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
        apk=$(find "$ROOT_DIR/apps/launcher/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: Launcher APK not found in $ROOT_DIR/apps/launcher/build/outputs/apk/release" >&2
        exit 1
    fi
    echo "==> Installing Launcher to $TARGET_DEV..."
    adb -s "$TARGET_DEV" install -r "$apk"
    echo "==> Restarting Launcher..."
    adb -s "$TARGET_DEV" shell am start -n com.appletv.launcher/.MainActivity
}

deploy_parental() {
    echo "==> Building Parental Control APK..."
    (cd "$ROOT_DIR" && ./gradlew :apps:parental-control:assembleRelease)
    local apk=""
    if [ -d "$ROOT_DIR/apps/parental-control/build/outputs/apk/release" ]; then
        apk=$(find "$ROOT_DIR/apps/parental-control/build/outputs/apk/release" -name "*.apk" 2>/dev/null | head -n 1 || true)
    fi
    if [ -z "$apk" ] || [ ! -f "$apk" ]; then
        echo "Error: Parental Control APK not found in $ROOT_DIR/apps/parental-control/build/outputs/apk/release" >&2
        exit 1
    fi
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
esac

echo "✅ Deploy completed successfully!"
