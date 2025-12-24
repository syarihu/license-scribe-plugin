package net.syarihu.licensescribe.hilt

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import net.syarihu.licensescribe.gradle.TASK_GROUP
import net.syarihu.licensescribe.gradle.registerVariantSourceDirectoriesEarly
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
      project.registerVariantSourceDirectoriesEarly("licensescribe")
      setupAndroidTasks(project, androidComponentsExtension)
    } else {
      // Non-Android project
      project.afterEvaluate {
        registerHiltTask(project, "", null)
      }
    }
  }

  private fun setupAndroidTasks(
    project: Project,
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>,
  ) {
    androidComponentsExtension.onVariants { variant ->
      // Use variant name for output directory to support product flavors
      registerHiltTask(project, variant.name, variant)
    }
  }

  private fun registerHiltTask(
    project: Project,
    variantName: String,
    variant: Variant?,
  ) {
    val suffix = variantName.replaceFirstChar { it.uppercaseChar() }
    val taskName = "scribeLicenses${suffix}GenerateHiltModule"

    val hiltTask =
      project.tasks.register(taskName, GenerateHiltModuleTask::class.java) { task ->
        task.group = TASK_GROUP
        task.description = "Generate Hilt module for license provider for $variantName"
        // Use variant name for output directory to support product flavors
        task.configureWith(project, variantName)

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
}
