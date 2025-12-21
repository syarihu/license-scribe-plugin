package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.model.ArtifactDefinition
import net.syarihu.licensescribe.model.ArtifactGroup
import net.syarihu.licensescribe.model.License
import net.syarihu.licensescribe.model.ScopedArtifacts
import net.syarihu.licensescribe.parser.ArtifactDefinitionsParser
import net.syarihu.licensescribe.parser.LicenseCatalogParser
import org.gradle.api.tasks.TaskAction

/**
 * Task to sync license definitions with current dependencies.
 * Adds new dependencies and removes ones no longer present.
 */
abstract class SyncLicensesTask : BaseLicenseTask() {
  @TaskAction
  fun execute() {
    val existingDefinitions = loadArtifactDefinitions()
    val existingCatalog = loadLicenseCatalog()
    val ignoreRules = loadIgnoreRules()

    // Build lookup map of existing definitions
    val existingMap =
      existingDefinitions
        .flatMap { scoped ->
          scoped.groups.flatMap { group ->
            group.artifacts.map { artifact ->
              "${group.groupId}:${artifact.name}" to (scoped.scope to artifact)
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

    val artifactsByScope = mutableMapOf<String, MutableMap<String, MutableList<ArtifactDefinition>>>()

    // First, add all existing definitions
    existingDefinitions.forEach { scoped ->
      val scopeMap = artifactsByScope.getOrPut(scoped.scope) { mutableMapOf() }
      scoped.groups.forEach { group ->
        val groupList = scopeMap.getOrPut(group.groupId) { mutableListOf() }
        groupList.addAll(group.artifacts)
      }
    }

    // Then, add new dependencies
    var addedCount = 0

    currentDependencies.forEach { artifact ->
      val coord = "${artifact.group}:${artifact.name}"
      val existing = existingMap[coord]

      val scope = existing?.first ?: "implementation"
      val scopeMap = artifactsByScope.getOrPut(scope) { mutableMapOf() }
      val groupList = scopeMap.getOrPut(artifact.group) { mutableListOf() }

      if (existing == null) {
        // Add new artifact
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
          ArtifactDefinition(
            name = artifact.name,
            url = pomInfo?.url?.let { stripVersionFromUrl(it) },
            copyrightHolder = pomInfo?.developers?.firstOrNull(),
            license = licenseKey,
          ),
        )
        addedCount++
      }
    }

    // Remove artifacts no longer in dependencies
    val currentCoords = currentDependencies.map { "${it.group}:${it.name}" }.toSet()
    var removedCount = 0

    artifactsByScope.forEach { (_, scopeMap) ->
      scopeMap.forEach { (groupId, artifacts) ->
        val toRemove = artifacts.filter { "$groupId:${it.name}" !in currentCoords }
        artifacts.removeAll(toRemove)
        removedCount += toRemove.size
      }
    }

    // Clean up empty groups and scopes
    artifactsByScope.forEach { (_, scopeMap) ->
      scopeMap.entries.removeIf { it.value.isEmpty() }
    }
    artifactsByScope.entries.removeIf { it.value.isEmpty() }

    // Convert back to ScopedArtifacts
    val syncedDefinitions =
      artifactsByScope
        .map { (scope, scopeMap) ->
          ScopedArtifacts(
            scope = scope,
            groups =
            scopeMap
              .map { (groupId, artifacts) ->
                ArtifactGroup(groupId = groupId, artifacts = artifacts.sortedBy { it.name })
              }.sortedBy { it.groupId },
          )
        }.sortedBy { it.scope }

    // Write updated files
    val artifactDefinitionsFile = resolveArtifactDefinitionsFile()
    artifactDefinitionsFile.writeText(ArtifactDefinitionsParser().serialize(syncedDefinitions))

    val licenseCatalogFile = resolveLicenseCatalogFile()
    licenseCatalogFile.writeText(
      LicenseCatalogParser().serialize(
        net.syarihu.licensescribe.model
          .LicenseCatalog(newLicenses),
      ),
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
