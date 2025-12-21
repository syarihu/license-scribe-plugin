plugins {
  alias(libs.plugins.kotlin.jvm)
  `maven-publish`
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}

group = "net.syarihu"
version = "0.1.0-SNAPSHOT"

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(libs.snakeyaml)
  implementation(libs.kotlinpoet)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}
