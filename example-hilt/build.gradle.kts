import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val versionName = findProperty("VERSION_NAME") as String

plugins {
  alias(pluginLibs.plugins.android.application)
  kotlin("plugin.compose")
  alias(exampleLibs.plugins.ksp)
  alias(exampleLibs.plugins.hilt)
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
  implementation(exampleLibs.hilt.android)
  ksp(exampleLibs.hilt.compiler)

  // Compose
  implementation(platform(exampleLibs.compose.bom))
  implementation(exampleLibs.compose.ui)
  implementation(exampleLibs.compose.ui.graphics)
  implementation(exampleLibs.compose.ui.tooling.preview)
  implementation(exampleLibs.compose.material3)
  debugImplementation(exampleLibs.compose.ui.tooling)

  // AndroidX
  implementation(exampleLibs.androidx.core.ktx)
  implementation(exampleLibs.androidx.activity.compose)
}

licenseScribe {
  baseDir.set(layout.projectDirectory.dir("licenses"))
  generatedPackageName.set("net.syarihu.licensescribe.example.hilt")
  generatedClassName.set("HiltExampleLicenses")
}
