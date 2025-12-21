package net.syarihu.licensescribe

/**
 * Interface for providing license information.
 * Generated code implements this interface, allowing dependency injection
 * in multi-module projects.
 */
interface LicenseProvider {
  /**
   * Returns all license information for the project's dependencies.
   */
  val all: List<LicenseInfo>
}
