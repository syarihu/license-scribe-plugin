plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  id("net.syarihu.license-scribe") version "0.1.0-SNAPSHOT"
}

android {
  namespace = "net.syarihu.licensescribe.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "net.syarihu.licensescribe.example"
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

dependencies {
  // Internal library module (to test transitive dependency detection)
  implementation(project(":example-library"))

  // AndroidX
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Compose
  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.ui.tooling.preview)
  implementation(libs.compose.material3)
  debugImplementation(libs.compose.ui.tooling)

  // Example dependencies for license detection
  implementation("com.squareup.okhttp3:okhttp:5.3.2")
  implementation("com.google.code.gson:gson:2.13.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

licenseScribe {
  baseDir.set(layout.projectDirectory.dir("licenses"))
  generatedPackageName.set("net.syarihu.licensescribe.example")
  generatedClassName.set("AppLicenses")
}
