# License Scribe Plugin

A Gradle plugin for managing and generating license information for Android/Kotlin project dependencies.

## Features

- Automatically detects project dependencies and their licenses
- Generates Kotlin code for easy access to license information in your app
- Supports YAML-based configuration for artifact definitions and license catalog
- Works with Android (application/library) and pure Kotlin/JVM projects

## Project Structure

```
license-scribe-plugin/
├── license-scribe-core/          # Core library (parser, model, code generator)
├── license-scribe-gradle-plugin/ # Gradle plugin
├── example/                      # Example Android app
├── Makefile                      # Development commands
└── build.gradle.kts              # Root build configuration
```

## Installation

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        mavenLocal()  // For local development
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}
```

### build.gradle.kts

```kotlin
plugins {
    id("net.syarihu.license-scribe") version "0.1.0-SNAPSHOT"
}

licenseScribe {
    // Base directory for license files (default: project directory)
    // Recommended: use a dedicated directory to keep things organized
    baseDir.set(layout.projectDirectory.dir("licenses"))

    // Package name for generated code (required)
    generatedPackageName.set("com.example.app")

    // Class name for generated code (default: "Licenses")
    generatedClassName.set("AppLicenses")

    // File names (optional, with defaults)
    // artifactDefinitionsFile.set("artifact-definitions.yml")
    // licenseCatalogFile.set("license-catalog.yml")
    // artifactIgnoreFile.set(".artifactignore")
}
```

## Usage

### 1. Initialize License Files

Run the init task to create the initial license management files:

```bash
# For Android projects (with variants)
./gradlew initDebugLicenses

# For non-Android projects
./gradlew initLicenses
```

This creates:
- `artifact-definitions.yml` - Defines artifacts and their license information
- `license-catalog.yml` - Catalog of license types
- `.artifactignore` - Patterns for artifacts to ignore

### 2. Review and Edit Definitions

Edit `artifact-definitions.yml` to add missing information:

```yaml
implementation:
  com.example:
  - name: library-name
    url: https://github.com/example/library
    copyrightHolder: Example Inc.
    license: apache-2.0
    version: 1.0.0
```

Edit `license-catalog.yml` to define license types:

```yaml
apache-2.0:
  name: The Apache License, Version 2.0
  url: http://www.apache.org/licenses/LICENSE-2.0.txt
mit:
  name: MIT License
  url: https://opensource.org/licenses/MIT
```

### 3. Check Licenses (Optional)

Check for missing attributes and validate that all dependencies are covered:

```bash
./gradlew checkDebugLicenses
```

### 4. Sync Licenses (Optional)

When dependencies change, sync definitions with current dependencies:

```bash
./gradlew syncDebugLicenses
```

### 5. Generate Code

Generate Kotlin code (automatically runs during build):

```bash
./gradlew generateDebugLicenseCode
```

### 6. Use Generated Code

Access license information in your app:

```kotlin
import com.example.app.AppLicenses
import com.example.app.LicenseInfo

// Get all licenses
val licenses: List<LicenseInfo> = AppLicenses.all

// Display in UI
licenses.forEach { license ->
    println("${license.artifactName} - ${license.licenseName}")
    println("  ID: ${license.artifactId}")
    println("  URL: ${license.artifactUrl}")
    println("  Copyright: ${license.copyrightHolder}")
    println("  License URL: ${license.licenseUrl}")
}
```

## Available Tasks

For Android projects, tasks are created per variant (e.g., `debug`, `release`):

| Task | Description |
|------|-------------|
| `init{Variant}Licenses` | Initialize license management files |
| `check{Variant}Licenses` | Check definitions for missing attributes and validate against dependencies |
| `sync{Variant}Licenses` | Sync definitions with current dependencies |
| `generate{Variant}LicenseCode` | Generate Kotlin code for licenses |

## Development

### Prerequisites

- JDK 17+
- Android SDK (for example project)

### Build Commands

```bash
# Publish to Maven Local (required before building example)
make publish

# Build all modules
make build

# Publish and build
make all

# Clean build artifacts
make clean

# Apply code formatting
make format

# Check formatting
make check
```

### Manual Gradle Commands

```bash
# Publish core module
./gradlew :license-scribe-core:publishToMavenLocal

# Publish plugin
./gradlew :license-scribe-gradle-plugin:publishToMavenLocal

# Build example
./gradlew :example:assembleDebug
```

### Configuration Cache

This plugin is compatible with Gradle's Configuration Cache. The Configuration Cache is enabled by default in `gradle.properties`:

```properties
org.gradle.configuration-cache=true
```

## Example Project

The `example/` directory contains a sample Android app demonstrating the plugin usage:

- Shows how to configure the plugin
- Displays a license list using Jetpack Compose
- Demonstrates the generated `AppLicenses` and `LicenseInfo` classes

To run the example:

```bash
# First, publish the plugin to Maven Local
make publish

# Then build the example
./gradlew :example:assembleDebug
```

## Generated Code Structure

The plugin generates two classes:

### LicenseInfo

```kotlin
data class LicenseInfo(
    val artifactId: String,      // e.g., "com.example:library:1.0.0"
    val artifactName: String,    // e.g., "library"
    val artifactUrl: String?,    // Project URL
    val copyrightHolder: String?, // e.g., "Example Inc."
    val licenseName: String,     // e.g., "The Apache License, Version 2.0"
    val licenseUrl: String?,     // License URL
)
```

### Licenses (or custom class name)

```kotlin
object AppLicenses {
    val all: List<LicenseInfo> = listOf(
        // All license entries
    )
}
```

## License

```
Copyright 2025 syarihu

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
