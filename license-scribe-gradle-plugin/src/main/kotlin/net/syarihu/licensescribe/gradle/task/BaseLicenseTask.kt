package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.gradle.LicenseScribeExtension
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.IgnoreRules
import net.syarihu.licensescribe.model.Catalog
import net.syarihu.licensescribe.model.ScopedRecords
import net.syarihu.licensescribe.parser.CatalogParser
import net.syarihu.licensescribe.parser.IgnoreRulesParser
import net.syarihu.licensescribe.parser.RecordsParser
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
  abstract val recordsFileName: Property<String>

  @get:Input
  abstract val catalogFileName: Property<String>

  @get:Input
  abstract val ignoreFileName: Property<String>

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
  ) {
    this.baseDir.set(extension.baseDir)
    this.recordsFileName.set(extension.recordsFile)
    this.catalogFileName.set(extension.catalogFile)
    this.ignoreFileName.set(extension.ignoreFile)

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

  protected fun resolveRecordsFile(): File = baseDir.file(recordsFileName).get().asFile

  protected fun resolveCatalogFile(): File = baseDir.file(catalogFileName).get().asFile

  protected fun resolveIgnoreFile(): File = baseDir.file(ignoreFileName).get().asFile

  protected fun loadRecords(): List<ScopedRecords> = RecordsParser().parse(resolveRecordsFile())

  protected fun loadCatalog(): Catalog = CatalogParser().parse(resolveCatalogFile())

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
   * Resolve POM info at configuration time
   */
  private fun resolvePomInfoFromConfiguration(
    config: Configuration,
    artifactId: ArtifactId,
  ): PomInfo? = try {
    val componentId =
      config.incoming.resolutionResult.allComponents
        .map { it.id }
        .filterIsInstance<ModuleComponentIdentifier>()
        .find { it.group == artifactId.group && it.module == artifactId.name }
        ?: return null

    val result =
      project.dependencies
        .createArtifactResolutionQuery()
        .forComponents(componentId)
        .withArtifacts(MavenModule::class.java, MavenPomArtifact::class.java)
        .execute()

    val pomArtifact =
      result.resolvedComponents
        .flatMap { component ->
          component.getArtifacts(MavenPomArtifact::class.java)
        }.filterIsInstance<ResolvedArtifactResult>()
        .firstOrNull()

    pomArtifact?.file?.let { parsePom(it) }
  } catch (e: Exception) {
    logger.debug("Failed to resolve POM for ${artifactId.coordinate}: ${e.message}")
    null
  }

  private fun parsePom(pomFile: File): PomInfo? = try {
    val factory =
      javax.xml.parsers.DocumentBuilderFactory
        .newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(pomFile)
    doc.documentElement.normalize()

    val name = getTextContent(doc, "name")
    val url = getTextContent(doc, "url")

    val licenses =
      try {
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

    val developers =
      try {
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

    PomInfo(
      name = name,
      url = url,
      licenses = licenses,
      developers = developers,
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
}

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
