#!/usr/bin/env bash
# Install a local APK on a connected device after saving its current state.
# Usage: ./tools/deploy_apk.sh <apk> <package> <device>
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
export PATH="/opt/android-sdk/platform-tools:$PATH"
APK="${1:?APK path required}"
PACKAGE="${2:?Package name required}"
DEVICE="${3:?Device required}"
[ -f "$APK" ] || { echo "APK not found: $APK" >&2; exit 1; }
adb -s "$DEVICE" get-state | grep -qx device
TAG="$(date +%Y%m%d_%H%M%S)_apk"
"$SCRIPT_DIR/backup.sh" "$DEVICE" "$TAG"
CLEAN_DEV=$(printf '%s' "$DEVICE" | tr -c 'A-Za-z0-9._-' '_')
SNAPSHOT="$ROOT_DIR/backups/${CLEAN_DEV}_${TAG}"
test -s "$SNAPSHOT/metadata.env"
OLD_PATH=$(adb -s "$DEVICE" shell pm path "$PACKAGE" | tr -d '\r' | sed -n 's/^package://p' | head -n 1)
if [ -n "$OLD_PATH" ]; then
    adb -s "$DEVICE" pull "$OLD_PATH" "$SNAPSHOT/previous.apk"
fi
adb -s "$DEVICE" install -r "$APK"
adb -s "$DEVICE" shell pm path "$PACKAGE"
echo "Snapshot: $SNAPSHOT"
