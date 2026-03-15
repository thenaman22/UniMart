#!/usr/bin/env sh

set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
PROPS_FILE="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.properties"
DIST_URL="$(grep '^distributionUrl=' "$PROPS_FILE" | cut -d= -f2- | sed 's/\\//g')"

if [ -z "$DIST_URL" ]; then
  echo "Could not read distributionUrl from $PROPS_FILE"
  exit 1
fi

DIST_FILE="$(basename "$DIST_URL")"
DIST_NAME="${DIST_FILE%-bin.zip}"
DIST_ROOT="$SCRIPT_DIR/.gradle-wrapper/dists"
DIST_DIR="$DIST_ROOT/$DIST_NAME"
GRADLE_CMD="$DIST_DIR/bin/gradle"

if [ ! -x "$GRADLE_CMD" ]; then
  mkdir -p "$DIST_ROOT"
  ZIP_PATH="$DIST_ROOT/$DIST_FILE"
  echo "Downloading $DIST_URL"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$ZIP_PATH"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP_PATH" "$DIST_URL"
  else
    echo "curl or wget is required to download Gradle."
    exit 1
  fi
  unzip -q -o "$ZIP_PATH" -d "$DIST_ROOT"
  chmod +x "$GRADLE_CMD"
fi

exec "$GRADLE_CMD" "$@"
