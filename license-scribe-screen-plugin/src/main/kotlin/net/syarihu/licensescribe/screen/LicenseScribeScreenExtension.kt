package net.syarihu.licensescribe.screen

import org.gradle.api.provider.Property

/**
 * Extension for configuring the License Scribe Screen plugin.
 */
abstract class LicenseScribeScreenExtension {
  /**
   * Package name for generated code.
   * Defaults to project.group if set, otherwise "licenses".
   */
  abstract val generatedPackageName: Property<String>

  /**
   * Class name for the generated licenses object.
   * Defaults to "AppLicenses".
   */
  abstract val licensesClassName: Property<String>

  /**
   * Class name for the generated Activity.
   * Defaults to "OpenSourceLicensesActivity".
   */
  abstract val activityClassName: Property<String>
}
