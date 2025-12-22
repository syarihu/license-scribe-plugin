package net.syarihu.licensescribe.model

/**
 * Represents an artifact entry within a license.
 */
data class ArtifactEntry(
  val name: String,
  val url: String? = null,
  val copyrightHolders: List<String> = emptyList(),
  val alternativeLicenses: List<String>? = null,
  val additionalLicenses: List<String>? = null,
)

/**
 * Represents a license entry with its associated artifacts.
 * Artifacts are grouped by their Maven group ID.
 */
data class LicenseEntry(
  val name: String,
  val url: String? = null,
  val artifacts: Map<String, List<ArtifactEntry>> = emptyMap(),
) {
  /**
   * Returns all artifact IDs (group:name) under this license.
   */
  fun getAllArtifactIds(): List<String> = artifacts.flatMap { (groupId, entries) ->
    entries.map { "$groupId:${it.name}" }
  }

  /**
   * Finds an artifact entry by group ID and artifact name.
   */
  fun findArtifact(
    groupId: String,
    artifactName: String,
  ): ArtifactEntry? = artifacts[groupId]?.find { it.name == artifactName }
}

/**
 * Represents the complete license catalog with all licenses and their artifacts.
 */
data class LicenseCatalog(
  val licenses: Map<String, LicenseEntry>,
) {
  companion object {
    val EMPTY = LicenseCatalog(emptyMap())
  }

  /**
   * Returns the license entry for a given license key.
   */
  fun getLicense(key: String): LicenseEntry? = licenses[key]

  /**
   * Checks if a license key exists in the catalog.
   */
  fun containsLicense(key: String): Boolean = licenses.containsKey(key)

  /**
   * Finds which license an artifact belongs to.
   * Returns the license key and artifact entry, or null if not found.
   */
  fun findArtifact(
    groupId: String,
    artifactName: String,
  ): Pair<String, ArtifactEntry>? {
    for ((licenseKey, licenseEntry) in licenses) {
      val artifact = licenseEntry.findArtifact(groupId, artifactName)
      if (artifact != null) {
        return licenseKey to artifact
      }
    }
    return null
  }

  /**
   * Returns all artifact IDs across all licenses.
   */
  fun getAllArtifactIds(): Set<String> = licenses.values.flatMap { it.getAllArtifactIds() }.toSet()

  /**
   * Returns a list of all license keys.
   */
  fun getLicenseKeys(): Set<String> = licenses.keys
}
