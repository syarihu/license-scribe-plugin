#!/bin/bash
set -e

echo "=== Test: scribeLicensesGenerateHiltModule should generate valid Hilt module ==="

# Run generate
./gradlew :example-hilt:scribeLicensesDebugGenerateHiltModule --no-configuration-cache

GENERATED_DIR="example-hilt/build/generated/source/licensescribe"

# Find generated Hilt module file (in package subdirectory)
HILT_MODULE_FILE=$(find "$GENERATED_DIR" -name "LicenseScribeHiltModule.kt" -type f 2>/dev/null | head -1)

# Verify file was created
if [[ -z "$HILT_MODULE_FILE" || ! -f "$HILT_MODULE_FILE" ]]; then
  echo "ERROR: LicenseScribeHiltModule.kt not found"
  exit 1
fi
echo "SUCCESS: LicenseScribeHiltModule.kt exists at $HILT_MODULE_FILE"

# Verify LicenseScribeHiltModule.kt contains expected Hilt annotations
if grep -q "@Module" "$HILT_MODULE_FILE" && \
   grep -q "@InstallIn" "$HILT_MODULE_FILE" && \
   grep -q "SingletonComponent" "$HILT_MODULE_FILE" && \
   grep -q "@Provides" "$HILT_MODULE_FILE" && \
   grep -q "LicenseProvider" "$HILT_MODULE_FILE"; then
  echo "SUCCESS: LicenseScribeHiltModule.kt contains expected Hilt module structure"
else
  echo "ERROR: LicenseScribeHiltModule.kt missing expected Hilt annotations"
  cat "$HILT_MODULE_FILE"
  exit 1
fi
