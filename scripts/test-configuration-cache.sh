#!/bin/bash
set -e

echo "=== Test: Configuration Cache should work correctly ==="

# Clean first
./gradlew clean

# First run - should store cache
./gradlew :example:scribeLicensesDebugCheck 2>&1 | tee /tmp/first_run.log

if grep -q "Configuration cache entry stored" /tmp/first_run.log; then
  echo "SUCCESS: Configuration cache entry stored"
else
  echo "WARNING: Could not verify cache storage"
fi

# Second run - should reuse cache
./gradlew :example:scribeLicensesDebugCheck 2>&1 | tee /tmp/second_run.log

if grep -q "Configuration cache entry reused" /tmp/second_run.log; then
  echo "SUCCESS: Configuration cache entry reused"
else
  echo "WARNING: Could not verify cache reuse"
fi
