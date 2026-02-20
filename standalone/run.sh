#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(cd "$(dirname "$0")"; pwd -P)"
OUT_DIR="$SCRIPT_DIR/out"
ENV_FILE="$SCRIPT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
fi

mkdir -p "$OUT_DIR"
javac -encoding UTF-8 -d "$OUT_DIR" "$SCRIPT_DIR/MappingServer.java"
exec java -cp "$OUT_DIR" MappingServer
