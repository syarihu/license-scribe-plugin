#!/bin/bash
set -e

echo "=== Test: syncLicenses should remove deleted dependency ==="

# Backup original files
cp example/licenses/artifact-definitions.yml example/licenses/artifact-definitions.yml.bak
cp example/build.gradle.kts example/build.gradle.kts.bak

cleanup() {
  mv example/licenses/artifact-definitions.yml.bak example/licenses/artifact-definitions.yml
  mv example/build.gradle.kts.bak example/build.gradle.kts
}
trap cleanup EXIT

# Remove gson dependency (not a transitive dependency of example-library's Retrofit)
sed -i '/com.google.code.gson:gson/d' example/build.gradle.kts

# Run sync
./gradlew :example:syncDebugLicenses --no-configuration-cache

# Verify gson was removed from definitions
if grep -q "com.google.code.gson" example/licenses/artifact-definitions.yml; then
  echo "ERROR: syncLicenses did not remove deleted dependency"
  exit 1
fi

echo "SUCCESS: syncLicenses correctly removed deleted dependency"
