package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.model.ArtifactEntry
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.model.LicenseEntry
import net.syarihu.licensescribe.parser.LicenseCatalogParser
import org.gradle.api.tasks.TaskAction

/**
 * Task to sync license records with current dependencies.
 * Adds new dependencies and removes ones no longer present.
 */
abstract class SyncLicensesTask : BaseLicenseTask() {
  @TaskAction
  fun execute() {
    val existingCatalog = loadLicenseCatalog()
    val ignoreRules = loadIgnoreRules()

    // Build lookup map of existing artifacts: "group:name" -> (licenseKey, ArtifactEntry)
    val existingMap = mutableMapOf<String, Pair<String, ArtifactEntry>>()
    existingCatalog.licenses.forEach { (licenseKey, licenseEntry) ->
      licenseEntry.artifacts.forEach { (groupId, artifacts) ->
        artifacts.forEach { artifact ->
          existingMap["$groupId:${artifact.name}"] = licenseKey to artifact
        }
      }
    }

    // Get current dependencies
    val currentDependencies =
      resolveDependencies()
        .filterNot { ignoreRules.shouldIgnore(it) }

    // Build new catalog structure: licenseKey -> (groupId -> List<ArtifactEntry>)
    val licenseToArtifacts = mutableMapOf<String, MutableMap<String, MutableList<ArtifactEntry>>>()
    val licenseInfoMap = mutableMapOf<String, Pair<String, String?>>() // licenseKey -> (name, url)

    // Copy existing license info
    existingCatalog.licenses.forEach { (key, entry) ->
      licenseInfoMap[key] = entry.name to entry.url
    }

    var addedCount = 0
    var removedCount = 0

    // Process current dependencies
    currentDependencies.forEach { artifact ->
      val coord = "${artifact.group}:${artifact.name}"
      val existing = existingMap[coord]

      if (existing != null) {
        // Keep existing record with its license
        val (licenseKey, existingArtifact) = existing
        licenseToArtifacts
          .getOrPut(licenseKey) { mutableMapOf() }
          .getOrPut(artifact.group) { mutableListOf() }
          .add(existingArtifact)
      } else {
        // Add new record
        val pomInfo = resolvePomInfo(artifact)
        val licenseKey = pomInfo?.licenses?.firstOrNull()?.let { pomLicense ->
          val key = normalizeLicenseKey(pomLicense.name)
          if (!licenseInfoMap.containsKey(key)) {
            licenseInfoMap[key] = pomLicense.name to pomLicense.url
          }
          key
        } ?: "unknown"

        // Ensure unknown license exists
        if (!licenseInfoMap.containsKey("unknown")) {
          licenseInfoMap["unknown"] = "Unknown License" to null
        }

        val artifactEntry = ArtifactEntry(
          name = artifact.name,
          url = pomInfo?.url?.let { stripVersionFromUrl(it) },
          copyrightHolders = pomInfo?.developers?.takeIf { it.isNotEmpty() } ?: emptyList(),
        )

        licenseToArtifacts
          .getOrPut(licenseKey) { mutableMapOf() }
          .getOrPut(artifact.group) { mutableListOf() }
          .add(artifactEntry)

        addedCount++
      }
    }

    // Count removed artifacts
    val currentCoords = currentDependencies.map { "${it.group}:${it.name}" }.toSet()
    existingMap.keys.forEach { coord ->
      if (coord !in currentCoords) {
        removedCount++
      }
    }

    // Supplement license info with well-known defaults (name/URL only)
    supplementLicenseInfo(licenseInfoMap)

    // Build final catalog - only include licenses that have artifacts
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

    val catalog = LicenseCatalog(licenses)

    // Write updated file
    val licensesFile = resolveLicensesFile()
    licensesFile.writeText(LicenseCatalogParser().serialize(catalog))

    logger.lifecycle(
      "Sync completed: $addedCount added, $removedCount removed",
    )
  }

  private fun normalizeLicenseKey(licenseName: String): String {
    val lower = licenseName.lowercase()
    return when {
      lower.contains("apache") && lower.contains("2") -> "apache-2.0"
      lower.contains("mit") -> "mit"
      lower.contains("bsd") && lower.contains("3") -> "bsd-3-clause"
      lower.contains("bsd") && lower.contains("2") -> "bsd-2-clause"
      lower.contains("bsd") -> "bsd"
      lower.contains("lgpl") && lower.contains("3") -> "lgpl-3.0"
      lower.contains("lgpl") && lower.contains("2.1") -> "lgpl-2.1"
      lower.contains("gpl") && lower.contains("3") -> "gpl-3.0"
      lower.contains("gpl") && lower.contains("2") -> "gpl-2.0"
      lower.contains("eclipse") || lower.contains("epl") -> "epl-1.0"
      lower.contains("mozilla") || lower.contains("mpl") -> "mpl-2.0"
      lower.contains("creative commons") && lower.contains("zero") -> "cc0-1.0"
      lower.contains("unlicense") -> "unlicense"
      lower.contains("isc") -> "isc"
      else ->
        licenseName
          .lowercase()
          .replace(Regex("[^a-z0-9]+"), "-")
          .trim('-')
    }
  }

  private fun stripVersionFromUrl(url: String): String = url
    .replace(Regex("#[\\d.]+$"), "") // Remove #1.7.6 style version anchors
    .replace(Regex("/[\\d.]+/?$"), "") // Remove /1.7.6 style version paths

  /**
   * Supplements existing license entries with well-known names and URLs.
   * Only updates licenses that already exist in the map (detected from dependencies).
   */
  private fun supplementLicenseInfo(licenseInfoMap: MutableMap<String, Pair<String, String?>>) {
    val wellKnownLicenses = mapOf(
      "apache-2.0" to ("Apache License 2.0" to "https://www.apache.org/licenses/LICENSE-2.0"),
      "mit" to ("MIT License" to "https://opensource.org/licenses/MIT"),
      "bsd-3-clause" to ("BSD 3-Clause License" to "https://opensource.org/licenses/BSD-3-Clause"),
      "bsd-2-clause" to ("BSD 2-Clause License" to "https://opensource.org/licenses/BSD-2-Clause"),
      "lgpl-2.1" to ("GNU Lesser General Public License v2.1" to "https://www.gnu.org/licenses/lgpl-2.1.html"),
      "lgpl-3.0" to ("GNU Lesser General Public License v3.0" to "https://www.gnu.org/licenses/lgpl-3.0.html"),
      "epl-1.0" to ("Eclipse Public License 1.0" to "https://www.eclipse.org/legal/epl-v10.html"),
      "mpl-2.0" to ("Mozilla Public License 2.0" to "https://www.mozilla.org/en-US/MPL/2.0/"),
      "gpl-2.0" to ("GNU General Public License v2.0" to "https://www.gnu.org/licenses/gpl-2.0.html"),
      "gpl-3.0" to ("GNU General Public License v3.0" to "https://www.gnu.org/licenses/gpl-3.0.html"),
      "cc0-1.0" to ("CC0 1.0 Universal" to "https://creativecommons.org/publicdomain/zero/1.0/"),
      "unlicense" to ("The Unlicense" to "https://unlicense.org/"),
      "isc" to ("ISC License" to "https://opensource.org/licenses/ISC"),
      "unknown" to ("Unknown License" to null),
    )

    // Only supplement info for licenses that already exist
    licenseInfoMap.keys.toList().forEach { key ->
      wellKnownLicenses[key]?.let { (name, url) ->
        val existing = licenseInfoMap[key]
        if (existing != null) {
          val currentUrl = existing.second
          licenseInfoMap[key] = name to (url ?: currentUrl)
        }
      }
    }
  }
}
