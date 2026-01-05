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

  /**
   * Night mode for the generated Activity.
   * - "followSystem": Follow system dark mode setting (default)
   * - "yes": Always use dark mode
   * - "no": Always use light mode
   */
  abstract val nightMode: Property<String>
}
