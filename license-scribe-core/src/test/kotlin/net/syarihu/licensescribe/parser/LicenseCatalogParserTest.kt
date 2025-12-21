package net.syarihu.licensescribe.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.StringReader

class LicenseCatalogParserTest : FunSpec(
  {
    val parser = LicenseCatalogParser()

    test("parse valid yaml") {
      val yaml =
        """
                apache-2.0:
                  name: Apache License 2.0
                  url: https://www.apache.org/licenses/LICENSE-2.0
                mit:
                  name: MIT License
                  url: https://opensource.org/licenses/MIT
        """.trimIndent()

      val catalog = parser.parse(StringReader(yaml))

      catalog.licenses.size shouldBe 2
      catalog.getLicense("apache-2.0") shouldNotBe null
      catalog.getLicense("apache-2.0")?.name shouldBe "Apache License 2.0"
      catalog.getLicense("apache-2.0")?.url shouldBe "https://www.apache.org/licenses/LICENSE-2.0"
      catalog.getLicense("mit")?.name shouldBe "MIT License"
    }

    test("parse empty yaml") {
      val yaml = ""
      val catalog = parser.parse(StringReader(yaml))
      catalog.licenses shouldBe emptyMap()
    }

    test("parse license without url") {
      val yaml =
        """
                custom:
                  name: Custom License
        """.trimIndent()

      val catalog = parser.parse(StringReader(yaml))
      catalog.getLicense("custom")?.url shouldBe null
    }

    test("serialize catalog") {
      val yaml = """
                apache-2.0:
                  name: Apache License 2.0
                  url: https://www.apache.org/licenses/LICENSE-2.0
        """.trimIndent()

      val catalog = parser.parse(StringReader(yaml))
      val serialized = parser.serialize(catalog)

      serialized.contains("apache-2.0") shouldBe true
      serialized.contains("Apache License 2.0") shouldBe true
    }
  },
)
