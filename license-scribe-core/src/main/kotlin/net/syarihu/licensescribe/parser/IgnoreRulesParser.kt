package net.syarihu.licensescribe.parser

import net.syarihu.licensescribe.model.IgnoreRule
import net.syarihu.licensescribe.model.IgnoreRules
import java.io.File
import java.io.Reader

/**
 * Parser for .artifactignore files.
 *
 * Expected format:
 * ```
 * # Comment lines start with #
 * com.example:artifact-to-ignore
 * com.example:*
 * ```
 */
class IgnoreRulesParser {
  fun parse(file: File): IgnoreRules {
    if (!file.exists()) {
      return IgnoreRules.EMPTY
    }
    return file.reader().use { parse(it) }
  }

  fun parse(reader: Reader): IgnoreRules {
    val rules =
      reader
        .readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { IgnoreRule(it) }

    return IgnoreRules(rules)
  }

  fun serialize(ignoreRules: IgnoreRules): String = ignoreRules.rules.joinToString("\n") { it.pattern }
}
