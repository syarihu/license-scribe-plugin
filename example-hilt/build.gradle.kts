val versionName = findProperty("VERSION_NAME") as String

plugins {
  alias(libs.plugins.android.application)
  kotlin("android")
  id("net.syarihu.license-scribe")
  id("net.syarihu.license-scribe-hilt")
}

android {
  namespace = "net.syarihu.licensescribe.example.hilt"
  compileSdk = 36

  defaultConfig {
    applicationId = "net.syarihu.licensescribe.example.hilt"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

dependencies {
  // License Scribe runtime
  implementation("net.syarihu.licensescribe:license-scribe-runtime:$versionName")

  // Hilt (for annotations only - we don't use Hilt Gradle plugin due to compatibility issues)
  implementation(libs.hilt.android)

  // AndroidX (minimal)
  implementation(libs.androidx.core.ktx)
}

licenseScribe {
  baseDir.set(layout.projectDirectory.dir("licenses"))
  generatedPackageName.set("net.syarihu.licensescribe.example.hilt")
  generatedClassName.set("HiltExampleLicenses")
}
