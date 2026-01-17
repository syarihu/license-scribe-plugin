val versionName = findProperty("VERSION_NAME") as String

plugins {
  alias(pluginLibs.plugins.android.application)
  kotlin("plugin.compose")
  id("net.syarihu.license-scribe")
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
  // License Scribe runtime (minimal dependency for LicenseProvider and LicenseInfo)
  implementation("net.syarihu.licensescribe:license-scribe-runtime:$versionName")

  // Internal library module (to test transitive dependency detection)
  implementation(project(":example-library"))

  // AndroidX
  implementation(exampleLibs.androidx.core.ktx)
  implementation(exampleLibs.androidx.lifecycle.runtime.ktx)
  implementation(exampleLibs.androidx.activity.compose)

  // Compose
  implementation(platform(exampleLibs.compose.bom))
  implementation(exampleLibs.compose.ui)
  implementation(exampleLibs.compose.ui.graphics)
  implementation(exampleLibs.compose.ui.tooling.preview)
  implementation(exampleLibs.compose.material3)
  debugImplementation(exampleLibs.compose.ui.tooling)

  // Example dependencies for license detection
  implementation(exampleLibs.okhttp)
  implementation(exampleLibs.gson)
  implementation(exampleLibs.kotlinx.coroutines.core)
  implementation(platform(exampleLibs.firebase.bom))
  implementation(exampleLibs.firebase.analytics)
}

licenseScribe {
  baseDir.set(layout.projectDirectory.dir("licenses"))
  generatedPackageName.set("net.syarihu.licensescribe.example")
  generatedClassName.set("AppLicenses")
}
