plugins {
  alias(pluginLibs.plugins.android.application)
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
  implementation(exampleLibs.androidx.core.ktx)
  implementation(exampleLibs.androidx.activity)
  implementation(exampleLibs.androidx.appcompat)
  implementation(exampleLibs.androidx.recyclerview)

  // Compose
  implementation(platform(exampleLibs.compose.bom))
  implementation(exampleLibs.compose.ui)
  implementation(exampleLibs.compose.ui.graphics)
  implementation(exampleLibs.compose.ui.tooling.preview)
  implementation(exampleLibs.compose.material3)
  implementation(exampleLibs.androidx.activity.compose)
  debugImplementation(exampleLibs.compose.ui.tooling)

  // Example dependencies for license detection
  implementation(exampleLibs.okhttp)
  implementation(exampleLibs.gson)
  implementation(exampleLibs.kotlinx.coroutines.core)
}

licenseScribeScreen {
  generatedPackageName.set("net.syarihu.licensescribe.example.screen")
}
