package net.syarihu.licensescribe

/**
 * Data class representing license information for a library artifact.
 * This class is used both by generated code and can be used directly in app code.
 */
data class LicenseInfo(
  val artifactId: String,
  val artifactName: String,
  val artifactUrl: String?,
  val copyrightHolders: List<String>,
  val licenseName: String,
  val licenseUrl: String?,
  val alternativeLicenses: List<AlternativeLicenseInfo>? = null,
  val additionalLicenses: List<AdditionalLicenseInfo>? = null,
)

/**
 * Represents an alternative license (OR relationship).
 * The user can choose to comply with either the main license or any of these alternatives.
 */
data class AlternativeLicenseInfo(
  val licenseName: String,
  val licenseUrl: String?,
)

/**
 * Represents an additional license (AND relationship).
 * The user must comply with both the main license and all additional licenses.
 */
data class AdditionalLicenseInfo(
  val licenseName: String,
  val licenseUrl: String?,
)
