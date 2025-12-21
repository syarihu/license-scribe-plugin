package net.syarihu.licensescribe.model

/**
 * Represents a license definition.
 */
data class License(
  val key: String,
  val name: String,
  val url: String? = null,
)
