package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.model.ArtifactEntry
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.model.LicenseEntry
import net.syarihu.licensescribe.parser.LicenseCatalogParser
import org.gradle.api.tasks.TaskAction

/**
 * Task to initialize license management files.
 * Creates scribe-licenses.yml and .scribeignore if they don't exist.
 */
abstract class InitLicensesTask : BaseLicenseTask() {
  @TaskAction
  fun execute() {
    val licensesFile = resolveLicensesFile()
    val ignoreFile = resolveIgnoreFile()

    val ignoreRules = loadIgnoreRules()
    val dependencies =
      resolveDependencies()
        .filterNot { ignoreRules.shouldIgnore(it) }

    // Group dependencies by license, then by group
    // Map: licenseKey -> (groupId -> List<ArtifactEntry>)
    val licenseToArtifacts = mutableMapOf<String, MutableMap<String, MutableList<ArtifactEntry>>>()
    val licenseInfoMap = mutableMapOf<String, Pair<String, String?>>() // licenseKey -> (name, url)

    dependencies.forEach { artifact ->
      val pomInfo = resolvePomInfo(artifact)

      // Determine license from POM
      val licenseKey = pomInfo?.licenses?.firstOrNull()?.let { pomLicense ->
        val key = normalizeLicenseKey(pomLicense.name)
        if (!licenseInfoMap.containsKey(key)) {
          licenseInfoMap[key] = pomLicense.name to pomLicense.url
        }
        key
      } ?: "unknown"

      // Ensure unknown license is in the map
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
    }

    // Supplement license info with well-known defaults (name/URL only)
    supplementLicenseInfo(licenseInfoMap)

    // Build LicenseCatalog - only include licenses that have artifacts
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

    // Write licenses file
    if (!licensesFile.exists() || licensesFile.length() == 0L) {
      val content = LicenseCatalogParser().serialize(catalog)
      licensesFile.writeText(content)
      val artifactCount = catalog.getAllArtifactIds().size
      logger.lifecycle("Created ${licensesFile.name} with $artifactCount artifacts in ${licenses.size} licenses")
    } else {
      logger.lifecycle("${licensesFile.name} already exists, skipping")
    }

    // Write ignore file
    if (!ignoreFile.exists()) {
      ignoreFile.writeText(
        """
                # Patterns to ignore artifacts
                # Examples:
                # com.example:artifact-name
                # com.example:*
        """.trimIndent(),
      )
      logger.lifecycle("Created ${ignoreFile.name}")
    } else {
      logger.lifecycle("${ignoreFile.name} already exists, skipping")
    }
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
        // Use well-known name/url if current one seems auto-generated or missing
        if (existing != null) {
          val currentName = existing.first
          val currentUrl = existing.second
          licenseInfoMap[key] = (name) to (url ?: currentUrl)
        }
      }
    }
  }
}
