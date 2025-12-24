package net.syarihu.licensescribe.hilt

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Task to generate Hilt module for LicenseProvider.
 */
abstract class GenerateHiltModuleTask : DefaultTask() {
  @get:Input
  abstract val generatedPackageName: Property<String>

  @get:Input
  abstract val licensesClassName: Property<String>

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  fun configureWith(
    project: Project,
    variantName: String,
  ) {
    // Get configuration from the base plugin's extension
    val extension = project.extensions.findByName("licenseScribe")
    if (extension != null) {
      val packageNameProp = extension.javaClass.getMethod("getGeneratedPackageName").invoke(extension)
      val classNameProp = extension.javaClass.getMethod("getGeneratedClassName").invoke(extension)

      @Suppress("UNCHECKED_CAST")
      this.generatedPackageName.set((packageNameProp as Property<String>))
      @Suppress("UNCHECKED_CAST")
      this.licensesClassName.set((classNameProp as Property<String>))
    } else {
      this.generatedPackageName.convention("licenses")
      this.licensesClassName.convention("Licenses")
    }

    // Use variant-specific output directory to match the base plugin
    val variantDir =
      if (variantName.isNotEmpty()) "generated/source/licensescribe/$variantName" else "generated/source/licensescribe"
    this.outputDirectory.set(project.layout.buildDirectory.dir(variantDir))
  }

  @TaskAction
  fun execute() {
    val packageName = generatedPackageName.get()
    if (packageName.isBlank()) {
      throw GradleException(
        "licenseScribe.generatedPackageName is required. " +
          "Please set it in your build.gradle.kts:\n\n" +
          "licenseScribe {\n" +
          "    generatedPackageName.set(\"com.example.app\")\n" +
          "}",
      )
    }

    val outputDir = outputDirectory.get().asFile
    outputDir.mkdirs()

    val generator = HiltModuleGenerator()
    generator.generate(
      packageName = generatedPackageName.get(),
      licensesClassName = licensesClassName.get(),
      outputDir = outputDir,
    )

    logger.lifecycle("Generated LicenseScribeHiltModule.kt in $outputDir")
  }
}
