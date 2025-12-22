package net.syarihu.licensescribe.parser

import net.syarihu.licensescribe.model.Record
import net.syarihu.licensescribe.model.RecordGroup
import net.syarihu.licensescribe.model.ScopedRecords
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader

/**
 * Parser for scribe-records.yml files.
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
class RecordsParser {
  private val yaml = Yaml()

  fun parse(file: File): List<ScopedRecords> {
    if (!file.exists()) {
      return emptyList()
    }
    return file.reader().use { parse(it) }
  }

  fun parse(reader: Reader): List<ScopedRecords> {
    val data: Map<String, Any> = yaml.load(reader) ?: return emptyList()

    return data.map { (scope, scopeData) ->
      parseScope(scope, scopeData)
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseScope(
    scope: String,
    scopeData: Any,
  ): ScopedRecords {
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
    return ScopedRecords(scope = scope, groups = groups)
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseGroup(
    groupId: String,
    groupData: Any,
  ): RecordGroup {
    val records =
      when (groupData) {
        is List<*> -> {
          (groupData as List<Map<String, Any>>).map { parseRecord(it) }
        }
        else -> emptyList()
      }
    return RecordGroup(groupId = groupId, records = records)
  }

  private fun parseRecord(data: Map<String, Any>): Record = Record(
    name = data["name"]?.toString() ?: "",
    url = data["url"]?.toString(),
    copyrightHolder = data["copyrightHolder"]?.toString(),
    license = data["license"]?.toString() ?: "",
  )

  fun serialize(scopedRecords: List<ScopedRecords>): String {
    val options =
      DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
      }
    val yaml = Yaml(options)

    val data =
      scopedRecords.associate { scopedRecord ->
        scopedRecord.scope to
          scopedRecord.groups.associate { group ->
            group.groupId to
              group.records.map { record ->
                buildMap {
                  put("name", record.name)
                  record.url?.let { put("url", it) }
                  record.copyrightHolder?.let { put("copyrightHolder", it) }
                  put("license", record.license)
                }
              }
          }
      }
    return yaml.dump(data)
  }
}
