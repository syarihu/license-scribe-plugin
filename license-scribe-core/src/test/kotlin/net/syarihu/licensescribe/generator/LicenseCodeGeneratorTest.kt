package net.syarihu.licensescribe.generator

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.License
import net.syarihu.licensescribe.model.ResolvedLicense
import java.io.File
import kotlin.io.path.createTempDirectory

class LicenseCodeGeneratorTest : FunSpec(
  {
    val generator = LicenseCodeGenerator()

    test("generate kotlin code") {
      val tempDir = createTempDirectory().toFile()
      try {
        val licenses =
          listOf(
            ResolvedLicense(
              artifactId = ArtifactId("com.example", "lib", "1.0.0"),
              artifactName = "lib",
              artifactUrl = "https://example.com",
              copyrightHolder = "Example Inc.",
              license = License("mit", "MIT License", "https://opensource.org/licenses/MIT"),
            ),
          )

        generator.generate(
          licenses = licenses,
          packageName = "com.test",
          className = "TestLicenses",
          outputDir = tempDir,
        )

        val licenseInfoFile = File(tempDir, "com/test/LicenseInfo.kt")
        val licensesFile = File(tempDir, "com/test/TestLicenses.kt")

        licenseInfoFile.exists() shouldBe true
        licensesFile.exists() shouldBe true

        val licensesContent = licensesFile.readText()
        licensesContent shouldContain "object TestLicenses"
        licensesContent shouldContain "com.example:lib:1.0.0"
        licensesContent shouldContain "MIT License"
      } finally {
        tempDir.deleteRecursively()
      }
    }

    test("generate code with special characters") {
      val tempDir = createTempDirectory().toFile()
      try {
        val licenses =
          listOf(
            ResolvedLicense(
              artifactId = ArtifactId("com.example", "lib", "1.0.0"),
              artifactName = "lib \"with\" quotes",
              artifactUrl = null,
              copyrightHolder = null,
              license = License("custom", "Custom \"License\"", null),
            ),
          )

        generator.generate(
          licenses = licenses,
          packageName = "com.test",
          className = "Licenses",
          outputDir = tempDir,
        )

        val licensesFile = File(tempDir, "com/test/Licenses.kt")
        licensesFile.exists() shouldBe true

        val content = licensesFile.readText()
        content shouldContain "lib \\\"with\\\" quotes"
      } finally {
        tempDir.deleteRecursively()
      }
    }

    test("generate empty licenses") {
      val tempDir = createTempDirectory().toFile()
      try {
        generator.generate(
          licenses = emptyList(),
          packageName = "com.test",
          className = "EmptyLicenses",
          outputDir = tempDir,
        )

        val licensesFile = File(tempDir, "com/test/EmptyLicenses.kt")
        licensesFile.exists() shouldBe true

        val content = licensesFile.readText()
        content shouldContain "listOf()"
      } finally {
        tempDir.deleteRecursively()
      }
    }
  },
)
