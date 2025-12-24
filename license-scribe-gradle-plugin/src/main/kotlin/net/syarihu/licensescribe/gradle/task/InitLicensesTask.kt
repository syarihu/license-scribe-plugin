package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.gradle.task.model.AmbiguousLicenseInfo
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
    val ambiguousLicenseArtifacts = mutableListOf<AmbiguousLicenseInfo>()

    dependencies.forEach { artifact ->
      val pomInfo = resolvePomInfo(artifact)

      // Determine license from POM
      val pomLicense = pomInfo?.licenses?.firstOrNull()
      val licenseKey = pomLicense?.let {
        val key = normalizeLicenseKey(it.name, it.url)
        if (!licenseInfoMap.containsKey(key)) {
          licenseInfoMap[key] = it.name to it.url
        }
        key
      } ?: "unknown"

      // Check for ambiguous license
      if (pomLicense != null && isAmbiguousLicense(pomLicense.name, pomLicense.url)) {
        ambiguousLicenseArtifacts.add(
          AmbiguousLicenseInfo(
            coordinate = "${artifact.group}:${artifact.name}",
            licenseName = pomLicense.name,
            licenseUrl = pomLicense.url,
          ),
        )
      }

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

    // Warn about ambiguous licenses
    if (ambiguousLicenseArtifacts.isNotEmpty()) {
      logger.warn("")
      logger.warn("WARNING: Found ${ambiguousLicenseArtifacts.size} artifact(s) with ambiguous license names.")
      logger.warn("Please verify these licenses manually and update scribe-licenses.yml if needed:")
      ambiguousLicenseArtifacts.forEach { info ->
        logger.warn("  - ${info.coordinate}: \"${info.licenseName}\" (${info.licenseUrl ?: "no URL"})")
      }
      logger.warn("")
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

    // Ensure variant directory exists
    licensesFile.parentFile?.mkdirs()

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
}
