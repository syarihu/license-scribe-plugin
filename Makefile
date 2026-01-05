.PHONY: publish build clean test help

# Publish to Maven Local (runtime first, then core, then plugins)
publish:
	./gradlew :license-scribe-runtime:publishToMavenLocal :license-scribe-core:publishToMavenLocal :license-scribe-gradle-plugin:publishToMavenLocal :license-scribe-hilt-plugin:publishToMavenLocal :license-scribe-screen-plugin:publishToMavenLocal --no-configuration-cache

# Build all modules (requires publish first)
build:
	./gradlew build --no-configuration-cache

# Publish and build
all: publish build

# Run all tests
test:
	./scripts/test-init.sh
	./scripts/test-check-valid.sh
	./scripts/test-check-missing.sh
	./scripts/test-sync-add.sh
	./scripts/test-sync-remove.sh
	./scripts/test-generate.sh
	./scripts/test-generate-hilt.sh
	./scripts/test-configuration-cache.sh

# Clean build artifacts
clean:
	./gradlew clean

# Run spotless
format:
	./gradlew spotlessApply

# Check formatting
check:
	./gradlew spotlessCheck

help:
	@echo "Available targets:"
	@echo "  make publish  - Publish all modules to Maven Local"
	@echo "  make build    - Build all modules"
	@echo "  make all      - Publish and build"
	@echo "  make test     - Run all integration tests"
	@echo "  make clean    - Clean build artifacts"
	@echo "  make format   - Apply spotless formatting"
	@echo "  make check    - Check formatting"
