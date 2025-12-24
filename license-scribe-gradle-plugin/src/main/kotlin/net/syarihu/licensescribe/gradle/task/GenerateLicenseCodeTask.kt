package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.generator.LicenseCodeGenerator
import net.syarihu.licensescribe.gradle.LicenseScribeExtension
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.License
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
    variantName: String,
    project: Project,
  ) {
    super.configureWith(extension, configuration, variantName)
    this.generatedPackageName.set(extension.generatedPackageName)
    this.generatedClassName.set(extension.generatedClassName)

    // Use variant-specific output directory to avoid conflicts between variants
    val variantDir = if (variantName.isNotEmpty()) "generated/source/licensescribe/$variantName" else "generated/source/licensescribe"
    this.outputDirectory.set(project.layout.buildDirectory.dir(variantDir))
  }

  @TaskAction
  fun execute() {
    val catalog = loadLicenseCatalog()

    val resolvedLicenses = mutableListOf<ResolvedLicense>()

    catalog.licenses.forEach { (licenseKey, licenseEntry) ->
      val mainLicense = License(
        key = licenseKey,
        name = licenseEntry.name,
        url = licenseEntry.url,
      )

      licenseEntry.artifacts.forEach { (groupId, artifacts) ->
        artifacts.forEach { artifact ->
          // Resolve alternative licenses
          val alternativeLicenses = artifact.alternativeLicenses?.mapNotNull { altKey ->
            catalog.licenses[altKey]?.let { altEntry ->
              License(key = altKey, name = altEntry.name, url = altEntry.url)
            }
          }

          // Resolve additional licenses
          val additionalLicenses = artifact.additionalLicenses?.mapNotNull { addKey ->
            catalog.licenses[addKey]?.let { addEntry ->
              License(key = addKey, name = addEntry.name, url = addEntry.url)
            }
          }

          resolvedLicenses.add(
            ResolvedLicense(
              artifactId = ArtifactId(
                group = groupId,
                name = artifact.name,
              ),
              artifactName = artifact.name,
              artifactUrl = artifact.url,
              copyrightHolders = artifact.copyrightHolders,
              license = mainLicense,
              alternativeLicenses = alternativeLicenses?.takeIf { it.isNotEmpty() },
              additionalLicenses = additionalLicenses?.takeIf { it.isNotEmpty() },
            ),
          )
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
