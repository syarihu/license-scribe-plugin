# License Scribe Plugin

A Gradle plugin that acts as your project's scribe, carefully recording and cataloging license information for Android/Kotlin dependencies into clean, accessible code.

## Features

- Automatically detects project dependencies and their licenses (including transitive dependencies)
- **Parent POM resolution** - detects licenses from parent POMs when not specified in the artifact's own POM
- **License-first YAML structure** - instantly see all licenses used in your project at a glance
- **Variant-aware** - generates separate license files per build variant (debug, release, etc.)
- **Smart license detection** - URL-based license identification for more accurate classification
- **Vendor separation** - proprietary and ambiguous licenses are kept separate per vendor
- Generates Kotlin code for easy access to license information in your app
- Works with Android (application/library) and pure Kotlin/JVM projects
- **Multi-module support** with `LicenseProvider` interface for dependency injection
- **Optional Hilt integration** for seamless DI in feature modules
- **Minimal runtime footprint** - only `LicenseInfo` and `LicenseProvider` classes are included in your APK

## Project Structure

```
license-scribe-plugin/
├── license-scribe-runtime/       # Minimal runtime library (LicenseInfo, LicenseProvider only)
├── license-scribe-core/          # Core library (parser, generator - build-time only)
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
    // Required: Runtime library provides LicenseInfo and LicenseProvider
    implementation("net.syarihu:license-scribe-runtime:0.1.0-SNAPSHOT")
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
    // licensesFile.set("scribe-licenses.yml")
    // ignoreFile.set(".scribeignore")
}
```

## Usage

### 1. Initialize License Files

Run the init task to create the initial license management files:

```bash
# For Android projects (with variants)
./gradlew scribeLicensesDebugInit

# For non-Android projects
./gradlew scribeLicensesInit
```

This creates (in variant subdirectory for Android projects):
- `licenses/{variant}/scribe-licenses.yml` - Defines licenses and their artifacts (license-first structure)
- `licenses/{variant}/.scribeignore` - Patterns for artifacts to ignore

For example, running `scribeLicensesDebugInit` with `baseDir.set(layout.projectDirectory.dir("licenses"))` creates:
```
licenses/
├── debug/
│   ├── scribe-licenses.yml
│   └── .scribeignore
└── release/
    ├── scribe-licenses.yml
    └── .scribeignore
```

This allows tracking different dependencies per build variant (e.g., `debugImplementation` vs `releaseImplementation`).

### 2. Review and Edit Definitions

Edit `scribe-licenses.yml` to add missing information. The file uses a **license-first structure** where artifacts are grouped under their license.

**Benefits of license-first structure:**
- Instantly see all license types used in your project by looking at the top-level keys
- Easily identify which libraries use specific licenses (e.g., GPL, LGPL)
- Simplifies license compliance review and auditing
- Makes it easy to spot libraries that need attention (grouped under `unknown`)

```yaml
licenses:
  apache-2.0:
    name: The Apache License, Version 2.0
    url: http://www.apache.org/licenses/LICENSE-2.0.txt
    artifacts:
      com.example:
      - name: library-name
        url: https://github.com/example/library
        copyrightHolders:
        - Example Inc.
      com.squareup.okhttp3:
      - name: okhttp
        url: https://square.github.io/okhttp/
        copyrightHolders:
        - Square, Inc.

  mit:
    name: MIT License
    url: https://opensource.org/licenses/MIT
    artifacts:
      org.example:
      - name: some-lib
        copyrightHolders:
        - Some Developer
```

#### Dual Licensing Support

For libraries with dual licensing, you can specify alternative or additional licenses:

```yaml
licenses:
  apache-2.0:
    name: Apache License 2.0
    url: https://www.apache.org/licenses/LICENSE-2.0
    artifacts:
      com.example:
      - name: dual-licensed-lib
        copyrightHolders:
        - Example Inc.
        alternativeLicenses: [mit]  # OR: user can choose either license

  gpl-2.0:
    name: GNU General Public License v2.0
    url: https://www.gnu.org/licenses/gpl-2.0.html
    artifacts:
      com.example:
      - name: classpath-exception-lib
        copyrightHolders:
        - Example Inc.
        additionalLicenses: [classpath-exception]  # AND: both licenses apply
```

#### License Detection and Normalization

The plugin automatically normalizes license names to standard keys (e.g., `apache-2.0`, `mit`, `bsd-3-clause`). Detection is performed in this order:

1. **URL-based detection** (most reliable) - checks license URL for known patterns
2. **Name-based detection** - matches common license name patterns

**Vendor-specific keys for proprietary/ambiguous licenses:**

Some POM files declare non-standard or ambiguous license names. The plugin handles these by creating vendor-specific keys:

| POM License Name | Generated Key | Example |
|------------------|---------------|---------|
| `Proprietary` | `proprietary-{vendor}` | `proprietary-adjust`, `proprietary-appsflyer` |
| `LICENSE` | `license-{vendor}` | `license-braze-inc` |

This prevents unrelated artifacts from being incorrectly grouped together.

**Ambiguous license warnings:**

When artifacts have ambiguous license names that don't identify a specific license type, the plugin shows a warning:

```
WARNING: Found 3 artifact(s) with ambiguous license names.
Please verify these licenses manually and update scribe-licenses.yml if needed:
  - com.braze:android-sdk-base: "LICENSE" (https://github.com/braze-inc/...)
  - com.adjust.signature:adjust-android-signature: "Proprietary" (https://github.com/adjust/...)
```

### 3. Check Licenses (Optional)

Check for missing attributes and validate that all dependencies are covered:

```bash
./gradlew scribeLicensesDebugCheck
```

### 4. Sync Licenses (Optional)

When dependencies change, sync definitions with current dependencies:

```bash
./gradlew scribeLicensesDebugSync
```

### 5. Generate Code

Generate Kotlin code (automatically runs during build):

```bash
./gradlew scribeLicensesDebugGenerate
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
    println("  Copyright: ${license.copyrightHolders.joinToString(", ")}")
    println("  License URL: ${license.licenseUrl}")
}
```

## Multi-Module Support

In multi-module projects, you may want to display licenses in a feature module (e.g., `:feature:settings`) while the plugin is applied to `:app`. The `LicenseProvider` interface enables this pattern.

### Using LicenseProvider Interface

The generated code implements `LicenseProvider` from `license-scribe-runtime`:

```kotlin
// Generated in :app module
object AppLicenses : LicenseProvider {
    override val all: List<LicenseInfo> = listOf(...)
}
```

### Feature Module Setup

In your feature module, depend only on `license-scribe-runtime`:

```kotlin
// feature/settings/build.gradle.kts
dependencies {
    implementation("net.syarihu:license-scribe-runtime:0.1.0-SNAPSHOT")
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
    implementation("net.syarihu:license-scribe-runtime:0.1.0-SNAPSHOT")
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
| `scribeLicenses{Variant}Init` | Initialize license management files |
| `scribeLicenses{Variant}Check` | Check definitions for missing attributes and validate against dependencies |
| `scribeLicenses{Variant}Sync` | Sync definitions with current dependencies |
| `scribeLicenses{Variant}Generate` | Generate Kotlin code for licenses |

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
# Publish runtime module
./gradlew :license-scribe-runtime:publishToMavenLocal

# Publish core module
./gradlew :license-scribe-core:publishToMavenLocal

# Publish plugins
./gradlew :license-scribe-gradle-plugin:publishToMavenLocal
./gradlew :license-scribe-hilt-plugin:publishToMavenLocal

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

### Runtime Library Classes

The `license-scribe-runtime` library provides:

#### LicenseInfo

```kotlin
package net.syarihu.licensescribe

data class LicenseInfo(
    val artifactId: String,                              // e.g., "com.example:library:1.0.0"
    val artifactName: String,                            // e.g., "library"
    val artifactUrl: String?,                            // Project URL
    val copyrightHolders: List<String>,                  // e.g., ["Example Inc."]
    val licenseName: String,                             // e.g., "The Apache License, Version 2.0"
    val licenseUrl: String?,                             // License URL
    val alternativeLicenses: List<AlternativeLicenseInfo>?,  // OR relationship (dual-licensing)
    val additionalLicenses: List<AdditionalLicenseInfo>?,    // AND relationship
)

data class AlternativeLicenseInfo(
    val licenseName: String,
    val licenseUrl: String?,
)

data class AdditionalLicenseInfo(
    val licenseName: String,
    val licenseUrl: String?,
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
    fun findByArtifactId(id: String): LicenseInfo?
    fun findByLicenseName(name: String): List<LicenseInfo>
}
```

## Acknowledgements

This project was inspired by [license-list-plugin](https://github.com/jmatsu/license-list-plugin).

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
