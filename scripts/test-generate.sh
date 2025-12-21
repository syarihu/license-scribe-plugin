#!/bin/bash
set -e

echo "=== Test: generateLicenseCode should generate valid Kotlin code ==="

# Run generate
./gradlew :example:generateDebugLicenseCode --no-configuration-cache

GENERATED_DIR="example/build/generated/source/licenseScribe"

# Find generated files (they are in package subdirectory)
LICENSES_FILE=$(find "$GENERATED_DIR" -name "AppLicenses.kt" -type f 2>/dev/null | head -1)
LICENSE_INFO_FILE=$(find "$GENERATED_DIR" -name "LicenseInfo.kt" -type f 2>/dev/null | head -1)

# Verify files were created
if [[ -z "$LICENSES_FILE" || ! -f "$LICENSES_FILE" ]]; then
  echo "ERROR: AppLicenses.kt not found"
  exit 1
fi
echo "SUCCESS: AppLicenses.kt exists at $LICENSES_FILE"

if [[ -z "$LICENSE_INFO_FILE" || ! -f "$LICENSE_INFO_FILE" ]]; then
  echo "ERROR: LicenseInfo.kt not found"
  exit 1
fi
echo "SUCCESS: LicenseInfo.kt exists at $LICENSE_INFO_FILE"

# Verify AppLicenses.kt contains expected content
if grep -q "object AppLicenses" "$LICENSES_FILE" && \
   grep -q "val all: List<LicenseInfo>" "$LICENSES_FILE"; then
  echo "SUCCESS: AppLicenses.kt contains expected code structure"
else
  echo "ERROR: AppLicenses.kt missing expected content"
  cat "$LICENSES_FILE"
  exit 1
fi

# Verify LicenseInfo.kt contains expected content
if grep -q "data class LicenseInfo" "$LICENSE_INFO_FILE"; then
  echo "SUCCESS: LicenseInfo.kt contains expected code structure"
else
  echo "ERROR: LicenseInfo.kt missing expected content"
  cat "$LICENSE_INFO_FILE"
  exit 1
fi
