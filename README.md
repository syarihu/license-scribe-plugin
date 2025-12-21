# License Scribe Plugin

A Gradle plugin for managing and generating license information for Android/Kotlin project dependencies.

## Features

- Automatically detects project dependencies and their licenses (including transitive dependencies)
- Generates Kotlin code for easy access to license information in your app
- Supports YAML-based configuration for artifact definitions and license catalog
- Works with Android (application/library) and pure Kotlin/JVM projects
- **Multi-module support** with `LicenseProvider` interface for dependency injection
- **Optional Hilt integration** for seamless DI in feature modules

## Project Structure

```
license-scribe-plugin/
├── license-scribe-core/          # Core library (LicenseInfo, LicenseProvider, parser, generator)
├── license-scribe-gradle-plugin/ # Main Gradle plugin
├── license-scribe-hilt-plugin/   # Optional Hilt integration plugin
├── example/                      # Example Android app
├── example-library/              # Example library module (for transitive dependency testing)
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

### build.gradle.kts (app module)

```kotlin
plugins {
    id("net.syarihu.license-scribe") version "0.1.0-SNAPSHOT"
}

dependencies {
    // Required: Core library provides LicenseInfo and LicenseProvider
    implementation("net.syarihu:license-scribe-core:0.1.0-SNAPSHOT")
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
import net.syarihu.licensescribe.LicenseInfo

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

## Multi-Module Support

In multi-module projects, you may want to display licenses in a feature module (e.g., `:feature:settings`) while the plugin is applied to `:app`. The `LicenseProvider` interface enables this pattern.

### Using LicenseProvider Interface

The generated code implements `LicenseProvider` from `license-scribe-core`:

```kotlin
// Generated in :app module
object AppLicenses : LicenseProvider {
    override val all: List<LicenseInfo> = listOf(...)
}
```

### Feature Module Setup

In your feature module, depend only on `license-scribe-core`:

```kotlin
// feature/settings/build.gradle.kts
dependencies {
    implementation("net.syarihu:license-scribe-core:0.1.0-SNAPSHOT")
}
```

Then use `LicenseProvider` via dependency injection:

```kotlin
// In feature module
class LicenseScreen(
    private val licenseProvider: LicenseProvider
) {
    fun showLicenses() {
        licenseProvider.all.forEach { license ->
            println(license.artifactName)
        }
    }
}
```

## Hilt Integration (Optional)

For projects using Hilt, the optional `license-scribe-hilt` plugin generates a Hilt module automatically.

### Setup

```kotlin
// app/build.gradle.kts
plugins {
    id("net.syarihu.license-scribe") version "0.1.0-SNAPSHOT"
    id("net.syarihu.license-scribe-hilt") version "0.1.0-SNAPSHOT"
}
```

### Generated Hilt Module

The plugin generates:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object LicenseScribeHiltModule {
    @Provides
    @Singleton
    fun provideLicenseProvider(): LicenseProvider = AppLicenses
}
```

### Usage in Feature Module

```kotlin
// feature/settings/build.gradle.kts
dependencies {
    implementation("net.syarihu:license-scribe-core:0.1.0-SNAPSHOT")
}

// ViewModel in feature module
@HiltViewModel
class LicenseViewModel @Inject constructor(
    private val licenseProvider: LicenseProvider
) : ViewModel() {
    val licenses: List<LicenseInfo> = licenseProvider.all
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

### Core Library Classes

The `license-scribe-core` library provides:

#### LicenseInfo

```kotlin
package net.syarihu.licensescribe

data class LicenseInfo(
    val artifactId: String,       // e.g., "com.example:library:1.0.0"
    val artifactName: String,     // e.g., "library"
    val artifactUrl: String?,     // Project URL
    val copyrightHolder: String?, // e.g., "Example Inc."
    val licenseName: String,      // e.g., "The Apache License, Version 2.0"
    val licenseUrl: String?,      // License URL
)
```

#### LicenseProvider

```kotlin
package net.syarihu.licensescribe

interface LicenseProvider {
    val all: List<LicenseInfo>
}
```

### Generated Class

The plugin generates one class that implements `LicenseProvider`:

```kotlin
package com.example.app

import net.syarihu.licensescribe.LicenseInfo
import net.syarihu.licensescribe.LicenseProvider

object AppLicenses : LicenseProvider {
    override val all: List<LicenseInfo> = listOf(
        // All license entries
    )

    // Helper functions
    fun findByArtifactId(artifactId: String): LicenseInfo?
    fun findByLicenseName(licenseName: String): List<LicenseInfo>
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
