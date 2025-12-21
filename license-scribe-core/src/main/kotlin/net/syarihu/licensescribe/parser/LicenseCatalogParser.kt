package net.syarihu.licensescribe.parser

import net.syarihu.licensescribe.model.License
import net.syarihu.licensescribe.model.LicenseCatalog
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.Reader

/**
 * Parser for license-catalog.yml files.
 *
 * Expected format:
 * ```yaml
 * apache-2.0:
 *   name: Apache License 2.0
 *   url: https://www.apache.org/licenses/LICENSE-2.0
 * mit:
 *   name: MIT License
 *   url: https://opensource.org/licenses/MIT
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

  fun parse(reader: Reader): LicenseCatalog {
    val data: Map<String, Any> = yaml.load(reader) ?: return LicenseCatalog.EMPTY

    val licenses =
      data
        .mapNotNull { (key, value) ->
          parseLicense(key, value)?.let { key to it }
        }.toMap()

    return LicenseCatalog(licenses)
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseLicense(
    key: String,
    value: Any,
  ): License? = when (value) {
    is Map<*, *> -> {
      val map = value as Map<String, Any>
      License(
        key = key,
        name = map["name"]?.toString() ?: key,
        url = map["url"]?.toString(),
      )
    }
    is String -> License(key = key, name = value)
    else -> null
  }

  fun serialize(catalog: LicenseCatalog): String {
    val data =
      catalog.licenses.mapValues { (_, license) ->
        buildMap {
          put("name", license.name)
          license.url?.let { put("url", it) }
        }
      }
    return yaml.dump(data)
  }
}
