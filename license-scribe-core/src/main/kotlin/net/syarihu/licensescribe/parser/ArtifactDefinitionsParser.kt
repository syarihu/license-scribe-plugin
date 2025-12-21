package net.syarihu.licensescribe.parser

import net.syarihu.licensescribe.model.ArtifactDefinition
import net.syarihu.licensescribe.model.ArtifactGroup
import net.syarihu.licensescribe.model.ScopedArtifacts
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader

/**
 * Parser for artifact-definitions.yml files.
 *
 * Expected format:
 * ```yaml
 * implementation:
 *   com.squareup.okhttp3:
 *     - name: okhttp
 *       url: https://github.com/square/okhttp
 *       copyrightHolder: Square, Inc.
 *       license: apache-2.0
 *   com.google.code.gson:
 *     - name: gson
 *       copyrightHolder: Google Inc.
 *       license: apache-2.0
 * ```
 */
class ArtifactDefinitionsParser {
  private val yaml = Yaml()

  fun parse(file: File): List<ScopedArtifacts> {
    if (!file.exists()) {
      return emptyList()
    }
    return file.reader().use { parse(it) }
  }

  fun parse(reader: Reader): List<ScopedArtifacts> {
    val data: Map<String, Any> = yaml.load(reader) ?: return emptyList()

    return data.map { (scope, scopeData) ->
      parseScope(scope, scopeData)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseScope(
    scope: String,
    scopeData: Any,
  ): ScopedArtifacts {
    val groups =
      when (scopeData) {
        is Map<*, *> -> {
          val groupsMap = scopeData as Map<String, Any>
          groupsMap.map { (groupId, groupData) ->
            parseGroup(groupId, groupData)
          }
        }
        else -> emptyList()
      }
    return ScopedArtifacts(scope = scope, groups = groups)
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseGroup(
    groupId: String,
    groupData: Any,
  ): ArtifactGroup {
    val artifacts =
      when (groupData) {
        is List<*> -> {
          (groupData as List<Map<String, Any>>).map { parseArtifact(it) }
        }
        else -> emptyList()
      }
    return ArtifactGroup(groupId = groupId, artifacts = artifacts)
  }

  private fun parseArtifact(data: Map<String, Any>): ArtifactDefinition = ArtifactDefinition(
    name = data["name"]?.toString() ?: "",
    url = data["url"]?.toString(),
    copyrightHolder = data["copyrightHolder"]?.toString(),
    license = data["license"]?.toString() ?: "",
  )

  fun serialize(scopedArtifacts: List<ScopedArtifacts>): String {
    val options =
      DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
      }
    val yaml = Yaml(options)

    val data =
      scopedArtifacts.associate { scopedArtifact ->
        scopedArtifact.scope to
          scopedArtifact.groups.associate { group ->
            group.groupId to
              group.artifacts.map { artifact ->
                buildMap {
                  put("name", artifact.name)
                  artifact.url?.let { put("url", it) }
                  artifact.copyrightHolder?.let { put("copyrightHolder", it) }
                  put("license", artifact.license)
                }
              }
          }
      }
    return yaml.dump(data)
  }
}
