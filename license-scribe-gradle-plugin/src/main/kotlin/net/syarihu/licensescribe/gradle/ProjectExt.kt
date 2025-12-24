package net.syarihu.licensescribe.gradle

import org.gradle.api.Project

/**
 * Shared constant for task group name.
 */
const val TASK_GROUP = "license-scribe"

/**
 * Registers source directories early for Kotlin compiler compatibility.
 * This ensures generated sources are recognized by the IDE and compiler.
 *
 * @param outputDirName The output directory name under build/generated/source/
 */
fun Project.registerVariantSourceDirectoriesEarly(outputDirName: String) {
  try {
    val androidExtension = extensions.findByName("android") ?: return
    val sourceSetsMethod = androidExtension.javaClass.getMethod("getSourceSets")
    val sourceSets = sourceSetsMethod.invoke(androidExtension)

    // Get build types from android extension
    val buildTypesMethod = androidExtension.javaClass.getMethod("getBuildTypes")
    val buildTypes = buildTypesMethod.invoke(androidExtension) as Iterable<*>
    val buildTypeNames = buildTypes.mapNotNull { buildType ->
      val nameMethod = buildType?.javaClass?.getMethod("getName")
      nameMethod?.invoke(buildType) as? String
    }

    // Get product flavors from android extension
    val productFlavorsMethod = androidExtension.javaClass.getMethod("getProductFlavors")
    val productFlavors = productFlavorsMethod.invoke(androidExtension) as Iterable<*>
    val flavorNames = productFlavors.mapNotNull { flavor ->
      val nameMethod = flavor?.javaClass?.getMethod("getName")
      nameMethod?.invoke(flavor) as? String
    }

    // Generate variant names (flavor + buildType combinations)
    val variantNames = if (flavorNames.isEmpty()) {
      // No flavors: variant names are just build type names
      buildTypeNames
    } else {
      // With flavors: variant names are flavor + BuildType (e.g., stagingDebug)
      flavorNames.flatMap { flavor ->
        buildTypeNames.map { buildType ->
          "$flavor${buildType.replaceFirstChar { it.uppercaseChar() }}"
        }
      }
    }

    val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)

    for (variantName in variantNames) {
      val outputDir = layout.buildDirectory.dir("generated/source/$outputDirName/$variantName")

      // Try to register to variant-specific source set first, fall back to buildType source set
      val sourceSetName = if (flavorNames.isEmpty()) {
        variantName // No flavors: use buildType name (debug, release)
      } else {
        // With flavors: try variant name first, but Android doesn't have variant source sets by default
        // Fall back to the build type portion
        val buildTypeName = buildTypeNames.find { variantName.endsWith(it, ignoreCase = true) }
        buildTypeName ?: variantName
      }

      try {
        val sourceSet = getByNameMethod.invoke(sourceSets, sourceSetName)
        val kotlinProperty = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet)
        val srcDirMethod = kotlinProperty.javaClass.getMethod("srcDir", Any::class.java)
        srcDirMethod.invoke(kotlinProperty, outputDir)
      } catch (e: Exception) {
        logger.debug("Could not register source directory for $sourceSetName: ${e.message}")
      }
    }
  } catch (e: Exception) {
    logger.debug("Could not register variant source directories early: ${e.message}")
  }
}
