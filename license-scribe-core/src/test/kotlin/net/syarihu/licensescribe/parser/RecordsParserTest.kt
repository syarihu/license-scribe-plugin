package net.syarihu.licensescribe.parser

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.StringReader

class RecordsParserTest : FunSpec(
  {
    val parser = RecordsParser()

    test("parse valid yaml") {
      val yaml =
        """
                implementation:
                  com.squareup.okhttp3:
                    - name: okhttp
                      url: https://github.com/square/okhttp
                      copyrightHolder: Square, Inc.
                      license: apache-2.0
                  com.google.code.gson:
                    - name: gson
                      copyrightHolder: Google Inc.
                      license: apache-2.0
        """.trimIndent()

      val result = parser.parse(StringReader(yaml))

      result.size shouldBe 1
      result[0].scope shouldBe "implementation"
      result[0].groups.size shouldBe 2

      val okhttpGroup = result[0].groups.find { it.groupId == "com.squareup.okhttp3" }
      okhttpGroup?.records?.size shouldBe 1
      okhttpGroup?.records?.get(0)?.name shouldBe "okhttp"
      okhttpGroup?.records?.get(0)?.license shouldBe "apache-2.0"
    }

    test("parse empty yaml") {
      val yaml = ""
      val result = parser.parse(StringReader(yaml))
      result shouldBe emptyList()
    }

    test("parse multiple scopes") {
      val yaml =
        """
                implementation:
                  com.example:
                    - name: lib1
                      license: mit
                testImplementation:
                  com.test:
                    - name: test-lib
                      license: apache-2.0
        """.trimIndent()

      val result = parser.parse(StringReader(yaml))

      result.size shouldBe 2
      result.map { it.scope }.toSet() shouldBe setOf("implementation", "testImplementation")
    }

    test("serialize records") {
      val yaml =
        """
                implementation:
                  com.example:
                    - name: lib
                      license: mit
        """.trimIndent()

      val records = parser.parse(StringReader(yaml))
      val serialized = parser.serialize(records)

      serialized.contains("implementation") shouldBe true
      serialized.contains("com.example") shouldBe true
      serialized.contains("lib") shouldBe true
    }
  },
)
