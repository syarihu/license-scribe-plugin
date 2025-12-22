#!/bin/bash
set -e

echo "=== Test: scribeLicensesCheck should fail when definition is missing ==="

# Backup original file
cp example/licenses/debug/scribe-licenses.yml example/licenses/debug/scribe-licenses.yml.bak

cleanup() {
  mv example/licenses/debug/scribe-licenses.yml.bak example/licenses/debug/scribe-licenses.yml
}
trap cleanup EXIT

# Remove com.squareup.retrofit2 group (5 lines) from the YAML
# This will cause scribeLicensesCheck to fail because retrofit is a dependency
awk '/com.squareup.retrofit2:/{skip=5} skip>0{skip--;next} 1' \
  example/licenses/debug/scribe-licenses.yml > example/licenses/debug/scribe-licenses.yml.tmp \
  && mv example/licenses/debug/scribe-licenses.yml.tmp example/licenses/debug/scribe-licenses.yml

# scribeLicensesCheck should fail
if ./gradlew :example:scribeLicensesDebugCheck --no-configuration-cache 2>&1; then
  echo "ERROR: scribeLicensesCheck should have failed but succeeded"
  exit 1
fi

echo "SUCCESS: scribeLicensesCheck correctly detected missing definition"
