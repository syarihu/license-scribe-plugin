#!/bin/bash
set -e

echo "=== Test: scribeLicensesCheck should fail when definition is missing ==="

# Backup original file
cp example/licenses/scribe-records.yml example/licenses/scribe-records.yml.bak

cleanup() {
  mv example/licenses/scribe-records.yml.bak example/licenses/scribe-records.yml
}
trap cleanup EXIT

# Remove one artifact definition
sed -i '/- name: okhttp/,/license:/d' example/licenses/scribe-records.yml

# scribeLicensesCheck should fail
if ./gradlew :example:scribeLicensesDebugCheck --no-configuration-cache 2>&1; then
  echo "ERROR: scribeLicensesCheck should have failed but succeeded"
  exit 1
fi

echo "SUCCESS: scribeLicensesCheck correctly detected missing definition"
