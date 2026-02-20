#!/bin/sh
set -eu

BASE_DIR="$(cd "$(dirname "$0")"; pwd -P)"
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPS="$WRAPPER_DIR/maven-wrapper.properties"

if [ ! -f "$WRAPPER_PROPS" ]; then
  echo "Missing $WRAPPER_PROPS" >&2
  exit 1
fi

if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL="$(sed -n 's/^wrapperUrl=//p' "$WRAPPER_PROPS" | tr -d '\r')"
  if [ -z "${WRAPPER_URL:-}" ]; then
    echo "wrapperUrl is not configured in $WRAPPER_PROPS" >&2
    exit 1
  fi
  mkdir -p "$WRAPPER_DIR"
  echo "Downloading Maven wrapper jar..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR"
  else
    echo "curl or wget is required to download maven-wrapper.jar" >&2
    exit 1
  fi
fi

exec java -Dmaven.multiModuleProjectDirectory="$BASE_DIR" -cp "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
