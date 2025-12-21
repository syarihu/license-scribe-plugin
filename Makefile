.PHONY: publish build clean help

# Publish to Maven Local (runtime first, then core, then gradle-plugin)
publish:
	./gradlew :license-scribe-runtime:publishToMavenLocal :license-scribe-core:publishToMavenLocal :license-scribe-gradle-plugin:publishToMavenLocal --no-configuration-cache

# Build all modules (requires publish first)
build:
	./gradlew build --no-configuration-cache

# Publish and build
all: publish build

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
	@echo "  make publish  - Publish core and gradle-plugin to Maven Local"
	@echo "  make build    - Build all modules"
	@echo "  make all      - Publish and build"
	@echo "  make clean    - Clean build artifacts"
	@echo "  make format   - Apply spotless formatting"
	@echo "  make check    - Check formatting"
