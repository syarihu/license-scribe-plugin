package net.syarihu.licensescribe.gradle.task

import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Task to check license definitions for correctness.
 * Combines validation against current dependencies and inspection of definition quality.
 */
abstract class CheckLicensesTask : BaseLicenseTask() {
  @TaskAction
  fun execute() {
    val definitions = loadArtifactDefinitions()
    val catalog = loadLicenseCatalog()
    val ignoreRules = loadIgnoreRules()

    val issues = mutableListOf<String>()

    // === Inspection: Check definition quality ===
    definitions.forEach { scopedArtifacts ->
      scopedArtifacts.groups.forEach { group ->
        group.artifacts.forEach { artifact ->
          val prefix = "${group.groupId}:${artifact.name}"

          // Check for missing name
          if (artifact.name.isBlank()) {
            issues.add("$prefix: Missing artifact name")
          }

          // Check for missing license
          if (artifact.license.isBlank()) {
            issues.add("$prefix: Missing license key")
          } else if (!catalog.containsKey(artifact.license)) {
            issues.add("$prefix: Unknown license key '${artifact.license}' not in catalog")
          }

          // Check for missing URL (warning)
          if (artifact.url.isNullOrBlank()) {
            logger.warn("$prefix: Missing URL (optional)")
          }

          // Check for missing copyright holder (warning)
          if (artifact.copyrightHolder.isNullOrBlank()) {
            logger.warn("$prefix: Missing copyright holder (optional)")
          }
        }
      }
    }

    // Check for unused licenses in catalog
    val usedLicenses =
      definitions
        .flatMap { scoped ->
          scoped.groups.flatMap { group ->
            group.artifacts.map { it.license }
          }
        }.toSet()

    catalog.licenses.keys.forEach { key ->
      if (key !in usedLicenses && key != "unknown") {
        logger.warn("License '$key' is defined but not used by any artifact")
      }
    }

    // === Validation: Check sync with dependencies ===
    val currentDependencies =
      resolveDependencies()
        .filterNot { ignoreRules.shouldIgnore(it) }
        .map { "${it.group}:${it.name}" }
        .toSet()

    val definedArtifacts =
      definitions
        .flatMap { scoped ->
          scoped.groups.flatMap { group ->
            group.artifacts.map { "${group.groupId}:${it.name}" }
          }
        }.toSet()

    val missingInDefinitions = currentDependencies - definedArtifacts
    val extraInDefinitions = definedArtifacts - currentDependencies

    if (missingInDefinitions.isNotEmpty()) {
      logger.error("Missing in definitions (${missingInDefinitions.size}):")
      missingInDefinitions.sorted().forEach { logger.error("  - $it") }
      issues.add("${missingInDefinitions.size} dependencies not in definitions")
    }

    if (extraInDefinitions.isNotEmpty()) {
      logger.error("Extra in definitions (no longer in dependencies) (${extraInDefinitions.size}):")
      extraInDefinitions.sorted().forEach { logger.error("  - $it") }
      issues.add("${extraInDefinitions.size} definitions not in dependencies")
    }

    // === Report results ===
    if (issues.isNotEmpty()) {
      issues.forEach { logger.error(it) }
      throw GradleException(
        "License check failed with ${issues.size} issue(s). " +
          "Run mergeLicenseList to update definitions.",
      )
    }

    logger.lifecycle("License check passed: ${definedArtifacts.size} artifacts defined, ${currentDependencies.size} dependencies")
  }
}
