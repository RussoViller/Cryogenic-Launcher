#!/usr/bin/env bash
# Builds a Launcher Mod (.cmod).
#
# Usage: ./build.sh <mod-folder> [output.cmod]
#
# The mod folder must contain:
#   cmod.json      mod info (id, name, version, author, description, main, apiVersion)
#   src/           Java source code
#   icon.png       (optional) mod icon
#   assets/        (optional) extra files, copied as they are
#
# Requires: JDK 17+ (javac, jar) and the Android SDK (android.jar + d8).
# ANDROID_HOME (or ANDROID_SDK_ROOT) must point to the SDK.
set -euo pipefail

SDK_DIR="$(cd "$(dirname "$0")" && pwd)"
MOD_DIR="$(cd "${1:?usage: ./build.sh <mod-folder> [output.cmod]}" && pwd)"
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$ANDROID_SDK" ] || { echo "Set ANDROID_HOME"; exit 1; }

PLATFORM="$(ls -d "$ANDROID_SDK"/platforms/android-* | sort -V | tail -1)"
BUILD_TOOLS="$(ls -d "$ANDROID_SDK"/build-tools/* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
D8="$BUILD_TOOLS/d8"

API_SRC="$SDK_DIR/../CryogenicLauncher/src/main/java/com/russoviller/cryogenic/cmod/api"
[ -d "$API_SRC" ] || { echo "API not found: run build.sh from inside the Cryogenic Launcher repository"; exit 1; }
[ -f "$MOD_DIR/cmod.json" ] || { echo "Missing cmod.json in $MOD_DIR"; exit 1; }

ID="$(sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$MOD_DIR/cmod.json" | head -1)"
OUT="${2:-$SDK_DIR/out/$ID.cmod}"
mkdir -p "$(dirname "$OUT")"
OUT="$(cd "$(dirname "$OUT")" && pwd)/$(basename "$OUT")"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# 1) API (only to compile against: the launcher already has these classes, they do NOT go in the .cmod)
mkdir -p "$WORK/api"
javac -source 8 -target 8 -nowarn -Xlint:-options -cp "$ANDROID_JAR" -d "$WORK/api" $(find "$API_SRC" -name '*.java')
(cd "$WORK/api" && jar cf "$WORK/cryomod-api.jar" .)

# 2) Mod code
mkdir -p "$WORK/classes"
find "$MOD_DIR/src" -name '*.java' > "$WORK/sources.txt"
javac -source 8 -target 8 -nowarn -Xlint:-options -encoding UTF-8 \
    -cp "$ANDROID_JAR:$WORK/cryomod-api.jar" -d "$WORK/classes" @"$WORK/sources.txt"
(cd "$WORK/classes" && jar cf "$WORK/mod.jar" .)

# 3) Dex
mkdir -p "$WORK/dex" "$WORK/pkg"
"$D8" --release --min-api 26 --lib "$ANDROID_JAR" --classpath "$WORK/cryomod-api.jar" \
    --output "$WORK/dex" "$WORK/mod.jar"

# 4) Package
cp "$MOD_DIR/cmod.json" "$WORK/pkg/"
cp "$WORK"/dex/classes*.dex "$WORK/pkg/"
[ -f "$MOD_DIR/icon.png" ] && cp "$MOD_DIR/icon.png" "$WORK/pkg/"
[ -d "$MOD_DIR/assets" ] && cp -r "$MOD_DIR/assets" "$WORK/pkg/"
rm -f "$OUT"
(cd "$WORK/pkg" && jar cfM "$OUT" .)
echo "OK: $OUT"
