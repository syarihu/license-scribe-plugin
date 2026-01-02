package net.syarihu.licensescribe.screen

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

private const val TASK_GROUP = "license scribe"

/**
 * Gradle plugin for generating OpenSourceLicensesActivity with embedded license data.
 * This plugin automatically generates:
 * - AppLicenses object with all license information
 * - OpenSourceLicensesActivity for displaying licenses (View-based, no Compose dependency)
 * - AndroidManifest.xml with Activity registration
 */
class LicenseScribeScreenPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create(
      "licenseScribeScreen",
      LicenseScribeScreenExtension::class.java,
    )

    // Set defaults
    extension.generatedPackageName.convention(
      project.provider {
        project.group.toString().takeIf { it.isNotBlank() } ?: "licenses"
      },
    )
    extension.licensesClassName.convention("AppLicenses")
    extension.activityClassName.convention("OpenSourceLicensesActivity")

    // Setup for Android project
    val androidComponentsExtension = project.extensions.findByType(AndroidComponentsExtension::class.java)
    if (androidComponentsExtension != null) {
      // Register source directories early for IDE compatibility
      registerVariantSourceDirectoriesEarly(project)
      setupAndroidTasks(project, extension, androidComponentsExtension)
    } else {
      project.logger.warn(
        "LicenseScribeScreenPlugin requires an Android project. " +
          "Please apply 'com.android.application' or 'com.android.library' plugin first.",
      )
    }
  }

  private fun setupAndroidTasks(
    project: Project,
    extension: LicenseScribeScreenExtension,
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>,
  ) {
    androidComponentsExtension.onVariants { variant ->
      val configuration = getAndroidConfiguration(project, variant.name)
      registerTasks(project, extension, variant.name, configuration, variant)
    }
  }

  private fun registerTasks(
    project: Project,
    extension: LicenseScribeScreenExtension,
    variantName: String,
    configuration: Configuration?,
    variant: Variant,
  ) {
    val variantSuffix = variantName.replaceFirstChar { it.uppercaseChar() }
    val taskName = "scribeLicenses${variantSuffix}ScreenGenerate"

    val generateTask = project.tasks.register(
      taskName,
      GenerateScreenTask::class.java,
    ) { task ->
      task.group = TASK_GROUP
      task.description = "Generate OpenSourceLicensesActivity and AppLicenses for $variantName"
      task.configureWith(extension, configuration, variantName, project)
    }

    // Register as generated source directory for IDE integration
    variant.sources.java?.addGeneratedSourceDirectory(
      generateTask,
      GenerateScreenTask::outputDirectory,
    )

    // Register generated res directory
    variant.sources.res?.addGeneratedSourceDirectory(
      generateTask,
      GenerateScreenTask::resOutputDirectory,
    )

    // Register generated manifest
    variant.sources.manifests?.addGeneratedManifestFile(
      generateTask,
      GenerateScreenTask::generatedManifestFile,
    )

    // Make compile tasks depend on this
    project.tasks.matching {
      (
        it.name.contains("compile", ignoreCase = true) &&
          it.name.contains("Kotlin", ignoreCase = true) &&
          it.name.contains(variantSuffix, ignoreCase = true)
        ) ||
        (it.name.startsWith("ksp") && it.name.contains(variantSuffix) && it.name.endsWith("Kotlin"))
    }.configureEach { compileTask ->
      compileTask.dependsOn(generateTask)
    }
  }

  private fun getAndroidConfiguration(
    project: Project,
    variant: String,
  ): Configuration? = project.configurations.findByName("${variant}RuntimeClasspath")
    ?: project.configurations.findByName("${variant}CompileClasspath")

  /**
   * Register source directories early for Kotlin compiler compatibility.
   */
  private fun registerVariantSourceDirectoriesEarly(project: Project) {
    project.afterEvaluate {
      try {
        val androidExtension = project.extensions.findByName("android") ?: return@afterEvaluate

        // Get build types
        val buildTypesMethod = androidExtension.javaClass.getMethod("getBuildTypes")
        val buildTypes = buildTypesMethod.invoke(androidExtension) as? Iterable<*> ?: return@afterEvaluate

        // Get product flavors
        val productFlavorsMethod = androidExtension.javaClass.getMethod("getProductFlavors")
        val productFlavors = productFlavorsMethod.invoke(androidExtension) as? Iterable<*> ?: emptyList<Any>()

        val flavorNames = productFlavors.mapNotNull { flavor ->
          try {
            flavor?.javaClass?.getMethod("getName")?.invoke(flavor) as? String
          } catch (_: Exception) {
            null
          }
        }

        buildTypes.forEach { buildType ->
          val buildTypeName = try {
            buildType?.javaClass?.getMethod("getName")?.invoke(buildType) as? String
          } catch (_: Exception) {
            null
          } ?: return@forEach

          if (flavorNames.isEmpty()) {
            // No flavors - just use build type name
            registerSourceDirectory(project, buildTypeName)
          } else {
            // With flavors - combine flavor + buildType
            flavorNames.forEach { flavorName ->
              val variantName = "$flavorName${buildTypeName.replaceFirstChar { it.uppercaseChar() }}"
              registerSourceDirectory(project, variantName)
            }
          }
        }
      } catch (e: Exception) {
        project.logger.debug("Could not register variant source directories early: ${e.message}")
      }
    }
  }

  private fun registerSourceDirectory(
    project: Project,
    variantName: String,
  ) {
    try {
      val androidExtension = project.extensions.findByName("android") ?: return
      val sourceSetsMethod = androidExtension.javaClass.getMethod("getSourceSets")
      val sourceSets = sourceSetsMethod.invoke(androidExtension)
      val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
      val sourceSet = getByNameMethod.invoke(sourceSets, variantName)

      val kotlinProperty = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet)
      val srcDirMethod = kotlinProperty.javaClass.getMethod("srcDir", Any::class.java)

      val outputDir = project.layout.buildDirectory
        .dir("generated/source/licensescribe-screen/$variantName")
        .get()
        .asFile

      srcDirMethod.invoke(kotlinProperty, outputDir)
    } catch (e: Exception) {
      project.logger.debug("Could not register source directory for $variantName: ${e.message}")
    }
  }
}
