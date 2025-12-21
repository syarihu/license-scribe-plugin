plugins {
  alias(libs.plugins.kotlin.jvm)
  `java-gradle-plugin`
  `maven-publish`
}

group = "net.syarihu"
version = "0.1.0-SNAPSHOT"

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":license-scribe-core"))
  compileOnly(libs.android.gradle.plugin)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

gradlePlugin {
  plugins {
    create("licenseScribe") {
      id = "net.syarihu.license-scribe"
      implementationClass = "net.syarihu.licensescribe.gradle.LicenseScribePlugin"
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
