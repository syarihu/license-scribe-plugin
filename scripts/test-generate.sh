#!/bin/bash
set -e

echo "=== Test: generateLicenseCode should generate valid Kotlin code ==="

# Run generate
./gradlew :example:generateDebugLicenseCode --no-configuration-cache

GENERATED_DIR="example/build/generated/source/licensescribe"

# Find generated files (they are in package subdirectory)
LICENSES_FILE=$(find "$GENERATED_DIR" -name "AppLicenses.kt" -type f 2>/dev/null | head -1)

# Verify files were created
if [[ -z "$LICENSES_FILE" || ! -f "$LICENSES_FILE" ]]; then
  echo "ERROR: AppLicenses.kt not found"
  exit 1
fi
echo "SUCCESS: AppLicenses.kt exists at $LICENSES_FILE"

# Verify AppLicenses.kt contains expected content
# - implements LicenseProvider interface
# - has override val all property
if grep -q "object AppLicenses" "$LICENSES_FILE" && \
   grep -q "LicenseProvider" "$LICENSES_FILE" && \
   grep -q "override val all" "$LICENSES_FILE"; then
  echo "SUCCESS: AppLicenses.kt contains expected code structure"
else
  echo "ERROR: AppLicenses.kt missing expected content"
  cat "$LICENSES_FILE"
  exit 1
fi
