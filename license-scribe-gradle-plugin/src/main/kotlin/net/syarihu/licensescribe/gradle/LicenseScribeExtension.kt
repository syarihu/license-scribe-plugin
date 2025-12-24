package net.syarihu.licensescribe.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/**
 * Extension for configuring the LicenseScribe plugin.
 */
abstract class LicenseScribeExtension {
  /**
   * Directory where license management files are stored.
   * Defaults to the project directory.
   */
  abstract val baseDir: DirectoryProperty

  /**
   * Name of the licenses file that defines licenses and their artifacts.
   * Defaults to "scribe-licenses.yml".
   */
  abstract val licensesFile: Property<String>

  /**
   * Name of the ignore file for excluding artifacts.
   * Defaults to ".scribeignore".
   */
  abstract val ignoreFile: Property<String>

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
    licensesFile.convention("scribe-licenses.yml")
    ignoreFile.convention(".scribeignore")
    generatedClassName.convention("Licenses")
  }
}
