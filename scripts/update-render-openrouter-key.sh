#!/usr/bin/env bash
# Update OPENROUTER_API_KEY on Render to zzzzzzzzzzz (or first argument).
# Requires: RENDER_API_KEY in environment (create at https://dashboard.render.com/u/settings#api-keys)
set -e
KEY="${1:-zzzzzzzzzz}"
if [ -z "$RENDER_API_KEY" ]; then
  echo "Error: Set RENDER_API_KEY (e.g. from https://dashboard.render.com/u/settings#api-keys)" >&2
  exit 1
fi
BASE="https://api.render.com/v1"
SERVICE_NAME="paradigm-sys-1"

echo "Resolving service ID for $SERVICE_NAME..."
SERVICE_ID=$(curl -s -S -X GET "$BASE/services?limit=100" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer $RENDER_API_KEY" | jq -r --arg n "$SERVICE_NAME" '.[] | select(.service .name == $n) | .service .id')
if [ -z "$SERVICE_ID" ] || [ "$SERVICE_ID" = "null" ]; then
  echo "Error: Service '$SERVICE_NAME' not found." >&2
  exit 1
fi

echo "Fetching current env vars..."
CURRENT=$(curl -s -S -X GET "$BASE/services/$SERVICE_ID/env-vars" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer $RENDER_API_KEY")

# Build new env list: from current, set OPENROUTER_API_KEY to KEY.
# Render GET returns array of { "envVar": { "key", "value" } }; PUT expects array of { "key", "value" }.
BODY=$(echo "$CURRENT" | jq --arg k "OPENROUTER_API_KEY" --arg v "$KEY" '
  map(.envVar // .) | map(if .key == $k then .key = $k | .value = $v else . end) |
  if any(.key == $k) then . else . + [{ key: $k, value: $v }] end |
  map({ key: .key, value: .value })
')

echo "Updating OPENROUTER_API_KEY to '$KEY'..."
curl -s -S -X PUT "$BASE/services/$SERVICE_ID/env-vars" \
  -H "Accept: application/json" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $RENDER_API_KEY" \
  -d "$BODY" | jq -e . >/dev/null || true
echo "Done. Redeploy the service in Render Dashboard if you want changes to take effect immediately."
