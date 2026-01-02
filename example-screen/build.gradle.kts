plugins {
  alias(libs.plugins.android.application)
  kotlin("android")
  kotlin("plugin.compose")
  id("net.syarihu.license-scribe-screen")
}

android {
  namespace = "net.syarihu.licensescribe.example.screen"
  compileSdk = 36

  defaultConfig {
    applicationId = "net.syarihu.licensescribe.example.screen"
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

  buildFeatures {
    compose = true
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
  }
}

val versionName = findProperty("VERSION_NAME") as String

dependencies {
  // License Scribe Runtime (provides LicenseInfo, LicenseProvider classes)
  implementation("net.syarihu.licensescribe:license-scribe-runtime:$versionName")

  // AndroidX (required for generated Activity)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.recyclerview)

  // Compose
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material3)
  implementation(libs.androidx.activity.compose)
  debugImplementation(libs.compose.ui.tooling)

  // Example dependencies for license detection
  implementation("com.squareup.okhttp3:okhttp:5.3.2")
  implementation("com.google.code.gson:gson:2.13.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

licenseScribeScreen {
  generatedPackageName.set("net.syarihu.licensescribe.example.screen")
}
