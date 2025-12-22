package net.syarihu.licensescribe.gradle.task

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Task to check license records for correctness.
 * Combines validation against current dependencies and inspection of record quality.
 */
abstract class CheckLicensesTask : BaseLicenseTask() {
  @TaskAction
  fun execute() {
    val catalog = loadLicenseCatalog()
    val ignoreRules = loadIgnoreRules()

    val issues = mutableListOf<String>()
    val licenseKeys = catalog.licenses.keys

    // === Inspection: Check artifact quality ===
    catalog.licenses.forEach { (licenseKey, licenseEntry) ->
      // Check license entry
      if (licenseEntry.name.isBlank()) {
        issues.add("License '$licenseKey': Missing license name")
      }

      if (licenseEntry.url.isNullOrBlank()) {
        logger.warn("License '$licenseKey': Missing URL (optional)")
      }

      licenseEntry.artifacts.forEach { (groupId, artifacts) ->
        artifacts.forEach { artifact ->
          val prefix = "$groupId:${artifact.name}"

          // Check for missing artifact name
          if (artifact.name.isBlank()) {
            issues.add("$prefix: Missing artifact name")
          }

          // Check for missing URL (warning)
          if (artifact.url.isNullOrBlank()) {
            logger.warn("$prefix: Missing URL (optional)")
          }

          // Check for missing copyright holders (warning)
          if (artifact.copyrightHolders.isEmpty()) {
            logger.warn("$prefix: Missing copyright holders (optional)")
          }

          // Check alternativeLicenses reference validity
          artifact.alternativeLicenses?.forEach { altLicense ->
            if (altLicense !in licenseKeys) {
              issues.add("$prefix: alternativeLicense '$altLicense' not found in licenses")
            }
          }

          // Check additionalLicenses reference validity
          artifact.additionalLicenses?.forEach { addLicense ->
            if (addLicense !in licenseKeys) {
              issues.add("$prefix: additionalLicense '$addLicense' not found in licenses")
            }
          }
        }
      }
    }

    // Check for empty licenses (warning)
    catalog.licenses.forEach { (key, entry) ->
      if (entry.artifacts.isEmpty() && key != "unknown") {
        logger.warn("License '$key' has no artifacts")
      }
    }

    // === Validation: Check sync with dependencies ===
    val currentDependencies =
      resolveDependencies()
        .filterNot { ignoreRules.shouldIgnore(it) }
        .map { "${it.group}:${it.name}" }
        .toSet()

    val catalogArtifacts = catalog.getAllArtifactIds()

    val missingInCatalog = currentDependencies - catalogArtifacts
    val extraInCatalog = catalogArtifacts - currentDependencies

    if (missingInCatalog.isNotEmpty()) {
      logger.error("Missing in catalog (${missingInCatalog.size}):")
      missingInCatalog.sorted().forEach { logger.error("  - $it") }
      issues.add("${missingInCatalog.size} dependencies not in catalog")
    }

    if (extraInCatalog.isNotEmpty()) {
      logger.error("Extra in catalog (no longer in dependencies) (${extraInCatalog.size}):")
      extraInCatalog.sorted().forEach { logger.error("  - $it") }
      issues.add("${extraInCatalog.size} catalog entries not in dependencies")
    }

    // === Report results ===
    if (issues.isNotEmpty()) {
      issues.forEach { logger.error(it) }
      throw GradleException(
        "License check failed with ${issues.size} issue(s). " +
          "Run syncLicenses to update definitions.",
      )
    }

    logger.lifecycle("License check passed: ${catalogArtifacts.size} artifacts, ${currentDependencies.size} dependencies")
  }
}
