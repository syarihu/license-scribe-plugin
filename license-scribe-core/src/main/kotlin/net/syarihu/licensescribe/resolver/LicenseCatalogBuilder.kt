package net.syarihu.licensescribe.resolver

import net.syarihu.licensescribe.model.ArtifactEntry
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.model.LicenseEntry
import net.syarihu.licensescribe.model.PomInfo
import net.syarihu.licensescribe.util.LicenseNormalizer
import net.syarihu.licensescribe.util.WellKnownLicenses

/**
 * Data class representing a dependency with its resolved POM info.
 */
data class DependencyInfo(
  val artifactId: ArtifactId,
  val pomInfo: PomInfo?,
)

/**
 * Builds a LicenseCatalog from dependency information.
 */
object LicenseCatalogBuilder {

  /**
   * Build a LicenseCatalog from a list of dependencies with their POM info.
   *
   * @param dependencies List of dependencies with resolved POM info
   * @return A LicenseCatalog containing all licenses and artifacts
   */
  fun build(dependencies: List<DependencyInfo>): LicenseCatalog {
    val licenseToArtifacts = mutableMapOf<String, MutableMap<String, MutableList<ArtifactEntry>>>()
    val licenseInfoMap = mutableMapOf<String, Pair<String, String?>>()

    dependencies.forEach { dependency ->
      val artifact = dependency.artifactId
      val pomInfo = dependency.pomInfo
      val pomLicense = pomInfo?.licenses?.firstOrNull()
      val licenseKey = pomLicense?.let {
        val key = LicenseNormalizer.normalizeKey(it.name, it.url)
        if (!licenseInfoMap.containsKey(key)) {
          licenseInfoMap[key] = it.name to it.url
        }
        key
      } ?: "unknown"

      // Ensure unknown license exists
      if (!licenseInfoMap.containsKey("unknown")) {
        licenseInfoMap["unknown"] = "Unknown License" to null
      }

      val copyrightHolders = pomInfo?.developers?.takeIf { it.isNotEmpty() } ?: emptyList()

      val artifactEntry = ArtifactEntry(
        name = artifact.name,
        url = pomInfo?.url?.let { LicenseNormalizer.stripVersionFromUrl(it) },
        copyrightHolders = copyrightHolders,
      )

      licenseToArtifacts
        .getOrPut(licenseKey) { mutableMapOf() }
        .getOrPut(artifact.group) { mutableListOf() }
        .add(artifactEntry)
    }

    // Supplement license info with well-known defaults
    WellKnownLicenses.supplementLicenseInfo(licenseInfoMap)

    // Build final catalog
    val licenses = licenseInfoMap
      .filter { (key, _) -> licenseToArtifacts.containsKey(key) }
      .map { (key, nameUrl) ->
        val (name, url) = nameUrl
        val artifacts = licenseToArtifacts[key]?.mapValues { (_, entries) ->
          entries.sortedBy { it.name }
        }?.toSortedMap() ?: emptyMap()

        key to LicenseEntry(
          name = name,
          url = url,
          artifacts = artifacts,
        )
      }.toMap().toSortedMap()

    return LicenseCatalog(licenses)
  }
}
