package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.gradle.task.model.AmbiguousLicenseInfo
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
    val ambiguousLicenseArtifacts = mutableListOf<AmbiguousLicenseInfo>()

    // Process current dependencies
    currentDependencies.forEach { artifact ->
      val coordinate = "${artifact.group}:${artifact.name}"
      val existing = existingMap[coordinate]

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
        val pomLicense = pomInfo?.licenses?.firstOrNull()
        val licenseKey = pomLicense?.let {
          val key = normalizeLicenseKey(it.name, it.url)
          if (!licenseInfoMap.containsKey(key)) {
            licenseInfoMap[key] = it.name to it.url
          }
          key
        } ?: "unknown"

        // Check for ambiguous license on new artifacts
        if (pomLicense != null && isAmbiguousLicense(pomLicense.name, pomLicense.url)) {
          ambiguousLicenseArtifacts.add(
            AmbiguousLicenseInfo(
              coordinate = coordinate,
              licenseName = pomLicense.name,
              licenseUrl = pomLicense.url,
            ),
          )
        }

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
    val currentCoordinates = currentDependencies.map { "${it.group}:${it.name}" }.toSet()
    existingMap.keys.forEach { coordinate ->
      if (coordinate !in currentCoordinates) {
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

    // Ensure variant directory exists
    val licensesFile = resolveLicensesFile()
    licensesFile.parentFile?.mkdirs()

    // Write updated file
    licensesFile.writeText(LicenseCatalogParser().serialize(catalog))

    logger.lifecycle(
      "Sync completed: $addedCount added, $removedCount removed",
    )

    // Warn about ambiguous licenses for newly added artifacts
    if (ambiguousLicenseArtifacts.isNotEmpty()) {
      logger.warn("")
      logger.warn("WARNING: Found ${ambiguousLicenseArtifacts.size} new artifact(s) with ambiguous license names.")
      logger.warn("Please verify these licenses manually and update scribe-licenses.yml if needed:")
      ambiguousLicenseArtifacts.forEach { info ->
        logger.warn("  - ${info.coordinate}: \"${info.licenseName}\" (${info.licenseUrl ?: "no URL"})")
      }
      logger.warn("")
    }
  }
}
