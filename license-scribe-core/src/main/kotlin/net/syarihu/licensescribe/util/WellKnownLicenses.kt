package net.syarihu.licensescribe.util

/**
 * Well-known license information for supplementing detected licenses.
 */
object WellKnownLicenses {

  /**
   * Map of license keys to (name, url) pairs.
   */
  val licenses: Map<String, Pair<String, String?>> = mapOf(
    "apache-2.0" to ("Apache License 2.0" to "https://www.apache.org/licenses/LICENSE-2.0"),
    "mit" to ("MIT License" to "https://opensource.org/licenses/MIT"),
    "bsd-3-clause" to ("BSD 3-Clause License" to "https://opensource.org/licenses/BSD-3-Clause"),
    "bsd-2-clause" to ("BSD 2-Clause License" to "https://opensource.org/licenses/BSD-2-Clause"),
    "lgpl-2.1" to (
      "GNU Lesser General Public License v2.1" to
        "https://www.gnu.org/licenses/lgpl-2.1.html"
      ),
    "lgpl-3.0" to (
      "GNU Lesser General Public License v3.0" to
        "https://www.gnu.org/licenses/lgpl-3.0.html"
      ),
    "epl-1.0" to ("Eclipse Public License 1.0" to "https://www.eclipse.org/legal/epl-v10.html"),
    "mpl-2.0" to ("Mozilla Public License 2.0" to "https://www.mozilla.org/en-US/MPL/2.0/"),
    "gpl-2.0" to (
      "GNU General Public License v2.0" to
        "https://www.gnu.org/licenses/gpl-2.0.html"
      ),
    "gpl-3.0" to (
      "GNU General Public License v3.0" to
        "https://www.gnu.org/licenses/gpl-3.0.html"
      ),
    "cc0-1.0" to ("CC0 1.0 Universal" to "https://creativecommons.org/publicdomain/zero/1.0/"),
    "unlicense" to ("The Unlicense" to "https://unlicense.org/"),
    "isc" to ("ISC License" to "https://opensource.org/licenses/ISC"),
    "unknown" to ("Unknown License" to null),
  )

  /**
   * Default copyright holders for well-known licenses.
   * Used when POM files don't include developer information.
   */
  val copyrightHolders: Map<String, List<String>> = mapOf(
    "android-software-development-kit-license" to listOf("Google LLC"),
  )

  /**
   * Supplements existing license entries with well-known names and URLs.
   * Only updates licenses that already exist in the map (detected from dependencies).
   */
  fun supplementLicenseInfo(licenseInfoMap: MutableMap<String, Pair<String, String?>>) {
    // Only supplement info for licenses that already exist
    licenseInfoMap.keys.toList().forEach { key ->
      licenses[key]?.let { (name, url) ->
        val existing = licenseInfoMap[key]
        if (existing != null) {
          val currentUrl = existing.second
          licenseInfoMap[key] = name to (url ?: currentUrl)
        }
      }
    }
  }

  /**
   * Returns default copyright holders for a given license key.
   * Used when POM files don't include developer information.
   */
  fun getDefaultCopyrightHolders(licenseKey: String): List<String> = copyrightHolders[licenseKey] ?: emptyList()
}
