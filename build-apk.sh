#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/android"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle is not installed. Install Android Studio (which includes the Android SDK), then install Gradle 8.9+ or generate a Gradle wrapper from Android Studio." >&2
  exit 1
fi
gradle --no-daemon assembleDebug
mkdir -p ../dist
cp app/build/outputs/apk/debug/app-debug.apk ../dist/ChatDemo-debug.apk
echo "APK created: $(cd .. && pwd)/dist/ChatDemo-debug.apk"
