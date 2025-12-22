#!/bin/bash
set -e

echo "=== Test: scribeLicensesCheck should pass with valid definitions ==="
./gradlew :example:scribeLicensesDebugCheck --no-configuration-cache
echo "SUCCESS: scribeLicensesCheck passed with valid definitions"
