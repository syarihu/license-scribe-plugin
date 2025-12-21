pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()
    google()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    google()
  }
}

rootProject.name = "license-scribe-plugin"

include(":license-scribe-runtime")
include(":license-scribe-core")
include(":license-scribe-gradle-plugin")
include(":license-scribe-hilt-plugin")

// example modules require the plugin to be published to Maven Local first
// Use -PexcludeExample to skip them during initial build
if (providers.gradleProperty("excludeExample").isPresent.not()) {
    include(":example")
    include(":example-library")
}
