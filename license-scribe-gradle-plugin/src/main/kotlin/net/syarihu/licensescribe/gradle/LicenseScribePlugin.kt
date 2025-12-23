package net.syarihu.licensescribe.gradle

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.Variant
import net.syarihu.licensescribe.gradle.task.CheckLicensesTask
import net.syarihu.licensescribe.gradle.task.GenerateLicenseCodeTask
import net.syarihu.licensescribe.gradle.task.InitLicensesTask
import net.syarihu.licensescribe.gradle.task.SyncLicensesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider

/**
 * Gradle plugin for managing licenses of project dependencies.
 */
class LicenseScribePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension =
      project.extensions.create(
        "licenseScribe",
        LicenseScribeExtension::class.java,
      )

    // Set default base directory
    extension.baseDir.convention(project.layout.projectDirectory)
    extension.generatedPackageName.convention(
      project.provider {
        project.group.toString().takeIf { it.isNotBlank() } ?: "licenses"
      },
    )

    // Try to setup for Android project using AGP API
    val androidComponentsExtension = project.extensions.findByType(AndroidComponentsExtension::class.java)
    if (androidComponentsExtension != null) {
      // Register source directories early for Kotlin compiler compatibility
      registerBuildTypeSourceDirectoriesEarly(project)
      setupAndroidTasks(project, extension, androidComponentsExtension)
    } else {
      // Non-Android project
      project.afterEvaluate {
        registerTasksForNonAndroid(project, extension, getResolvableConfiguration(project))
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
    extension: LicenseScribeExtension,
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>,
  ) {
    androidComponentsExtension.onVariants { variant ->
      val configuration = getAndroidConfiguration(project, variant.name)
      // Use buildType name for output directory to match source set registration
      val buildTypeName = variant.buildType ?: variant.name
      registerTasksForConfiguration(project, extension, variant.name, buildTypeName, configuration, variant)
    }
  }

  private fun registerTasksForConfiguration(
    project: Project,
    extension: LicenseScribeExtension,
    variantName: String,
    buildTypeName: String,
    configuration: Configuration?,
    variant: Variant,
  ) {
    val suffix = variantName.replaceFirstChar { it.uppercaseChar() }

    project.tasks.register("scribeLicenses${suffix}Init", InitLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Initialize license management files for $variantName"
      task.configureWith(extension, configuration, buildTypeName)
    }

    project.tasks.register("scribeLicenses${suffix}Check", CheckLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Check license definitions for $variantName"
      task.configureWith(extension, configuration, buildTypeName)
    }

    project.tasks.register("scribeLicenses${suffix}Sync", SyncLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Sync license definitions with current dependencies for $variantName"
      task.configureWith(extension, configuration, buildTypeName)
    }

    val generateTask =
      project.tasks.register("scribeLicenses${suffix}Generate", GenerateLicenseCodeTask::class.java) { task ->
        task.group = TASK_GROUP
        task.description = "Generate Kotlin code for licenses for $variantName"
        // Use buildType name for output directory to match source set registration
        task.configureWith(extension, configuration, buildTypeName, project)
      }

    // Use AGP Variant API to register source directory for IDE integration
    // Use .java instead of .kotlin for proper IDE recognition as generated sources root
    variant.sources.java?.addGeneratedSourceDirectory(
      generateTask,
      GenerateLicenseCodeTask::outputDirectory,
    )

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
      compileTask.dependsOn(generateTask)
    }
  }

  private fun registerTasksForNonAndroid(
    project: Project,
    extension: LicenseScribeExtension,
    configuration: Configuration?,
  ) {
    project.tasks.register("scribeLicensesInit", InitLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Initialize license management files"
      task.configureWith(extension, configuration, "")
    }

    project.tasks.register("scribeLicensesCheck", CheckLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Check license definitions"
      task.configureWith(extension, configuration, "")
    }

    project.tasks.register("scribeLicensesSync", SyncLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Sync license definitions with current dependencies"
      task.configureWith(extension, configuration, "")
    }

    val generateTask =
      project.tasks.register("scribeLicensesGenerate", GenerateLicenseCodeTask::class.java) { task ->
        task.group = TASK_GROUP
        task.description = "Generate Kotlin code for licenses"
        task.configureWith(extension, configuration, "", project)
      }

    // For non-Android projects, add to Kotlin source sets
    registerKotlinSourceDirectory(project, generateTask)

    // Make compile and KSP tasks depend on generate task
    project.tasks.matching {
      (it.name.contains("compile", ignoreCase = true) && it.name.contains("Kotlin", ignoreCase = true)) ||
        (it.name.startsWith("ksp") && it.name.endsWith("Kotlin"))
    }.configureEach { compileTask ->
      compileTask.dependsOn(generateTask)
    }
  }

  private fun registerKotlinSourceDirectory(
    project: Project,
    generateTask: TaskProvider<GenerateLicenseCodeTask>,
  ) {
    try {
      val kotlinExtension = project.extensions.findByName("kotlin") ?: return
      val sourceSetsMethod = kotlinExtension.javaClass.getMethod("getSourceSets")
      val sourceSets = sourceSetsMethod.invoke(kotlinExtension)
      val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
      val sourceSet = getByNameMethod.invoke(sourceSets, "main")
      val kotlinProperty = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet)
      val srcDirMethod = kotlinProperty.javaClass.getMethod("srcDir", Any::class.java)
      srcDirMethod.invoke(kotlinProperty, generateTask.flatMap { it.outputDirectory })
    } catch (e: Exception) {
      project.logger.debug("Could not register Kotlin source directory: ${e.message}")
    }
  }

  private fun getResolvableConfiguration(project: Project): Configuration? = project.configurations.findByName("runtimeClasspath")
    ?: project.configurations.findByName("compileClasspath")

  private fun getAndroidConfiguration(
    project: Project,
    variant: String,
  ): Configuration? = project.configurations.findByName("${variant}RuntimeClasspath")
    ?: project.configurations.findByName("${variant}CompileClasspath")

  companion object {
    const val TASK_GROUP = "license-scribe"
  }
}
