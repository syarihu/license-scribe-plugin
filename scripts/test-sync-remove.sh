#!/bin/bash
set -e

echo "=== Test: scribeLicensesSync should remove deleted dependency ==="

# Backup original files
cp example/licenses/scribe-records.yml example/licenses/scribe-records.yml.bak
cp example/build.gradle.kts example/build.gradle.kts.bak

cleanup() {
  mv example/licenses/scribe-records.yml.bak example/licenses/scribe-records.yml
  mv example/build.gradle.kts.bak example/build.gradle.kts
}
trap cleanup EXIT

# Remove gson dependency (not a transitive dependency of example-library's Retrofit)
sed -i '/com.google.code.gson:gson/d' example/build.gradle.kts

# Run sync
./gradlew :example:scribeLicensesDebugSync --no-configuration-cache

# Verify gson was removed from definitions
if grep -q "com.google.code.gson" example/licenses/scribe-records.yml; then
  echo "ERROR: scribeLicensesSync did not remove deleted dependency"
  exit 1
fi

echo "SUCCESS: scribeLicensesSync correctly removed deleted dependency"
