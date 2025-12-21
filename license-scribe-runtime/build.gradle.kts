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

// No dependencies - this is a minimal runtime library
