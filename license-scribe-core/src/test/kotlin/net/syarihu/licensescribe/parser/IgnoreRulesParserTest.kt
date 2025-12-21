package net.syarihu.licensescribe.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import net.syarihu.licensescribe.model.ArtifactId
import java.io.StringReader

class IgnoreRulesParserTest : FunSpec(
  {
    val parser = IgnoreRulesParser()

    test("parse valid ignore file") {
      val content =
        """
                # Comment line
                com.example:artifact1
                com.example:*
        """.trimIndent()

      val rules = parser.parse(StringReader(content))

      rules.rules.size shouldBe 2
    }

    test("parse empty file") {
      val content = ""
      val rules = parser.parse(StringReader(content))
      rules.rules shouldBe emptyList()
    }

    test("ignore comments and empty lines") {
      val content =
        """
                # This is a comment

                com.example:lib

                # Another comment
        """.trimIndent()

      val rules = parser.parse(StringReader(content))
      rules.rules.size shouldBe 1
    }

    test("exact match rule") {
      val content = "com.example:artifact"
      val rules = parser.parse(StringReader(content))

      rules.shouldIgnore(ArtifactId("com.example", "artifact")) shouldBe true
      rules.shouldIgnore(ArtifactId("com.example", "other")) shouldBe false
    }

    test("wildcard match rule") {
      val content = "com.example:*"
      val rules = parser.parse(StringReader(content))

      rules.shouldIgnore(ArtifactId("com.example", "artifact1")) shouldBe true
      rules.shouldIgnore(ArtifactId("com.example", "artifact2")) shouldBe true
      rules.shouldIgnore(ArtifactId("com.other", "artifact")) shouldBe false
    }

    test("serialize rules") {
      val content =
        """
                com.example:lib1
                com.example:lib2
        """.trimIndent()

      val rules = parser.parse(StringReader(content))
      val serialized = parser.serialize(rules)

      serialized.contains("com.example:lib1") shouldBe true
      serialized.contains("com.example:lib2") shouldBe true
    }
  },
)
