#!/bin/bash
set -e

echo "=== Test: initLicenses should create license files ==="

# Backup and remove existing files
cp -r example/licenses example/licenses.bak

cleanup() {
  rm -rf example/licenses
  mv example/licenses.bak example/licenses
}
trap cleanup EXIT

rm -f example/licenses/artifact-definitions.yml
rm -f example/licenses/license-catalog.yml
rm -f example/licenses/.artifactignore

# Run init
./gradlew :example:initDebugLicenses --no-configuration-cache

# Verify files were created
if [[ -f example/licenses/artifact-definitions.yml && \
      -f example/licenses/license-catalog.yml && \
      -f example/licenses/.artifactignore ]]; then
  echo "SUCCESS: initLicenses created all required files"
else
  echo "ERROR: initLicenses did not create all files"
  ls -la example/licenses/
  exit 1
fi

# Verify artifact-definitions.yml has content
if [[ -s example/licenses/artifact-definitions.yml ]]; then
  echo "SUCCESS: artifact-definitions.yml has content"
else
  echo "ERROR: artifact-definitions.yml is empty"
  exit 1
fi
