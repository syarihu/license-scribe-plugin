package net.syarihu.licensescribe.hilt

import com.android.build.api.variant.AndroidComponentsExtension
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
      setupAndroidTasks(project, androidComponentsExtension)
    } else {
      // Non-Android project
      project.afterEvaluate {
        registerHiltTask(project, "")
      }
    }
  }

  private fun setupAndroidTasks(
    project: Project,
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>,
  ) {
    androidComponentsExtension.onVariants { variant ->
      registerHiltTask(project, variant.name)
    }
  }

  private fun registerHiltTask(
    project: Project,
    variantName: String,
  ) {
    val suffix = variantName.replaceFirstChar { it.uppercaseChar() }
    val taskName = "generate${suffix}LicenseHiltModule"

    project.tasks.register(taskName, GenerateHiltModuleTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Generate Hilt module for license provider for $variantName"
      task.configureWith(project)

      // Depend on the base generate task
      val baseTaskName = "generate${suffix}LicenseCode"
      project.tasks.findByName(baseTaskName)?.let { baseTask ->
        task.dependsOn(baseTask)
      }
    }

    // Make compile and KSP tasks depend on this task
    project.tasks.matching {
      (it.name.contains("compile", ignoreCase = true) &&
        it.name.contains("Kotlin", ignoreCase = true)) ||
        (it.name.startsWith("ksp") && it.name.endsWith("Kotlin"))
    }.configureEach { compileTask ->
      compileTask.dependsOn(taskName)
    }
  }

  companion object {
    const val TASK_GROUP = "license-scribe"
  }
}
