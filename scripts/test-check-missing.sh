#!/bin/bash
set -e

echo "=== Test: checkLicenses should fail when definition is missing ==="

# Backup original file
cp example/licenses/artifact-definitions.yml example/licenses/artifact-definitions.yml.bak

cleanup() {
  mv example/licenses/artifact-definitions.yml.bak example/licenses/artifact-definitions.yml
}
trap cleanup EXIT

# Remove one artifact definition
sed -i '/- name: okhttp/,/license:/d' example/licenses/artifact-definitions.yml

# checkLicenses should fail
if ./gradlew :example:checkDebugLicenses --no-configuration-cache 2>&1; then
  echo "ERROR: checkLicenses should have failed but succeeded"
  exit 1
fi

echo "SUCCESS: checkLicenses correctly detected missing definition"
