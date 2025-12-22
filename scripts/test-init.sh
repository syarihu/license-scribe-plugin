#!/bin/bash
set -e

echo "=== Test: scribeLicensesInit should create license files ==="

# Backup and remove existing files
cp -r example/licenses example/licenses.bak

cleanup() {
  rm -rf example/licenses
  mv example/licenses.bak example/licenses
}
trap cleanup EXIT

rm -f example/licenses/scribe-records.yml
rm -f example/licenses/scribe-catalog.yml
rm -f example/licenses/.scribeignore

# Run init
./gradlew :example:scribeLicensesDebugInit --no-configuration-cache

# Verify files were created
if [[ -f example/licenses/scribe-records.yml && \
      -f example/licenses/scribe-catalog.yml && \
      -f example/licenses/.scribeignore ]]; then
  echo "SUCCESS: scribeLicensesInit created all required files"
else
  echo "ERROR: scribeLicensesInit did not create all files"
  ls -la example/licenses/
  exit 1
fi

# Verify scribe-records.yml has content
if [[ -s example/licenses/scribe-records.yml ]]; then
  echo "SUCCESS: scribe-records.yml has content"
else
  echo "ERROR: scribe-records.yml is empty"
  exit 1
fi
