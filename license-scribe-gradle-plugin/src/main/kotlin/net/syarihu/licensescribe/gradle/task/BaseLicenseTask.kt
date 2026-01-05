package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.gradle.LicenseScribeExtension
import net.syarihu.licensescribe.gradle.task.model.PomInfo
import net.syarihu.licensescribe.gradle.task.model.PomLicense
import net.syarihu.licensescribe.gradle.task.model.SerializablePomInfo
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.IgnoreRules
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.parser.IgnoreRulesParser
import net.syarihu.licensescribe.parser.LicenseCatalogParser
import net.syarihu.licensescribe.parser.PomParser
import net.syarihu.licensescribe.util.LicenseNormalizer
import net.syarihu.licensescribe.util.WellKnownLicenses
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.io.File
import net.syarihu.licensescribe.model.PomInfo as CorePomInfo

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
    variantName: String = "",
  ) {
    this.baseDir.set(extension.baseDir)
    this.licensesFileName.set(extension.licensesFile)
    this.ignoreFileName.set(extension.ignoreFile)
    this.variantName.set(variantName)

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
          val dependencies = resolveDependenciesFromConfiguration(configuration)
          val pomMap = mutableMapOf<String, SerializablePomInfo>()
          dependencies.forEach { artifactId ->
            resolvePomInfoFromConfiguration(artifactId)?.let { pomInfo ->
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
  protected fun resolveDependencies(): List<ArtifactId> = resolvedDependencies.get().map { mavenCoordinate ->
    val parts = mavenCoordinate.split(":")
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
      }
      .filter { it.group.isNotBlank() && it.name.isNotBlank() }
      .distinctBy { "${it.group}:${it.name}" }
  } catch (e: Exception) {
    logger.warn("Failed to resolve dependencies: ${e.message}")
    emptyList()
  }

  /**
   * Resolve POM info at configuration time, including parent POM resolution
   */
  private fun resolvePomInfoFromConfiguration(artifactId: ArtifactId): PomInfo? {
    return try {
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
    val parentGroupId = pomData.parentGroupId
    val parentArtifactId = pomData.parentArtifactId
    val parentVersion = pomData.parentVersion

    if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
      val parentPomInfo = resolvePomInfoRecursive(
        parentGroupId,
        parentArtifactId,
        parentVersion,
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
    val componentId = DefaultModuleComponentIdentifier.newId(
      DefaultModuleIdentifier.newId(groupId, artifactId),
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
      }
      .filterIsInstance<ResolvedArtifactResult>()
      .firstOrNull()
      ?.file
  } catch (e: Exception) {
    logger.debug("Failed to resolve POM file for $groupId:$artifactId:$version: ${e.message}")
    null
  }

  /**
   * Data class to hold POM info with parent info.
   */
  private data class PomDataWithParentInfo(
    val pomInfo: PomInfo,
    val parentGroupId: String?,
    val parentArtifactId: String?,
    val parentVersion: String?,
  )

  /**
   * Parse POM file and extract both POM info and parent info using core PomParser.
   */
  private fun parsePomWithParentInfo(pomFile: File): PomDataWithParentInfo? {
    val corePomInfo = PomParser.parse(pomFile) ?: return null
    return PomDataWithParentInfo(
      pomInfo = corePomInfo.toGradlePomInfo(),
      parentGroupId = corePomInfo.parentGroupId,
      parentArtifactId = corePomInfo.parentArtifactId,
      parentVersion = corePomInfo.parentVersion,
    )
  }

  /**
   * Convert core PomInfo to gradle-plugin PomInfo.
   */
  private fun CorePomInfo.toGradlePomInfo(): PomInfo = PomInfo(
    name = name,
    url = url,
    licenses = licenses.map { PomLicense(it.name, it.url) },
    developers = developers,
  )

  /**
   * Normalizes a license name and URL to a standard license key (e.g., "apache-2.0", "mit").
   * First tries to identify the license from the URL (more reliable), then from the name.
   *
   * Delegates to [LicenseNormalizer.normalizeKey].
   */
  protected fun normalizeLicenseKey(
    licenseName: String,
    licenseUrl: String? = null,
  ): String = LicenseNormalizer.normalizeKey(licenseName, licenseUrl)

  /**
   * Checks if a license name/URL combination is ambiguous and requires manual verification.
   * Returns true for licenses like "LICENSE", "LICENCE", or URLs pointing to repository LICENSE files.
   *
   * Delegates to [LicenseNormalizer.isAmbiguousLicense].
   */
  protected fun isAmbiguousLicense(
    licenseName: String,
    licenseUrl: String?,
  ): Boolean = LicenseNormalizer.isAmbiguousLicense(licenseName, licenseUrl)

  /**
   * Strips version information from a URL (e.g., version anchors or path segments).
   *
   * Delegates to [LicenseNormalizer.stripVersionFromUrl].
   */
  protected fun stripVersionFromUrl(url: String): String = LicenseNormalizer.stripVersionFromUrl(url)

  /**
   * Supplements existing license entries with well-known names and URLs.
   * Only updates licenses that already exist in the map (detected from dependencies).
   *
   * Delegates to [WellKnownLicenses.supplementLicenseInfo].
   */
  protected fun supplementLicenseInfo(licenseInfoMap: MutableMap<String, Pair<String, String?>>) {
    WellKnownLicenses.supplementLicenseInfo(licenseInfoMap)
  }

  /**
   * Returns default copyright holders for a given license key.
   * Used when POM files don't include developer information.
   *
   * Delegates to [WellKnownLicenses.getDefaultCopyrightHolders].
   */
  protected fun getDefaultCopyrightHolders(licenseKey: String): List<String> = WellKnownLicenses.getDefaultCopyrightHolders(licenseKey)
}
