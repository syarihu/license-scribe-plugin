#!/bin/bash
set -e

echo "=== Test: checkLicenses should pass with valid definitions ==="
./gradlew :example:checkDebugLicenses --no-configuration-cache
echo "SUCCESS: checkLicenses passed with valid definitions"
