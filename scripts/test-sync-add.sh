#!/bin/bash
set -e

echo "=== Test: scribeLicensesSync should add new dependency ==="

# Backup original files
cp example/licenses/scribe-licenses.yml example/licenses/scribe-licenses.yml.bak
cp example/build.gradle.kts example/build.gradle.kts.bak

cleanup() {
  mv example/licenses/scribe-licenses.yml.bak example/licenses/scribe-licenses.yml
  mv example/build.gradle.kts.bak example/build.gradle.kts
}
trap cleanup EXIT

# Add a new dependency
sed -i '' 's/implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")/implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")\n  implementation("com.google.code.gson:gson:2.11.0")/' example/build.gradle.kts

# Run sync
./gradlew :example:scribeLicensesDebugSync --no-configuration-cache

# Verify gson was added to definitions
if grep -q "gson" example/licenses/scribe-licenses.yml; then
  echo "SUCCESS: scribeLicensesSync correctly added new dependency"
else
  echo "ERROR: scribeLicensesSync did not add new dependency"
  exit 1
fi
