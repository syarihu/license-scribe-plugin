package net.syarihu.licensescribe.gradle.task.model

/**
 * Represents a license entry from a POM file.
 */
data class PomLicense(
  val name: String,
  val url: String?,
)
