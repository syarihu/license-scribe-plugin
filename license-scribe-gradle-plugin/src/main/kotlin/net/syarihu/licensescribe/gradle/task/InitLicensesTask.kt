package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.model.Catalog
import net.syarihu.licensescribe.model.License
import net.syarihu.licensescribe.model.Record
import net.syarihu.licensescribe.model.RecordGroup
import net.syarihu.licensescribe.model.ScopedRecords
import net.syarihu.licensescribe.parser.CatalogParser
import net.syarihu.licensescribe.parser.RecordsParser
import org.gradle.api.tasks.TaskAction

/**
 * Task to initialize license management files.
 * Creates scribe-records.yml, scribe-catalog.yml, and .scribeignore if they don't exist.
 */
abstract class InitLicensesTask : BaseLicenseTask() {
  @TaskAction
  fun execute() {
    val recordsFile = resolveRecordsFile()
    val catalogFile = resolveCatalogFile()
    val ignoreFile = resolveIgnoreFile()

    val ignoreRules = loadIgnoreRules()
    val dependencies =
      resolveDependencies()
        .filterNot { ignoreRules.shouldIgnore(it) }

    // Group dependencies by scope and group
    val scopedRecords = mutableListOf<ScopedRecords>()
    val licenseMap = mutableMapOf<String, License>()

    val groups =
      dependencies.groupBy { it.group }.map { (groupId, artifacts) ->
        val records =
          artifacts.map { artifact ->
            val pomInfo = resolvePomInfo(artifact)

            // Try to determine license from POM
            val licenseKey =
              pomInfo?.licenses?.firstOrNull()?.let { pomLicense ->
                val key = normalizeLicenseKey(pomLicense.name)
                if (!licenseMap.containsKey(key)) {
                  licenseMap[key] =
                    License(
                      key = key,
                      name = pomLicense.name,
                      url = pomLicense.url,
                    )
                }
                key
              } ?: "unknown"

            Record(
              name = artifact.name,
              url = pomInfo?.url?.let { stripVersionFromUrl(it) },
              copyrightHolder = pomInfo?.developers?.firstOrNull(),
              license = licenseKey,
            )
          }
        RecordGroup(groupId = groupId, records = records)
      }

    if (groups.isNotEmpty()) {
      scopedRecords.add(ScopedRecords(scope = "implementation", groups = groups))
    }

    // Write records file
    if (!recordsFile.exists() || recordsFile.length() == 0L) {
      val content = RecordsParser().serialize(scopedRecords)
      recordsFile.writeText(content)
      logger.lifecycle("Created ${recordsFile.name} with ${dependencies.size} records")
    } else {
      logger.lifecycle("${recordsFile.name} already exists, skipping")
    }

    // Add default licenses if not present
    addDefaultLicenses(licenseMap)

    // Write catalog file
    if (!catalogFile.exists() || catalogFile.length() == 0L) {
      val catalog = Catalog(licenseMap)
      val content = CatalogParser().serialize(catalog)
      catalogFile.writeText(content)
      logger.lifecycle("Created ${catalogFile.name} with ${licenseMap.size} licenses")
    } else {
      logger.lifecycle("${catalogFile.name} already exists, skipping")
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

  private fun addDefaultLicenses(licenseMap: MutableMap<String, License>) {
    val defaults =
      mapOf(
        "apache-2.0" to
          License(
            key = "apache-2.0",
            name = "Apache License 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0",
          ),
        "mit" to
          License(
            key = "mit",
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT",
          ),
        "bsd-3-clause" to
          License(
            key = "bsd-3-clause",
            name = "BSD 3-Clause License",
            url = "https://opensource.org/licenses/BSD-3-Clause",
          ),
        "bsd-2-clause" to
          License(
            key = "bsd-2-clause",
            name = "BSD 2-Clause License",
            url = "https://opensource.org/licenses/BSD-2-Clause",
          ),
        "lgpl-2.1" to
          License(
            key = "lgpl-2.1",
            name = "GNU Lesser General Public License v2.1",
            url = "https://www.gnu.org/licenses/lgpl-2.1.html",
          ),
        "lgpl-3.0" to
          License(
            key = "lgpl-3.0",
            name = "GNU Lesser General Public License v3.0",
            url = "https://www.gnu.org/licenses/lgpl-3.0.html",
          ),
        "epl-1.0" to
          License(
            key = "epl-1.0",
            name = "Eclipse Public License 1.0",
            url = "https://www.eclipse.org/legal/epl-v10.html",
          ),
        "unknown" to
          License(
            key = "unknown",
            name = "Unknown License",
            url = null,
          ),
      )

    defaults.forEach { (key, license) ->
      if (!licenseMap.containsKey(key)) {
        licenseMap[key] = license
      }
    }
  }
}
