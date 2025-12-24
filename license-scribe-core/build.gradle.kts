plugins {
  kotlin("jvm")
  alias(libs.plugins.dokka)
  `maven-publish`
  signing
}

group = "net.syarihu.licensescribe"
version = "0.1.0-SNAPSHOT"

kotlin {
  jvmToolchain(17)
}

dependencies {
  compileOnly(kotlin("stdlib"))
  api(project(":license-scribe-runtime"))
  implementation(libs.snakeyaml)
  implementation(libs.kotlinpoet)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

java {
  withSourcesJar()
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
  dependsOn(tasks.dokkaJavadoc)
  from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
      artifact(dokkaJavadocJar)

      pom {
        name.set("License Scribe Core")
        description.set("Core library for License Scribe - YAML parser and code generator for license management")
        url.set("https://github.com/syarihu/license-scribe-plugin")

        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }

        developers {
          developer {
            id.set("syarihu")
            name.set("syarihu")
            url.set("https://github.com/syarihu")
          }
        }

        scm {
          url.set("https://github.com/syarihu/license-scribe-plugin")
          connection.set("scm:git:git://github.com/syarihu/license-scribe-plugin.git")
          developerConnection.set("scm:git:ssh://git@github.com/syarihu/license-scribe-plugin.git")
        }
      }
    }
  }
}

signing {
  val signingKey = findProperty("signingKey")?.toString() ?: System.getenv("SIGNING_KEY")
  val signingPassword = findProperty("signingPassword")?.toString() ?: System.getenv("SIGNING_PASSWORD")

  if (signingKey != null && signingPassword != null) {
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["maven"])
  }
}
