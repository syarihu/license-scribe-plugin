package net.syarihu.licensescribe.gradle

import com.android.build.api.variant.AndroidComponentsExtension
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
      // Register source directory early (before afterEvaluate)
      registerAndroidSourceDirectory(project, extension)
      setupAndroidTasks(project, extension, androidComponentsExtension)
    } else {
      // Non-Android project
      project.afterEvaluate {
        registerTasksForConfiguration(project, extension, "", getResolvableConfiguration(project))
      }
    }
  }

  private fun registerAndroidSourceDirectory(
    project: Project,
    extension: LicenseScribeExtension,
  ) {
    val outputDir = project.layout.buildDirectory.dir("generated/source/licenseScribe")

    try {
      val androidExtension = project.extensions.findByName("android")
      if (androidExtension != null) {
        val sourceSetsMethod = androidExtension.javaClass.getMethod("getSourceSets")
        val sourceSets = sourceSetsMethod.invoke(androidExtension)
        val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
        val mainSourceSet = getByNameMethod.invoke(sourceSets, "main")
        val kotlinProperty = mainSourceSet.javaClass.getMethod("getKotlin").invoke(mainSourceSet)
        val srcDirMethod = kotlinProperty.javaClass.getMethod("srcDir", Any::class.java)
        srcDirMethod.invoke(kotlinProperty, outputDir)
      }
    } catch (e: Exception) {
      project.logger.debug("Could not register Android source directory: ${e.message}")
    }
  }

  private fun setupAndroidTasks(
    project: Project,
    extension: LicenseScribeExtension,
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>,
  ) {
    androidComponentsExtension.onVariants { variant ->
      val configuration = getAndroidConfiguration(project, variant.name)
      registerTasksForConfiguration(project, extension, variant.name, configuration)
    }
  }

  private fun registerTasksForConfiguration(
    project: Project,
    extension: LicenseScribeExtension,
    variantName: String,
    configuration: Configuration?,
  ) {
    val suffix = variantName.replaceFirstChar { it.uppercaseChar() }

    project.tasks.register("init${suffix}Licenses", InitLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Initialize license management files for $variantName"
      task.configureWith(extension, configuration)
    }

    project.tasks.register("check${suffix}Licenses", CheckLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Check license definitions for $variantName"
      task.configureWith(extension, configuration)
    }

    project.tasks.register("sync${suffix}Licenses", SyncLicensesTask::class.java) { task ->
      task.group = TASK_GROUP
      task.description = "Sync license definitions with current dependencies for $variantName"
      task.configureWith(extension, configuration)
    }

    val generateTask =
      project.tasks.register("generate${suffix}LicenseCode", GenerateLicenseCodeTask::class.java) { task ->
        task.group = TASK_GROUP
        task.description = "Generate Kotlin code for licenses for $variantName"
        task.configureWith(extension, configuration, variantName, project)
      }

    // Register generated source directory and task dependency
    registerGeneratedSourceDirectory(project, variantName, generateTask)
  }

  private fun registerGeneratedSourceDirectory(
    project: Project,
    variantName: String,
    generateTask: TaskProvider<GenerateLicenseCodeTask>,
  ) {
    // For non-Android projects, add to Kotlin source sets
    if (project.extensions.findByName("android") == null) {
      registerKotlinSourceDirectory(project, generateTask)
    }

    // Make ALL compile tasks depend on this generate task
    // This is necessary because all variants share the same output directory
    project.tasks.matching {
      it.name.contains("compile", ignoreCase = true) &&
        it.name.contains("Kotlin", ignoreCase = true)
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

      // Make compile task depend on generate task
      project.tasks.matching { it.name.contains("compile", ignoreCase = true) && it.name.contains("Kotlin", ignoreCase = true) }
        .configureEach { compileTask ->
          compileTask.dependsOn(generateTask)
        }
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
