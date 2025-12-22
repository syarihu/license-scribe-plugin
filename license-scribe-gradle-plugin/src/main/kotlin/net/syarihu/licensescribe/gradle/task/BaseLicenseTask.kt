package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.gradle.LicenseScribeExtension
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.IgnoreRules
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.parser.IgnoreRulesParser
import net.syarihu.licensescribe.parser.LicenseCatalogParser
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.io.File
import java.io.Serializable

/**
 * Base class for license-related tasks.
 */
abstract class BaseLicenseTask : DefaultTask() {
  @get:Internal
  abstract val baseDir: DirectoryProperty

  @get:Input
  abstract val licensesFileName: Property<String>

  @get:Input
  abstract val ignoreFileName: Property<String>

  /**
   * Variant name for Android projects (empty for non-Android projects)
   */
  @get:Input
  abstract val variantName: Property<String>

  /**
   * Resolved dependencies as serializable strings "group:name:version"
   */
  @get:Input
  abstract val resolvedDependencies: ListProperty<String>

  /**
   * POM info map: key is "group:name", value is serialized PomInfo
   */
  @get:Input
  abstract val pomInfoMap: MapProperty<String, SerializablePomInfo>

  fun configureWith(
    extension: LicenseScribeExtension,
    configuration: Configuration?,
    variant: String = "",
  ) {
    this.baseDir.set(extension.baseDir)
    this.licensesFileName.set(extension.licensesFile)
    this.ignoreFileName.set(extension.ignoreFile)
    this.variantName.set(variant)

    // Use lazy evaluation to defer dependency resolution until task execution
    // This ensures compatibility with configure-on-demand mode
    if (configuration != null) {
      this.resolvedDependencies.set(
        project.provider {
          resolveDependenciesFromConfiguration(configuration)
            .map { "${it.group}:${it.name}:${it.version}" }
        },
      )

      this.pomInfoMap.set(
        project.provider {
          val deps = resolveDependenciesFromConfiguration(configuration)
          val pomMap = mutableMapOf<String, SerializablePomInfo>()
          deps.forEach { artifactId ->
            resolvePomInfoFromConfiguration(configuration, artifactId)?.let { pomInfo ->
              pomMap["${artifactId.group}:${artifactId.name}"] = pomInfo.toSerializable()
            }
          }
          pomMap
        },
      )
    } else {
      this.resolvedDependencies.set(emptyList())
      this.pomInfoMap.set(emptyMap())
    }
  }

  /**
   * Returns the variant-specific directory.
   * For Android projects: baseDir/variant/ (e.g., licenses/debug/, licenses/release/)
   * For non-Android projects: baseDir/
   */
  protected fun resolveVariantDir(): File {
    val base = baseDir.get().asFile
    val variant = variantName.get()
    return if (variant.isNotEmpty()) {
      File(base, variant)
    } else {
      base
    }
  }

  protected fun resolveLicensesFile(): File = File(resolveVariantDir(), licensesFileName.get())

  protected fun resolveIgnoreFile(): File = File(resolveVariantDir(), ignoreFileName.get())

  protected fun loadLicenseCatalog(): LicenseCatalog = LicenseCatalogParser().parse(resolveLicensesFile())

  protected fun loadIgnoreRules(): IgnoreRules = IgnoreRulesParser().parse(resolveIgnoreFile())

  /**
   * Get resolved dependencies (from configuration time)
   */
  protected fun resolveDependencies(): List<ArtifactId> = resolvedDependencies.get().map { coord ->
    val parts = coord.split(":")
    ArtifactId(
      group = parts[0],
      name = parts[1],
      version = parts.getOrElse(2) { "" },
    )
  }

  /**
   * Get POM info for an artifact (from configuration time)
   */
  protected fun resolvePomInfo(artifactId: ArtifactId): PomInfo? {
    val key = "${artifactId.group}:${artifactId.name}"
    return pomInfoMap.get()[key]?.toPomInfo()
  }

  /**
   * Resolve dependencies from configuration at configuration time
   */
  private fun resolveDependenciesFromConfiguration(config: Configuration): List<ArtifactId> = try {
    config.incoming.resolutionResult.allComponents
      .mapNotNull { component ->
        val id = component.id
        if (id is ModuleComponentIdentifier) {
          ArtifactId(
            group = id.group,
            name = id.module,
            version = id.version,
          )
        } else {
          null
        }
      }.filter { it.group.isNotBlank() && it.name.isNotBlank() }
      .distinctBy { "${it.group}:${it.name}" }
  } catch (e: Exception) {
    logger.warn("Failed to resolve dependencies: ${e.message}")
    emptyList()
  }

  /**
   * Resolve POM info at configuration time, including parent POM resolution
   */
  private fun resolvePomInfoFromConfiguration(
    config: Configuration,
    artifactId: ArtifactId,
  ): PomInfo? = try {
    val version = artifactId.version
    if (version.isNullOrBlank()) {
      logger.debug("Cannot resolve POM for ${artifactId.coordinate}: version is missing")
      return null
    }
    resolvePomInfoRecursive(artifactId.group, artifactId.name, version, maxDepth = 5)
  } catch (e: Exception) {
    logger.debug("Failed to resolve POM for ${artifactId.coordinate}: ${e.message}")
    null
  }

  /**
   * Recursively resolve POM info, following parent POMs if needed
   */
  private fun resolvePomInfoRecursive(
    groupId: String,
    artifactId: String,
    version: String,
    maxDepth: Int,
  ): PomInfo? {
    if (maxDepth <= 0) {
      logger.debug("Max depth reached while resolving parent POM for $groupId:$artifactId:$version")
      return null
    }

    val pomFile = resolvePomFile(groupId, artifactId, version) ?: return null
    val pomData = parsePomWithParentInfo(pomFile) ?: return null

    // If we have licenses, no need to check parent
    if (pomData.pomInfo.licenses.isNotEmpty()) {
      return pomData.pomInfo
    }

    // Try to get info from parent POM
    val parentInfo = pomData.parentInfo
    if (parentInfo != null) {
      val parentPomInfo = resolvePomInfoRecursive(
        parentInfo.groupId,
        parentInfo.artifactId,
        parentInfo.version,
        maxDepth - 1,
      )

      if (parentPomInfo != null) {
        // Merge: child values take precedence, but use parent values for missing fields
        return PomInfo(
          name = pomData.pomInfo.name ?: parentPomInfo.name,
          url = pomData.pomInfo.url ?: parentPomInfo.url,
          licenses = pomData.pomInfo.licenses.ifEmpty { parentPomInfo.licenses },
          developers = pomData.pomInfo.developers.ifEmpty { parentPomInfo.developers },
        )
      }
    }

    return pomData.pomInfo
  }

  /**
   * Resolve POM file for a given artifact
   */
  private fun resolvePomFile(
    groupId: String,
    artifactId: String,
    version: String,
  ): File? = try {
    val componentId = org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier.newId(
      org.gradle.api.internal.artifacts.DefaultModuleIdentifier.newId(groupId, artifactId),
      version,
    )

    val result = project.dependencies
      .createArtifactResolutionQuery()
      .forComponents(componentId)
      .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
      .execute()

    result.resolvedComponents
      .flatMap { component ->
        component.getArtifacts(MavenPomArtifact::class.java)
      }.filterIsInstance<ResolvedArtifactResult>()
      .firstOrNull()
      ?.file
  } catch (e: Exception) {
    logger.debug("Failed to resolve POM file for $groupId:$artifactId:$version: ${e.message}")
    null
  }

  /**
   * Parse POM file and extract both POM info and parent info
   */
  private fun parsePomWithParentInfo(pomFile: File): PomDataWithParent? = try {
    val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(pomFile)
    doc.documentElement.normalize()

    val name = getTextContent(doc, "name")
    val url = getTextContent(doc, "url")

    // Parse licenses
    val licenses = try {
      val licensesNodes = doc.getElementsByTagName("licenses")
      if (licensesNodes.length > 0) {
        val licensesNode = licensesNodes.item(0)
        val licenseNodes = (licensesNode as? org.w3c.dom.Element)?.getElementsByTagName("license")
        (0 until (licenseNodes?.length ?: 0)).mapNotNull { i ->
          val licenseNode = licenseNodes?.item(i) as? org.w3c.dom.Element
          val licenseName = getChildTextContent(licenseNode, "name")
          val licenseUrl = getChildTextContent(licenseNode, "url")
          if (licenseName != null) {
            PomLicense(licenseName, licenseUrl)
          } else {
            null
          }
        }
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }

    // Parse developers
    val developers = try {
      val developersNodes = doc.getElementsByTagName("developers")
      if (developersNodes.length > 0) {
        val developersNode = developersNodes.item(0)
        val developerNodes = (developersNode as? org.w3c.dom.Element)?.getElementsByTagName("developer")
        (0 until (developerNodes?.length ?: 0)).mapNotNull { i ->
          val developerNode = developerNodes?.item(i) as? org.w3c.dom.Element
          getChildTextContent(developerNode, "name")
        }
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }

    // Parse parent info
    val parentInfo = try {
      val parentNodes = doc.getElementsByTagName("parent")
      if (parentNodes.length > 0) {
        val parentNode = parentNodes.item(0) as? org.w3c.dom.Element
        val parentGroupId = getChildTextContent(parentNode, "groupId")
        val parentArtifactId = getChildTextContent(parentNode, "artifactId")
        val parentVersion = getChildTextContent(parentNode, "version")
        if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
          ParentPomInfo(parentGroupId, parentArtifactId, parentVersion)
        } else {
          null
        }
      } else {
        null
      }
    } catch (e: Exception) {
      null
    }

    PomDataWithParent(
      pomInfo = PomInfo(
        name = name,
        url = url,
        licenses = licenses,
        developers = developers,
      ),
      parentInfo = parentInfo,
    )
  } catch (e: Exception) {
    logger.debug("Failed to parse POM: ${e.message}")
    null
  }

  private fun getTextContent(
    doc: org.w3c.dom.Document,
    tagName: String,
  ): String? {
    val nodes = doc.documentElement.childNodes
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.nodeName == tagName) {
        return node.textContent?.takeIf { it.isNotBlank() }
      }
    }
    return null
  }

  private fun getChildTextContent(
    element: org.w3c.dom.Element?,
    tagName: String,
  ): String? {
    if (element == null) return null
    val nodes = element.getElementsByTagName(tagName)
    return if (nodes.length > 0) {
      nodes.item(0).textContent?.takeIf { it.isNotBlank() }
    } else {
      null
    }
  }

  /**
   * Normalizes a license name and URL to a standard license key (e.g., "apache-2.0", "mit").
   * First tries to identify the license from the URL (more reliable), then from the name.
   */
  protected fun normalizeLicenseKey(
    licenseName: String,
    licenseUrl: String? = null,
  ): String {
    // First, try to identify license from URL (more reliable)
    licenseUrl?.let { url ->
      val urlLower = url.lowercase()
      when {
        urlLower.contains("apache.org/licenses/license-2.0") ||
          urlLower.contains("apache-2.0") -> return "apache-2.0"
        urlLower.contains("opensource.org/licenses/mit") ||
          urlLower.contains("mit-license") -> return "mit"
        urlLower.contains("opensource.org/licenses/bsd-3-clause") ||
          urlLower.contains("bsd-3-clause") -> return "bsd-3-clause"
        urlLower.contains("opensource.org/licenses/bsd-2-clause") ||
          urlLower.contains("bsd-2-clause") -> return "bsd-2-clause"
        urlLower.contains("gnu.org/licenses/lgpl-3") ||
          urlLower.contains("lgpl-3.0") -> return "lgpl-3.0"
        urlLower.contains("gnu.org/licenses/lgpl-2.1") ||
          urlLower.contains("lgpl-2.1") -> return "lgpl-2.1"
        urlLower.contains("gnu.org/licenses/gpl-3") ||
          urlLower.contains("gpl-3.0") -> return "gpl-3.0"
        urlLower.contains("gnu.org/licenses/gpl-2") ||
          urlLower.contains("gpl-2.0") -> return "gpl-2.0"
        urlLower.contains("eclipse.org/legal/epl") -> return "epl-1.0"
        urlLower.contains("mozilla.org") && urlLower.contains("mpl") -> return "mpl-2.0"
        urlLower.contains("creativecommons.org/publicdomain/zero") -> return "cc0-1.0"
        urlLower.contains("unlicense.org") -> return "unlicense"
        urlLower.contains("opensource.org/licenses/isc") -> return "isc"
      }
    }

    // Then, try to identify from license name
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
      lower.contains("creative commons") && lower.contains("zero") -> "cc0-1.0"
      lower.contains("unlicense") -> "unlicense"
      lower.contains("isc") -> "isc"
      // Proprietary licenses should be kept separate per vendor
      lower == "proprietary" -> {
        val vendorSuffix = extractVendorFromUrl(licenseUrl)
        if (vendorSuffix != null) "proprietary-$vendorSuffix" else "proprietary"
      }
      else ->
        licenseName
          .lowercase()
          .replace(Regex("[^a-z0-9]+"), "-")
          .trim('-')
    }
  }

  /**
   * Extracts a vendor identifier from a URL for creating unique license keys.
   * Returns the domain name or GitHub org/repo as identifier.
   */
  private fun extractVendorFromUrl(url: String?): String? {
    if (url == null) return null
    return try {
      val urlLower = url.lowercase()
      when {
        // GitHub: extract org name
        urlLower.contains("github.com") -> {
          val match = Regex("github\\.com/([^/]+)").find(urlLower)
          match?.groupValues?.get(1)
        }
        // GitLab: extract org name
        urlLower.contains("gitlab.com") -> {
          val match = Regex("gitlab\\.com/([^/]+)").find(urlLower)
          match?.groupValues?.get(1)
        }
        // Other URLs: extract domain without TLD
        else -> {
          val match = Regex("://(?:www\\.)?([^./]+)").find(urlLower)
          match?.groupValues?.get(1)
        }
      }
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Checks if a license name/URL combination is ambiguous and requires manual verification.
   * Returns true for licenses like "LICENSE", "LICENCE", or URLs pointing to repository LICENSE files.
   */
  protected fun isAmbiguousLicense(
    licenseName: String,
    licenseUrl: String?,
  ): Boolean {
    val nameLower = licenseName.lowercase().trim()

    // Check for generic license names that don't identify the actual license type
    val ambiguousNames = setOf(
      "license",
      "licence",
      "the license",
      "the licence",
      "see license",
      "see licence",
    )
    if (nameLower in ambiguousNames) {
      return true
    }

    // Check if URL points to a repository LICENSE file (not a known license URL)
    licenseUrl?.let { url ->
      val urlLower = url.lowercase()
      // GitHub/GitLab/Bitbucket LICENSE file patterns
      if (urlLower.contains("/license") &&
        (urlLower.contains("github.com") || urlLower.contains("gitlab.com") || urlLower.contains("bitbucket.org"))
      ) {
        // But not if it's a well-known license URL pattern
        val wellKnownPatterns = listOf(
          "apache.org/licenses",
          "opensource.org/licenses",
          "gnu.org/licenses",
          "eclipse.org/legal",
          "mozilla.org",
          "creativecommons.org",
          "unlicense.org",
        )
        if (wellKnownPatterns.none { urlLower.contains(it) }) {
          return true
        }
      }
    }

    return false
  }

  /**
   * Strips version information from a URL (e.g., version anchors or path segments).
   */
  protected fun stripVersionFromUrl(url: String): String = url
    .replace(Regex("#[\\d.]+$"), "") // Remove #1.7.6 style version anchors
    .replace(Regex("/[\\d.]+/?$"), "") // Remove /1.7.6 style version paths

  /**
   * Supplements existing license entries with well-known names and URLs.
   * Only updates licenses that already exist in the map (detected from dependencies).
   */
  protected fun supplementLicenseInfo(licenseInfoMap: MutableMap<String, Pair<String, String?>>) {
    val wellKnownLicenses = mapOf(
      "apache-2.0" to ("Apache License 2.0" to "https://www.apache.org/licenses/LICENSE-2.0"),
      "mit" to ("MIT License" to "https://opensource.org/licenses/MIT"),
      "bsd-3-clause" to ("BSD 3-Clause License" to "https://opensource.org/licenses/BSD-3-Clause"),
      "bsd-2-clause" to ("BSD 2-Clause License" to "https://opensource.org/licenses/BSD-2-Clause"),
      "lgpl-2.1" to ("GNU Lesser General Public License v2.1" to "https://www.gnu.org/licenses/lgpl-2.1.html"),
      "lgpl-3.0" to ("GNU Lesser General Public License v3.0" to "https://www.gnu.org/licenses/lgpl-3.0.html"),
      "epl-1.0" to ("Eclipse Public License 1.0" to "https://www.eclipse.org/legal/epl-v10.html"),
      "mpl-2.0" to ("Mozilla Public License 2.0" to "https://www.mozilla.org/en-US/MPL/2.0/"),
      "gpl-2.0" to ("GNU General Public License v2.0" to "https://www.gnu.org/licenses/gpl-2.0.html"),
      "gpl-3.0" to ("GNU General Public License v3.0" to "https://www.gnu.org/licenses/gpl-3.0.html"),
      "cc0-1.0" to ("CC0 1.0 Universal" to "https://creativecommons.org/publicdomain/zero/1.0/"),
      "unlicense" to ("The Unlicense" to "https://unlicense.org/"),
      "isc" to ("ISC License" to "https://opensource.org/licenses/ISC"),
      "unknown" to ("Unknown License" to null),
    )

    // Only supplement info for licenses that already exist
    licenseInfoMap.keys.toList().forEach { key ->
      wellKnownLicenses[key]?.let { (name, url) ->
        val existing = licenseInfoMap[key]
        if (existing != null) {
          val currentUrl = existing.second
          licenseInfoMap[key] = name to (url ?: currentUrl)
        }
      }
    }
  }
}

/**
 * Data class to hold information about artifacts with ambiguous licenses.
 */
data class AmbiguousLicenseInfo(
  val coordinate: String,
  val licenseName: String,
  val licenseUrl: String?,
)

/**
 * Serializable version of PomInfo for Configuration Cache compatibility
 */
@Suppress("serial")
data class SerializablePomInfo(
  val name: String?,
  val url: String?,
  val licenses: List<SerializablePomLicense>,
  val developers: List<String>,
) : Serializable {
  fun toPomInfo(): PomInfo = PomInfo(
    name = name,
    url = url,
    licenses = licenses.map { PomLicense(it.name, it.url) },
    developers = developers,
  )
}

/**
 * Serializable version of PomLicense for Configuration Cache compatibility
 */
@Suppress("serial")
data class SerializablePomLicense(
  val name: String,
  val url: String?,
) : Serializable

data class PomInfo(
  val name: String?,
  val url: String?,
  val licenses: List<PomLicense>,
  val developers: List<String>,
) {
  fun toSerializable(): SerializablePomInfo = SerializablePomInfo(
    name = name,
    url = url,
    licenses = licenses.map { SerializablePomLicense(it.name, it.url) },
    developers = developers,
  )
}

data class PomLicense(
  val name: String,
  val url: String?,
)

/**
 * Container for POM data including parent reference
 */
data class PomDataWithParent(
  val pomInfo: PomInfo,
  val parentInfo: ParentPomInfo?,
)

/**
 * Parent POM reference information
 */
data class ParentPomInfo(
  val groupId: String,
  val artifactId: String,
  val version: String,
)
