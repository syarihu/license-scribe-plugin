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
  implementation(libs.kotlinpoet)
  compileOnly(libs.android.gradle.plugin)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

gradlePlugin {
  plugins {
    create("licenseScribeHilt") {
      id = "net.syarihu.license-scribe-hilt"
      implementationClass = "net.syarihu.licensescribe.hilt.LicenseScribeHiltPlugin"
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
