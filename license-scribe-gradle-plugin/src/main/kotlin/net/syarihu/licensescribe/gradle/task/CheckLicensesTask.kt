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
    val records = loadRecords()
    val catalog = loadCatalog()
    val ignoreRules = loadIgnoreRules()

    val issues = mutableListOf<String>()

    // === Inspection: Check record quality ===
    records.forEach { scopedRecords ->
      scopedRecords.groups.forEach { group ->
        group.records.forEach { record ->
          val prefix = "${group.groupId}:${record.name}"

          // Check for missing name
          if (record.name.isBlank()) {
            issues.add("$prefix: Missing record name")
          }

          // Check for missing license
          if (record.license.isBlank()) {
            issues.add("$prefix: Missing license key")
          } else if (!catalog.containsKey(record.license)) {
            issues.add("$prefix: Unknown license key '${record.license}' not in catalog")
          }

          // Check for missing URL (warning)
          if (record.url.isNullOrBlank()) {
            logger.warn("$prefix: Missing URL (optional)")
          }

          // Check for missing copyright holder (warning)
          if (record.copyrightHolder.isNullOrBlank()) {
            logger.warn("$prefix: Missing copyright holder (optional)")
          }
        }
      }
    }

    // Check for unused licenses in catalog
    val usedLicenses =
      records
        .flatMap { scoped ->
          scoped.groups.flatMap { group ->
            group.records.map { it.license }
          }
        }.toSet()

    catalog.licenses.keys.forEach { key ->
      if (key !in usedLicenses && key != "unknown") {
        logger.warn("License '$key' is defined but not used by any record")
      }
    }

    // === Validation: Check sync with dependencies ===
    val currentDependencies =
      resolveDependencies()
        .filterNot { ignoreRules.shouldIgnore(it) }
        .map { "${it.group}:${it.name}" }
        .toSet()

    val recordedItems =
      records
        .flatMap { scoped ->
          scoped.groups.flatMap { group ->
            group.records.map { "${group.groupId}:${it.name}" }
          }
        }.toSet()

    val missingInRecords = currentDependencies - recordedItems
    val extraInRecords = recordedItems - currentDependencies

    if (missingInRecords.isNotEmpty()) {
      logger.error("Missing in records (${missingInRecords.size}):")
      missingInRecords.sorted().forEach { logger.error("  - $it") }
      issues.add("${missingInRecords.size} dependencies not in records")
    }

    if (extraInRecords.isNotEmpty()) {
      logger.error("Extra in records (no longer in dependencies) (${extraInRecords.size}):")
      extraInRecords.sorted().forEach { logger.error("  - $it") }
      issues.add("${extraInRecords.size} records not in dependencies")
    }

    // === Report results ===
    if (issues.isNotEmpty()) {
      issues.forEach { logger.error(it) }
      throw GradleException(
        "License check failed with ${issues.size} issue(s). " +
          "Run mergeLicenseList to update definitions.",
      )
    }

    logger.lifecycle("License check passed: ${recordedItems.size} records, ${currentDependencies.size} dependencies")
  }
}
