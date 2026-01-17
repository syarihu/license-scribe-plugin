package net.syarihu.licensescribe.screen

import net.syarihu.licensescribe.generator.LicenseCodeGenerator
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.PomInfo
import net.syarihu.licensescribe.parser.LicenseCatalogParser
import net.syarihu.licensescribe.parser.PomParser
import net.syarihu.licensescribe.resolver.DependencyInfo
import net.syarihu.licensescribe.resolver.LicenseCatalogBuilder
import net.syarihu.licensescribe.resolver.LicenseCatalogResolver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.internal.component.external.model.DefaultModuleComponentIdentifier
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import java.io.File

/**
 * Task to generate OpenSourceLicensesActivity, AppLicenses, and AndroidManifest.
 */
abstract class GenerateScreenTask : DefaultTask() {
  @get:Internal
  abstract val baseDir: DirectoryProperty

  @get:Input
  abstract val licensesFileName: Property<String>

  @get:Input
  abstract val variantName: Property<String>

  @get:Input
  abstract val resolvedDependencies: ListProperty<String>

  @get:Input
  abstract val pomInfoMap: MapProperty<String, PomInfo>

  @get:Input
  abstract val generatedPackageName: Property<String>

  @get:Input
  abstract val licensesClassName: Property<String>

  @get:Input
  abstract val activityClassName: Property<String>

  @get:Input
  abstract val nightMode: Property<String>

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @get:OutputDirectory
  abstract val resOutputDirectory: DirectoryProperty

  @get:OutputFile
  abstract val generatedManifestFile: RegularFileProperty

  private val codeGenerator = LicenseCodeGenerator()
  private val activityGenerator = OpenSourceLicensesActivityGenerator()

  fun configureWith(
    extension: LicenseScribeScreenExtension,
    configuration: Configuration?,
    variantName: String,
    project: Project,
  ) {
    // Set up base directory in build directory (not user-managed)
    val buildLicenseDir = project.layout.buildDirectory.dir("licensescribe/$variantName")
    this.baseDir.set(buildLicenseDir)
    this.licensesFileName.set("scribe-licenses.yml")
    this.variantName.set(variantName)

    // Set up dependency resolution
    if (configuration != null) {
      this.resolvedDependencies.set(
        project.provider {
          resolveDependenciesFromConfiguration(configuration)
        },
      )

      this.pomInfoMap.set(
        project.provider {
          resolvePomInfoMapFromConfiguration(configuration)
        },
      )
    } else {
      this.resolvedDependencies.set(emptyList())
      this.pomInfoMap.set(emptyMap())
    }

    this.generatedPackageName.set(extension.generatedPackageName)
    this.licensesClassName.set(extension.licensesClassName)
    this.activityClassName.set(extension.activityClassName)
    this.nightMode.set(extension.nightMode)

    // Use variant-specific output directory
    val variantDir = if (variantName.isNotEmpty()) {
      "generated/source/licensescribe-screen/$variantName"
    } else {
      "generated/source/licensescribe-screen"
    }
    this.outputDirectory.set(project.layout.buildDirectory.dir(variantDir))

    // Use variant-specific res output directory
    val resDir = if (variantName.isNotEmpty()) {
      "generated/res/licensescribe-screen/$variantName"
    } else {
      "generated/res/licensescribe-screen"
    }
    this.resOutputDirectory.set(project.layout.buildDirectory.dir(resDir))

    // Set up manifest output
    val manifestDir = if (variantName.isNotEmpty()) {
      "generated/manifests/licensescribe-screen/$variantName"
    } else {
      "generated/manifests/licensescribe-screen"
    }
    this.generatedManifestFile.set(
      project.layout.buildDirectory.file("$manifestDir/AndroidManifest.xml"),
    )
  }

  @TaskAction
  fun execute() {
    val packageName = generatedPackageName.get()
    validatePackageName(packageName)

    val licensesClass = licensesClassName.get()
    validateClassName(licensesClass, "licensesClassName")

    val activityClass = activityClassName.get()
    validateClassName(activityClass, "activityClassName")

    val nightModeValue = nightMode.get()
    validateNightMode(nightModeValue)

    // Build dependency info list
    val dependencies = buildDependencyInfoList()

    // Generate catalog using core builder
    val catalog = LicenseCatalogBuilder.build(dependencies)

    // Write YAML to build directory
    val licensesFile = resolveLicensesFile()
    licensesFile.parentFile?.mkdirs()
    licensesFile.writeText(LicenseCatalogParser().serialize(catalog))

    // Resolve catalog to licenses using core resolver
    val resolvedLicenses = LicenseCatalogResolver.resolve(catalog)

    // Generate AppLicenses code
    val outputDir = outputDirectory.get().asFile
    outputDir.mkdirs()

    codeGenerator.generate(
      licenses = resolvedLicenses,
      packageName = packageName,
      className = licensesClassName.get(),
      outputDir = outputDir,
    )

    // Generate OpenSourceLicensesActivity code and layout XML
    val resOutputDir = resOutputDirectory.get().asFile
    resOutputDir.mkdirs()

    activityGenerator.generate(
      packageName = packageName,
      licensesClassName = licensesClassName.get(),
      activityClassName = activityClassName.get(),
      outputDir = outputDir,
      resOutputDir = resOutputDir,
      nightMode = nightMode.get(),
    )

    // Generate AndroidManifest.xml
    generateManifest(packageName)

    logger.lifecycle(
      "Generated ${licensesClassName.get()}.kt, ${activityClassName.get()}.kt, " +
        "and AndroidManifest.xml with ${resolvedLicenses.size} licenses",
    )
  }

  private fun resolveLicensesFile(): File = File(baseDir.get().asFile, licensesFileName.get())

  /**
   * Build dependency info list from resolved dependencies and POM info.
   */
  private fun buildDependencyInfoList(): List<DependencyInfo> {
    val pomMap = pomInfoMap.get()
    return resolvedDependencies.get().map { mavenCoordinate ->
      val parts = mavenCoordinate.split(":")
      val artifactId = ArtifactId(
        group = parts[0],
        name = parts[1],
        version = parts.getOrElse(2) { "" },
      )
      val key = "${artifactId.group}:${artifactId.name}"
      DependencyInfo(
        artifactId = artifactId,
        pomInfo = pomMap[key],
      )
    }
  }

  /**
   * Generate AndroidManifest.xml with Activity registration.
   *
   * The activity uses Theme.AppCompat.Light.NoActionBar (or DayNight variant) to prevent
   * conflicts with apps that have windowActionBar=true in their theme.
   * The generated Activity uses a custom Toolbar with setSupportActionBar(),
   * which requires windowActionBar=false.
   */
  private fun generateManifest(packageName: String) {
    val manifestFile = generatedManifestFile.get().asFile
    manifestFile.parentFile?.mkdirs()

    val activityName = activityClassName.get()
    // Use DayNight.NoActionBar for dark mode support, Light.NoActionBar otherwise
    val activityTheme = if (nightMode.get() == "no") {
      "@style/Theme.AppCompat.Light.NoActionBar"
    } else {
      "@style/Theme.AppCompat.DayNight.NoActionBar"
    }
    val manifest = """
            |<?xml version="1.0" encoding="utf-8"?>
            |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
            |  <application>
            |    <activity
            |      android:name="$packageName.$activityName"
            |      android:exported="false"
            |      android:theme="$activityTheme" />
            |  </application>
            |</manifest>
    """.trimMargin()

    manifestFile.writeText(manifest)
  }

  /**
   * Resolve dependencies from configuration.
   * Returns list of "group:name:version" strings.
   */
  private fun resolveDependenciesFromConfiguration(config: Configuration): List<String> {
    return try {
      config.incoming.resolutionResult.allComponents
        .mapNotNull { component ->
          val id = component.id
          if (id is ModuleComponentIdentifier) {
            "${id.group}:${id.module}:${id.version}"
          } else {
            null
          }
        }
        .filter {
          val parts = it.split(":")
          parts[0].isNotBlank() && parts[1].isNotBlank()
        }
        .distinctBy {
          val parts = it.split(":")
          "${parts[0]}:${parts[1]}"
        }
    } catch (e: Exception) {
      logger.warn("Failed to resolve dependencies: ${e.message}")
      emptyList()
    }
  }

  /**
   * Resolve POM info map from configuration.
   */
  private fun resolvePomInfoMapFromConfiguration(config: Configuration): Map<String, PomInfo> {
    val dependencies = resolveDependenciesFromConfiguration(config)
    val pomMap = mutableMapOf<String, PomInfo>()

    dependencies.forEach { coordinate ->
      val parts = coordinate.split(":")
      if (parts.size >= 3) {
        val artifactId = ArtifactId(
          group = parts[0],
          name = parts[1],
          version = parts[2],
        )
        resolvePomInfoForArtifact(artifactId)?.let { pomInfo ->
          pomMap["${artifactId.group}:${artifactId.name}"] = pomInfo
        }
      }
    }

    return pomMap
  }

  /**
   * Resolve POM info for a single artifact.
   */
  private fun resolvePomInfoForArtifact(artifactId: ArtifactId): PomInfo? {
    return try {
      val version = artifactId.version
      if (version.isNullOrBlank()) {
        return null
      }
      resolvePomInfoRecursive(artifactId.group, artifactId.name, version, maxDepth = 5)
    } catch (e: Exception) {
      logger.debug("Failed to resolve POM for ${artifactId.coordinate}: ${e.message}")
      null
    }
  }

  /**
   * Recursively resolve POM info, following parent POMs if needed.
   */
  private fun resolvePomInfoRecursive(
    groupId: String,
    artifactIdStr: String,
    version: String,
    maxDepth: Int,
  ): PomInfo? {
    if (maxDepth <= 0) {
      return null
    }

    val pomFile = resolvePomFile(groupId, artifactIdStr, version) ?: return null
    val pomData = PomParser.parse(pomFile) ?: return null

    // If we have licenses, no need to check parent
    if (pomData.licenses.isNotEmpty()) {
      return pomData
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
        // Merge: child values take precedence
        return PomInfo(
          name = pomData.name ?: parentPomInfo.name,
          url = pomData.url ?: parentPomInfo.url,
          licenses = pomData.licenses.ifEmpty { parentPomInfo.licenses },
          developers = pomData.developers.ifEmpty { parentPomInfo.developers },
          parentGroupId = null,
          parentArtifactId = null,
          parentVersion = null,
        )
      }
    }

    return pomData
  }

  /**
   * Resolve POM file for a given artifact.
   */
  private fun resolvePomFile(
    groupId: String,
    artifactIdStr: String,
    version: String,
  ): File? = try {
    val componentId = DefaultModuleComponentIdentifier.newId(
      DefaultModuleIdentifier.newId(groupId, artifactIdStr),
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
    logger.debug("Failed to resolve POM file for $groupId:$artifactIdStr:$version: ${e.message}")
    null
  }

  private fun validatePackageName(packageName: String) {
    if (packageName.isBlank()) {
      throw GradleException(
        "licenseScribeScreen.generatedPackageName is required. " +
          "Please set it in your build.gradle.kts:\n\n" +
          "licenseScribeScreen {\n" +
          "    generatedPackageName.set(\"com.example.app\")\n" +
          "}",
      )
    }

    val packageNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$")
    if (!packageName.matches(packageNamePattern)) {
      throw GradleException(
        "Invalid package name: '$packageName'\n" +
          "Package name must:\n" +
          "  - Start with a letter\n" +
          "  - Contain only letters, digits, and underscores\n" +
          "  - Use dots to separate segments\n" +
          "  - Each segment must start with a letter\n\n" +
          "Example: com.example.app",
      )
    }
  }

  private fun validateClassName(
    className: String,
    propertyName: String,
  ) {
    if (className.isBlank()) {
      throw GradleException(
        "licenseScribeScreen.$propertyName is required.",
      )
    }

    val classNamePattern = Regex("^[a-zA-Z][a-zA-Z0-9_]*$")
    if (!className.matches(classNamePattern)) {
      throw GradleException(
        "Invalid class name for $propertyName: '$className'\n" +
          "Class name must:\n" +
          "  - Start with a letter\n" +
          "  - Contain only letters, digits, and underscores\n\n" +
          "Example: AppLicenses, OpenSourceLicensesActivity",
      )
    }
  }

  private fun validateNightMode(nightMode: String) {
    val validValues = listOf("followSystem", "yes", "no")
    if (nightMode !in validValues) {
      throw GradleException(
        "Invalid nightMode value: '$nightMode'\n" +
          "nightMode must be one of: ${validValues.joinToString(", ")}\n\n" +
          "Example:\n" +
          "licenseScribeScreen {\n" +
          "    nightMode.set(\"followSystem\") // Follow system dark mode setting\n" +
          "    // or\n" +
          "    nightMode.set(\"yes\") // Always use dark mode\n" +
          "    // or\n" +
          "    nightMode.set(\"no\") // Always use light mode\n" +
          "}",
      )
    }
  }
}
