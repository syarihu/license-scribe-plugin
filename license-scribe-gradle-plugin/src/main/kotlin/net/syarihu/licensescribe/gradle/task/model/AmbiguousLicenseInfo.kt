package net.syarihu.licensescribe.gradle.task.model

/**
 * Data class to hold information about artifacts with ambiguous licenses.
 */
data class AmbiguousLicenseInfo(
  val coordinate: String,
  val licenseName: String,
  val licenseUrl: String?,
)
