package net.syarihu.licensescribe.hilt

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for generating Hilt module that provides LicenseProvider.
 * This plugin should be applied together with the base license-scribe plugin.
 */
class LicenseScribeHiltPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    // Verify that the base plugin is applied
    project.afterEvaluate {
      val licenseScribeExtension = project.extensions.findByName("licenseScribe")
      if (licenseScribeExtension == null) {
        project.logger.warn(
          "LicenseScribeHiltPlugin: The base 'net.syarihu.license-scribe' plugin is not applied. " +
            "Please apply it before this plugin.",
        )
        return@afterEvaluate
      }
    }

    // Setup for Android project
    val androidComponentsExtension = project.extensions.findByType(AndroidComponentsExtension::class.java)
    if (androidComponentsExtension != null) {
      // Register source directories early for Kotlin compiler compatibility
      registerBuildTypeSourceDirectoriesEarly(project)
      setupAndroidTasks(project, androidComponentsExtension)
    } else {
      // Non-Android project
      project.afterEvaluate {
        registerHiltTask(project, "", "", null)
      }
    }
  }

  private fun registerBuildTypeSourceDirectoriesEarly(project: Project) {
    try {
      val androidExtension = project.extensions.findByName("android") ?: return
      val sourceSetsMethod = androidExtension.javaClass.getMethod("getSourceSets")
      val sourceSets = sourceSetsMethod.invoke(androidExtension)

      // Get build types from android extension
      val buildTypesMethod = androidExtension.javaClass.getMethod("getBuildTypes")
      val buildTypes = buildTypesMethod.invoke(androidExtension) as Iterable<*>

      for (buildType in buildTypes) {
        val nameMethod = buildType?.javaClass?.getMethod("getName")
        val buildTypeName = nameMethod?.invoke(buildType) as? String ?: continue

        val outputDir = project.layout.buildDirectory.dir("generated/source/licensescribe/$buildTypeName")

        val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
        val sourceSet = getByNameMethod.invoke(sourceSets, buildTypeName)
        val kotlinProperty = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet)
        val srcDirMethod = kotlinProperty.javaClass.getMethod("srcDir", Any::class.java)
        srcDirMethod.invoke(kotlinProperty, outputDir)
      }
    } catch (e: Exception) {
      project.logger.debug("Could not register buildType source directories early: ${e.message}")
    }
  }

  private fun setupAndroidTasks(
    project: Project,
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>,
  ) {
    androidComponentsExtension.onVariants { variant ->
      // Use buildType name for output directory to match source set registration
      val buildTypeName = variant.buildType ?: variant.name
      registerHiltTask(project, variant.name, buildTypeName, variant)
    }
  }

  private fun registerHiltTask(
    project: Project,
    variantName: String,
    buildTypeName: String,
    variant: Variant?,
  ) {
    val suffix = variantName.replaceFirstChar { it.uppercaseChar() }
    val taskName = "scribeLicenses${suffix}GenerateHiltModule"

    val hiltTask =
      project.tasks.register(taskName, GenerateHiltModuleTask::class.java) { task ->
        task.group = TASK_GROUP
        task.description = "Generate Hilt module for license provider for $variantName"
        // Use buildType name for output directory to match source set registration
        task.configureWith(project, buildTypeName)

        // Depend on the base generate task
        val baseTaskName = "scribeLicenses${suffix}Generate"
        project.tasks.findByName(baseTaskName)?.let { baseTask ->
          task.dependsOn(baseTask)
        }
      }

    // Use AGP Variant API to register source directory for IDE integration
    // Use .java instead of .kotlin for proper IDE recognition as generated sources root
    if (variant != null) {
      variant.sources.java?.addGeneratedSourceDirectory(
        hiltTask,
        GenerateHiltModuleTask::outputDirectory,
      )
    }

    // Set up variant-specific task dependencies as a safety measure
    val variantSuffix = variantName.replaceFirstChar { it.uppercaseChar() }
    project.tasks.matching {
      (
        it.name.contains("compile", ignoreCase = true) &&
          it.name.contains("Kotlin", ignoreCase = true) &&
          it.name.contains(variantSuffix, ignoreCase = true)
        ) ||
        (it.name.startsWith("ksp") && it.name.contains(variantSuffix) && it.name.endsWith("Kotlin"))
    }.configureEach { compileTask ->
      compileTask.dependsOn(hiltTask)
    }
  }

  companion object {
    const val TASK_GROUP = "license-scribe"
  }
}
