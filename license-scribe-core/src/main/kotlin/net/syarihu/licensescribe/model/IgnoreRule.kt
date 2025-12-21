package net.syarihu.licensescribe.model

import java.util.regex.Pattern

/**
 * Represents a rule for ignoring artifacts.
 */
data class IgnoreRule(
  val pattern: String,
) {
  private val compiledPattern: Pattern by lazy {
    val regexPattern =
      if (pattern.contains("*") && !pattern.contains(".*")) {
        // Convert glob pattern to regex
        pattern
          .replace(".", "\\.")
          .replace("*", ".*")
      } else {
        pattern
      }
    Pattern.compile(regexPattern)
  }

  fun matches(coordinate: String): Boolean = compiledPattern.matcher(coordinate).matches()
}

/**
 * Represents a collection of ignore rules.
 */
data class IgnoreRules(
  val rules: List<IgnoreRule>,
) {
  companion object {
    val EMPTY = IgnoreRules(emptyList())
  }

  fun shouldIgnore(artifactId: ArtifactId): Boolean {
    val coordinate = artifactId.coordinate
    return rules.any { it.matches(coordinate) }
  }
}
