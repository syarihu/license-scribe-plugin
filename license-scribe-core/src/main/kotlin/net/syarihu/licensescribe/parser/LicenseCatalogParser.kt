package net.syarihu.licensescribe.parser

import net.syarihu.licensescribe.model.ArtifactEntry
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.model.LicenseEntry
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader

/**
 * Parser for scribe-licenses.yml files.
 *
 * Expected format:
 * ```yaml
 * licenses:
 *   apache-2.0:
 *     name: Apache License 2.0
 *     url: https://www.apache.org/licenses/LICENSE-2.0
 *     artifacts:
 *       com.squareup.okhttp3:
 *         - name: okhttp
 *           url: https://square.github.io/okhttp/
 *           copyrightHolders: [Square, Inc.]
 *         - name: okhttp-android
 *           url: https://square.github.io/okhttp/
 *           copyrightHolders: [Square, Inc.]
 *
 *   mit:
 *     name: MIT License
 *     url: https://opensource.org/licenses/MIT
 *     artifacts:
 *       some.library:
 *         - name: dual-licensed-lib
 *           copyrightHolders: [Someone]
 *           alternativeLicenses: [apache-2.0]
 * ```
 */
class LicenseCatalogParser {
  private val yaml = Yaml()

  fun parse(file: File): LicenseCatalog {
    if (!file.exists()) {
      return LicenseCatalog.EMPTY
    }
    return file.reader().use { parse(it) }
  }

  @Suppress("UNCHECKED_CAST")
  fun parse(reader: Reader): LicenseCatalog {
    val data: Map<String, Any> = yaml.load(reader) ?: return LicenseCatalog.EMPTY

    val licensesData = data["licenses"] as? Map<String, Any> ?: return LicenseCatalog.EMPTY

    val licenses = licensesData.mapNotNull { (key, value) ->
      parseLicenseEntry(key, value)?.let { key to it }
    }.toMap()

    return LicenseCatalog(licenses)
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseLicenseEntry(
    key: String,
    value: Any,
  ): LicenseEntry? {
    if (value !is Map<*, *>) return null

    val map = value as Map<String, Any>
    val name = map["name"]?.toString() ?: key
    val url = map["url"]?.toString()
    val artifactsData = map["artifacts"] as? Map<String, Any>

    val artifacts = artifactsData?.mapNotNull { (groupId, groupData) ->
      parseArtifactGroup(groupId, groupData)?.let { groupId to it }
    }?.toMap() ?: emptyMap()

    return LicenseEntry(
      name = name,
      url = url,
      artifacts = artifacts,
    )
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseArtifactGroup(
    groupId: String,
    groupData: Any,
  ): List<ArtifactEntry>? {
    if (groupData !is List<*>) return null

    return (groupData as List<Map<String, Any>>).mapNotNull { parseArtifactEntry(it) }
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseArtifactEntry(data: Map<String, Any>): ArtifactEntry? {
    val name = data["name"]?.toString() ?: return null
    val url = data["url"]?.toString()

    val copyrightHolders = when (val holders = data["copyrightHolders"]) {
      is List<*> -> holders.mapNotNull { it?.toString() }
      is String -> listOf(holders)
      else -> emptyList()
    }

    val alternativeLicenses = when (val alt = data["alternativeLicenses"]) {
      is List<*> -> alt.mapNotNull { it?.toString() }
      else -> null
    }

    val additionalLicenses = when (val add = data["additionalLicenses"]) {
      is List<*> -> add.mapNotNull { it?.toString() }
      else -> null
    }

    return ArtifactEntry(
      name = name,
      url = url,
      copyrightHolders = copyrightHolders,
      alternativeLicenses = alternativeLicenses,
      additionalLicenses = additionalLicenses,
    )
  }

  fun serialize(catalog: LicenseCatalog): String {
    val options = DumperOptions().apply {
      defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
      isPrettyFlow = true
    }
    val yaml = Yaml(options)

    val licensesData = catalog.licenses.mapValues { (_, licenseEntry) ->
      buildMap {
        put("name", licenseEntry.name)
        licenseEntry.url?.let { put("url", it) }

        if (licenseEntry.artifacts.isNotEmpty()) {
          put(
            "artifacts",
            licenseEntry.artifacts.mapValues { (_, artifacts) ->
              artifacts.map { artifact ->
                buildMap {
                  put("name", artifact.name)
                  artifact.url?.let { put("url", it) }
                  if (artifact.copyrightHolders.isNotEmpty()) {
                    put("copyrightHolders", artifact.copyrightHolders)
                  }
                  artifact.alternativeLicenses?.let { put("alternativeLicenses", it) }
                  artifact.additionalLicenses?.let { put("additionalLicenses", it) }
                }
              }
            },
          )
        }
      }
    }

    val data = mapOf("licenses" to licensesData)
    return yaml.dump(data)
  }
}
