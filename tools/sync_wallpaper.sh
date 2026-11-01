#!/usr/bin/env bash
# ==============================================================================
# sync_wallpaper.sh — Standardized 4K UHD Wallpaper Processor & Sync Tool
# ==============================================================================
# Normalizes input wallpapers to Apple TV Launcher standards:
# - Resolution: 3840x2160 (16:9 4K UHD, center-crop)
# - Format: High Quality JPEG (MozJPEG q=92, < 1.8 MB)
# - Device target: /sdcard/Android/data/com.appletv.launcher/files/wallpapers/
# ==============================================================================

set -euo pipefail

IMAGE_INPUT="${1:-}"
TV_DEVICE="${2:-}"

if [ -z "$IMAGE_INPUT" ] || [ ! -f "$IMAGE_INPUT" ]; then
    echo "❌ Usage: $0 <image_file> [TV_IP:PORT]"
    echo "   Example: $0 wallpapers/Cartoon_Cat.png 192.168.1.10:5555"
    exit 1
fi

# Detect device if not supplied
if [ -z "$TV_DEVICE" ]; then
    CONNECTED=$(adb devices | grep -E "device$" | awk '{print $1}' | head -n 1 || true)
    if [ -n "$CONNECTED" ]; then
        TV_DEVICE="$CONNECTED"
    else
        TV_DEVICE="192.168.1.10:5555"
    fi
fi

# Clean any WSL Zone.Identifier metadata
rm -f "${IMAGE_INPUT}:Zone.Identifier" 2>/dev/null || true

RAW_NAME=$(basename "$IMAGE_INPUT")
NAME_NO_EXT="${RAW_NAME%.*}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$SCRIPT_DIR")"
WALLPAPERS_DIR="$REPO_DIR/wallpapers"
mkdir -p "$WALLPAPERS_DIR"

OUTPUT_JPG="$WALLPAPERS_DIR/${NAME_NO_EXT}.jpg"
TMP_PROCESSED="/tmp/${NAME_NO_EXT}_$$.jpg"
CACHE_DIR="$HOME/.cache/apple-tv-launcher"

echo "🎨 Processing '$IMAGE_INPUT' -> 3840x2160 UHD JPEG..."

# Use Node.js + sharp for optimal Lanczos3 scaling and MozJPEG compression
if command -v node >/dev/null 2>&1; then
    mkdir -p "$CACHE_DIR"
    if [ ! -d "$CACHE_DIR/node_modules/sharp" ]; then
        echo "📦 Initializing sharp image processor in cache ($CACHE_DIR)..."
        (cd "$CACHE_DIR" && npm init -y >/dev/null 2>&1 && npm install --silent sharp >/dev/null 2>&1)
    fi

    CACHE_DIR="$CACHE_DIR" node -e '
        const sharp = require(process.env.CACHE_DIR + "/node_modules/sharp");
        sharp(process.argv[1])
            .resize(3840, 2160, { fit: "cover", position: "center" })
            .jpeg({ quality: 92, mozjpeg: true })
            .toFile(process.argv[2])
            .then(info => {
                console.log("   ✅ Converted: 3840x2160 (" + Math.round(info.size / 1024) + " KB)");
            })
            .catch(err => {
                console.error("   ❌ Sharp error:", err.message);
                process.exit(1);
            });
    ' "$IMAGE_INPUT" "$TMP_PROCESSED"
    mv -f "$TMP_PROCESSED" "$OUTPUT_JPG"

elif command -v magick >/dev/null 2>&1 || command -v convert >/dev/null 2>&1; then
    CMD=$(command -v magick || command -v convert)
    $CMD "$IMAGE_INPUT" -resize "3840x2160^" -gravity center -extent 3840x2160 -quality 92 "$OUTPUT_JPG"
    echo "   ✅ Converted via ImageMagick"
elif command -v ffmpeg >/dev/null 2>&1; then
    ffmpeg -y -i "$IMAGE_INPUT" -vf "scale=3840:2160:force_original_aspect_ratio=increase,crop=3840:2160" -q:v 2 "$OUTPUT_JPG" >/dev/null 2>&1
    echo "   ✅ Converted via ffmpeg"
else
    echo "❌ Error: No image processor found (node, imagemagick, or ffmpeg)."
    exit 1
fi

# If original was a non-jpg in wallpapers/, remove it to prevent duplicate UI cards
if [ "$IMAGE_INPUT" != "$OUTPUT_JPG" ] && [[ "$IMAGE_INPUT" == *"/wallpapers/"* ]]; then
    rm -f "$IMAGE_INPUT"
fi

echo "🚀 Syncing to TV ($TV_DEVICE)..."
TARGET_DIR="/sdcard/Android/data/com.appletv.launcher/files/wallpapers"
TMP_REMOTE="/data/local/tmp/${NAME_NO_EXT}.jpg"

# Android /sdcard FUSE safe 2-step push
adb -s "$TV_DEVICE" push "$OUTPUT_JPG" "$TMP_REMOTE" >/dev/null
adb -s "$TV_DEVICE" shell "cp '$TMP_REMOTE' '$TARGET_DIR/${NAME_NO_EXT}.jpg' && rm -f '$TMP_REMOTE'"

LOCAL_MD5=$(md5sum "$OUTPUT_JPG" | awk '{print $1}')
REMOTE_MD5=$(adb -s "$TV_DEVICE" shell "md5sum '$TARGET_DIR/${NAME_NO_EXT}.jpg'" | awk '{print $1}')

if [ "$LOCAL_MD5" = "$REMOTE_MD5" ]; then
    echo "✨ Successfully synced '${NAME_NO_EXT}.jpg' to TV!"
    echo "   MD5: $LOCAL_MD5"
    echo "   Target: $TARGET_DIR/${NAME_NO_EXT}.jpg"
else
    echo "⚠️ Checksum mismatch: Local=$LOCAL_MD5 vs Remote=$REMOTE_MD5"
    exit 1
fi
