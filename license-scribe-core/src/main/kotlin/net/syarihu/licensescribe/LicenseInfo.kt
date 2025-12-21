package net.syarihu.licensescribe

/**
 * Data class representing license information for a library artifact.
 * This class is used both by generated code and can be used directly in app code.
 */
data class LicenseInfo(
  val artifactId: String,
  val artifactName: String,
  val artifactUrl: String?,
  val copyrightHolder: String?,
  val licenseName: String,
  val licenseUrl: String?,
)
