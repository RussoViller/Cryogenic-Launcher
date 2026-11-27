#!/usr/bin/env bash
# Compila um Mod do Launcher (.cmod).
#
# Uso: ./build.sh <pasta-do-mod> [saida.cmod]
#
# A pasta do mod precisa ter:
#   cmod.json      dados do mod (id, name, version, author, description, main, apiVersion)
#   src/           código Java
#   icon.png       (opcional) ícone do mod
#   assets/        (opcional) arquivos extras, copiados como estão
#
# Precisa de: JDK 17+ (javac, jar) e Android SDK (android.jar + d8).
# ANDROID_HOME (ou ANDROID_SDK_ROOT) deve apontar para o SDK.
set -euo pipefail

SDK_DIR="$(cd "$(dirname "$0")" && pwd)"
MOD_DIR="$(cd "${1:?uso: ./build.sh <pasta-do-mod> [saida.cmod]}" && pwd)"
ANDROID_SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$ANDROID_SDK" ] || { echo "Defina ANDROID_HOME"; exit 1; }

PLATFORM="$(ls -d "$ANDROID_SDK"/platforms/android-* | sort -V | tail -1)"
BUILD_TOOLS="$(ls -d "$ANDROID_SDK"/build-tools/* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
D8="$BUILD_TOOLS/d8"

API_SRC="$SDK_DIR/../CryogenicLauncher/src/main/java/com/russoviller/cryogenic/cmod/api"
[ -d "$API_SRC" ] || { echo "API não encontrada: rode o build.sh de dentro do repositório do Cryogenic Launcher"; exit 1; }
[ -f "$MOD_DIR/cmod.json" ] || { echo "Falta cmod.json em $MOD_DIR"; exit 1; }

ID="$(sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "$MOD_DIR/cmod.json" | head -1)"
OUT="${2:-$SDK_DIR/out/$ID.cmod}"
mkdir -p "$(dirname "$OUT")"
OUT="$(cd "$(dirname "$OUT")" && pwd)/$(basename "$OUT")"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# 1) API (só para compilar: o launcher já tem essas classes, elas NÃO vão no .cmod)
mkdir -p "$WORK/api"
javac -source 8 -target 8 -nowarn -Xlint:-options -cp "$ANDROID_JAR" -d "$WORK/api" $(find "$API_SRC" -name '*.java')
(cd "$WORK/api" && jar cf "$WORK/cryomod-api.jar" .)

# 2) Código do mod
mkdir -p "$WORK/classes"
find "$MOD_DIR/src" -name '*.java' > "$WORK/sources.txt"
javac -source 8 -target 8 -nowarn -Xlint:-options -encoding UTF-8 \
    -cp "$ANDROID_JAR:$WORK/cryomod-api.jar" -d "$WORK/classes" @"$WORK/sources.txt"
(cd "$WORK/classes" && jar cf "$WORK/mod.jar" .)

# 3) Dex
mkdir -p "$WORK/dex" "$WORK/pkg"
"$D8" --release --min-api 26 --lib "$ANDROID_JAR" --classpath "$WORK/cryomod-api.jar" \
    --output "$WORK/dex" "$WORK/mod.jar"

# 4) Pacote
cp "$MOD_DIR/cmod.json" "$WORK/pkg/"
cp "$WORK"/dex/classes*.dex "$WORK/pkg/"
[ -f "$MOD_DIR/icon.png" ] && cp "$MOD_DIR/icon.png" "$WORK/pkg/"
[ -d "$MOD_DIR/assets" ] && cp -r "$MOD_DIR/assets" "$WORK/pkg/"
rm -f "$OUT"
(cd "$WORK/pkg" && jar cfM "$OUT" .)
echo "OK: $OUT"
