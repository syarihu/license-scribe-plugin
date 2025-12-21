package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.generator.LicenseCodeGenerator
import net.syarihu.licensescribe.gradle.LicenseScribeExtension
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.ResolvedLicense
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task to generate Kotlin code for license information.
 */
abstract class GenerateLicenseCodeTask : BaseLicenseTask() {
  @get:Input
  abstract val generatedPackageName: Property<String>

  @get:Input
  abstract val generatedClassName: Property<String>

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  private val codeGenerator = LicenseCodeGenerator()

  fun configureWith(
    extension: LicenseScribeExtension,
    configuration: Configuration?,
    variant: String,
    project: Project,
  ) {
    super.configureWith(extension, configuration)
    this.generatedPackageName.set(extension.generatedPackageName)
    this.generatedClassName.set(extension.generatedClassName)

    // Use the same output directory for all variants to avoid duplication
    this.outputDirectory.set(project.layout.buildDirectory.dir("generated/source/licensescribe"))
  }

  @TaskAction
  fun execute() {
    val definitions = loadArtifactDefinitions()
    val catalog = loadLicenseCatalog()

    val resolvedLicenses =
      definitions.flatMap { scoped ->
        scoped.groups.flatMap { group ->
          group.artifacts.mapNotNull { artifact ->
            val license = catalog.getLicense(artifact.license)
            if (license != null) {
              ResolvedLicense(
                artifactId =
                ArtifactId(
                  group = group.groupId,
                  name = artifact.name,
                ),
                artifactName = artifact.name,
                artifactUrl = artifact.url,
                copyrightHolder = artifact.copyrightHolder,
                license = license,
              )
            } else {
              logger.warn(
                "Unknown license '${artifact.license}' for ${group.groupId}:${artifact.name}",
              )
              null
            }
          }
        }
      }

    val outputDir = outputDirectory.get().asFile
    outputDir.mkdirs()

    codeGenerator.generate(
      licenses = resolvedLicenses,
      packageName = generatedPackageName.get(),
      className = generatedClassName.get(),
      outputDir = outputDir,
    )

    logger.lifecycle(
      "Generated ${generatedClassName.get()}.kt with ${resolvedLicenses.size} licenses " +
        "in ${outputDir.absolutePath}",
    )
  }
}
