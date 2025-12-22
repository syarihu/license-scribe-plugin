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
 * Task to sync license records with current dependencies.
 * Adds new dependencies and removes ones no longer present.
 */
abstract class SyncLicensesTask : BaseLicenseTask() {
  @TaskAction
  fun execute() {
    val existingRecords = loadRecords()
    val existingCatalog = loadCatalog()
    val ignoreRules = loadIgnoreRules()

    // Build lookup map of existing records
    val existingMap =
      existingRecords
        .flatMap { scoped ->
          scoped.groups.flatMap { group ->
            group.records.map { record ->
              "${group.groupId}:${record.name}" to (scoped.scope to record)
            }
          }
        }.toMap()

    // Get current dependencies
    val currentDependencies =
      resolveDependencies()
        .filterNot { ignoreRules.shouldIgnore(it) }

    // Sync licenses
    val newLicenses = mutableMapOf<String, License>()
    existingCatalog.licenses.forEach { (key, license) ->
      newLicenses[key] = license
    }

    val recordsByScope = mutableMapOf<String, MutableMap<String, MutableList<Record>>>()

    // First, add all existing records
    existingRecords.forEach { scoped ->
      val scopeMap = recordsByScope.getOrPut(scoped.scope) { mutableMapOf() }
      scoped.groups.forEach { group ->
        val groupList = scopeMap.getOrPut(group.groupId) { mutableListOf() }
        groupList.addAll(group.records)
      }
    }

    // Then, add new dependencies
    var addedCount = 0

    currentDependencies.forEach { artifact ->
      val coord = "${artifact.group}:${artifact.name}"
      val existing = existingMap[coord]

      val scope = existing?.first ?: "implementation"
      val scopeMap = recordsByScope.getOrPut(scope) { mutableMapOf() }
      val groupList = scopeMap.getOrPut(artifact.group) { mutableListOf() }

      if (existing == null) {
        // Add new record
        val pomInfo = resolvePomInfo(artifact)
        val licenseKey =
          pomInfo?.licenses?.firstOrNull()?.let { pomLicense ->
            val key = normalizeLicenseKey(pomLicense.name)
            if (!newLicenses.containsKey(key)) {
              newLicenses[key] =
                License(
                  key = key,
                  name = pomLicense.name,
                  url = pomLicense.url,
                )
            }
            key
          } ?: "unknown"

        groupList.add(
          Record(
            name = artifact.name,
            url = pomInfo?.url?.let { stripVersionFromUrl(it) },
            copyrightHolder = pomInfo?.developers?.firstOrNull(),
            license = licenseKey,
          ),
        )
        addedCount++
      }
    }

    // Remove records no longer in dependencies
    val currentCoords = currentDependencies.map { "${it.group}:${it.name}" }.toSet()
    var removedCount = 0

    recordsByScope.forEach { (_, scopeMap) ->
      scopeMap.forEach { (groupId, records) ->
        val toRemove = records.filter { "$groupId:${it.name}" !in currentCoords }
        records.removeAll(toRemove)
        removedCount += toRemove.size
      }
    }

    // Clean up empty groups and scopes
    recordsByScope.forEach { (_, scopeMap) ->
      scopeMap.entries.removeIf { it.value.isEmpty() }
    }
    recordsByScope.entries.removeIf { it.value.isEmpty() }

    // Convert back to ScopedRecords
    val syncedRecords =
      recordsByScope
        .map { (scope, scopeMap) ->
          ScopedRecords(
            scope = scope,
            groups =
            scopeMap
              .map { (groupId, records) ->
                RecordGroup(groupId = groupId, records = records.sortedBy { it.name })
              }.sortedBy { it.groupId },
          )
        }.sortedBy { it.scope }

    // Write updated files
    val recordsFile = resolveRecordsFile()
    recordsFile.writeText(RecordsParser().serialize(syncedRecords))

    val catalogFile = resolveCatalogFile()
    catalogFile.writeText(
      CatalogParser().serialize(Catalog(newLicenses)),
    )

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
}
