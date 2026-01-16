import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val versionName = findProperty("VERSION_NAME") as String

plugins {
  alias(libs.plugins.android.application)
  kotlin("android")
  kotlin("plugin.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  id("net.syarihu.license-scribe")
  id("net.syarihu.license-scribe-hilt")
}

hilt {
  enableAggregatingTask = false
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

  buildFeatures {
    compose = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

dependencies {
  // License Scribe runtime
  implementation("net.syarihu.licensescribe:license-scribe-runtime:$versionName")

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Compose
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material3)
  debugImplementation(libs.compose.ui.tooling)

  // AndroidX
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
}

licenseScribe {
  baseDir.set(layout.projectDirectory.dir("licenses"))
  generatedPackageName.set("net.syarihu.licensescribe.example.hilt")
  generatedClassName.set("HiltExampleLicenses")
}
