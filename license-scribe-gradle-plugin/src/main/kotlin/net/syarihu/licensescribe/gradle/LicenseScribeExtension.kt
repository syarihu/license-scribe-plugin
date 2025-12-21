package net.syarihu.licensescribe.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Extension for configuring the LicenseScribe plugin.
 */
abstract class LicenseScribeExtension
@Inject
constructor(
  objects: ObjectFactory,
) {
  /**
   * Directory where license management files are stored.
   * Defaults to the project directory.
   */
  abstract val baseDir: DirectoryProperty

  /**
   * Name of the artifact definitions file.
   * Defaults to "artifact-definitions.yml".
   */
  abstract val artifactDefinitionsFile: Property<String>

  /**
   * Name of the license catalog file.
   * Defaults to "license-catalog.yml".
   */
  abstract val licenseCatalogFile: Property<String>

  /**
   * Name of the artifact ignore file.
   * Defaults to ".artifactignore".
   */
  abstract val artifactIgnoreFile: Property<String>

  /**
   * Package name for generated Kotlin code.
   */
  abstract val generatedPackageName: Property<String>

  /**
   * Class name for generated Kotlin code.
   * Defaults to "Licenses".
   */
  abstract val generatedClassName: Property<String>

  init {
    artifactDefinitionsFile.convention("artifact-definitions.yml")
    licenseCatalogFile.convention("license-catalog.yml")
    artifactIgnoreFile.convention(".artifactignore")
    generatedClassName.convention("Licenses")
  }
}
