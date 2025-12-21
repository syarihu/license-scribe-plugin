package net.syarihu.licensescribe.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ArtifactIdTest : FunSpec(
  {
    test("create artifact id") {
      val id = ArtifactId("com.example", "lib", "1.0.0")

      id.group shouldBe "com.example"
      id.name shouldBe "lib"
      id.version shouldBe "1.0.0"
    }

    test("coordinate with version") {
      val id = ArtifactId("com.example", "lib", "1.0.0")
      id.coordinate shouldBe "com.example:lib:1.0.0"
    }

    test("coordinate without version") {
      val id = ArtifactId("com.example", "lib")
      id.coordinate shouldBe "com.example:lib"
    }

    test("parse coordinate with version") {
      val id = ArtifactId.parse("com.example:lib:1.0.0")

      id.group shouldBe "com.example"
      id.name shouldBe "lib"
      id.version shouldBe "1.0.0"
    }

    test("parse coordinate without version") {
      val id = ArtifactId.parse("com.example:lib")

      id.group shouldBe "com.example"
      id.name shouldBe "lib"
      id.version shouldBe null
    }

    test("parse invalid coordinate throws exception") {
      shouldThrow<IllegalArgumentException> {
        ArtifactId.parse("invalid")
      }
    }
  },
)
