plugins {
  kotlin("jvm")
  alias(libs.plugins.dokka)
  `java-gradle-plugin`
  `maven-publish`
  signing
}

group = "net.syarihu.licensescribe"
version = findProperty("VERSION_NAME") as String

kotlin {
  jvmToolchain(17)
}

java {
  withSourcesJar()
}

dependencies {
  implementation(project(":license-scribe-core"))
  implementation(project(":license-scribe-gradle-plugin"))
  compileOnly(libs.android.gradle.plugin)

  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotest.property)
}

gradlePlugin {
  plugins {
    create("licenseScribeScreen") {
      id = "net.syarihu.license-scribe-screen"
      implementationClass = "net.syarihu.licensescribe.screen.LicenseScribeScreenPlugin"
      displayName = "License Scribe Screen Plugin"
      description = "A Gradle plugin that auto-generates LicenseListActivity with embedded license data"
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
  dependsOn(tasks.dokkaJavadoc)
  from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
  archiveClassifier.set("javadoc")
}

publishing {
  publications {
    // java-gradle-plugin creates "pluginMaven" publication automatically
    withType<MavenPublication>().configureEach {
      if (name == "pluginMaven") {
        artifact(dokkaJavadocJar)
      }

      pom {
        name.set("License Scribe Screen Plugin")
        description.set("A Gradle plugin that auto-generates LicenseListActivity with embedded license data")
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
    sign(publishing.publications)
  }
}
