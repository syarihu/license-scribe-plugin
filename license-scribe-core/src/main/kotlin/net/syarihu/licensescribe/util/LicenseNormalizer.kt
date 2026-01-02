package net.syarihu.licensescribe.util

/**
 * Utility object for normalizing license information.
 */
object LicenseNormalizer {

  private val AMBIGUOUS_LICENSE_NAMES = setOf(
    "license",
    "licence",
    "the license",
    "the licence",
    "see license",
    "see licence",
  )

  private val WELL_KNOWN_LICENSE_URL_PATTERNS = listOf(
    "apache.org/licenses",
    "opensource.org/licenses",
    "gnu.org/licenses",
    "eclipse.org/legal",
    "mozilla.org",
    "creativecommons.org",
    "unlicense.org",
  )

  /**
   * Normalizes a license name and URL to a standard license key (e.g., "apache-2.0", "mit").
   * First tries to identify the license from the URL (more reliable), then from the name.
   */
  fun normalizeKey(
    licenseName: String,
    licenseUrl: String? = null,
  ): String {
    // First, try to identify license from URL (more reliable)
    licenseUrl?.let { url ->
      val urlLower = url.lowercase()
      when {
        urlLower.contains("apache.org/licenses/license-2.0") ||
          urlLower.contains("apache-2.0") -> return "apache-2.0"
        urlLower.contains("opensource.org/licenses/mit") ||
          urlLower.contains("mit-license") -> return "mit"
        urlLower.contains("opensource.org/licenses/bsd-3-clause") ||
          urlLower.contains("bsd-3-clause") -> return "bsd-3-clause"
        urlLower.contains("opensource.org/licenses/bsd-2-clause") ||
          urlLower.contains("bsd-2-clause") -> return "bsd-2-clause"
        urlLower.contains("gnu.org/licenses/lgpl-3") ||
          urlLower.contains("lgpl-3.0") -> return "lgpl-3.0"
        urlLower.contains("gnu.org/licenses/lgpl-2.1") ||
          urlLower.contains("lgpl-2.1") -> return "lgpl-2.1"
        urlLower.contains("gnu.org/licenses/gpl-3") ||
          urlLower.contains("gpl-3.0") -> return "gpl-3.0"
        urlLower.contains("gnu.org/licenses/gpl-2") ||
          urlLower.contains("gpl-2.0") -> return "gpl-2.0"
        urlLower.contains("eclipse.org/legal/epl") -> return "epl-1.0"
        urlLower.contains("mozilla.org") && urlLower.contains("mpl") -> return "mpl-2.0"
        urlLower.contains("creativecommons.org/publicdomain/zero") -> return "cc0-1.0"
        urlLower.contains("unlicense.org") -> return "unlicense"
        urlLower.contains("opensource.org/licenses/isc") -> return "isc"
      }
    }

    // Then, try to identify from license name
    val lower = licenseName.lowercase()
    return when {
      lower.contains("apache") && lower.contains("2") -> "apache-2.0"
      lower.contains("mit") -> "mit"
      lower.contains("bsd") && lower.contains("3") -> "bsd-3-clause"
      lower.contains("bsd") && lower.contains("2") -> "bsd-2-clause"
      lower.contains("bsd") -> "bsd"
      lower.contains("lgpl") && lower.contains("3") -> "lgpl-3.0"
      lower.contains("lgpl") && lower.contains("2.1") -> "lgpl-2.1"
      lower.contains("gpl") && lower.contains("3") -> "gpl-3.0"
      lower.contains("gpl") && lower.contains("2") -> "gpl-2.0"
      lower.contains("eclipse") || lower.contains("epl") -> "epl-1.0"
      lower.contains("mozilla") || lower.contains("mpl") -> "mpl-2.0"
      lower.contains("creative commons") && lower.contains("zero") -> "cc0-1.0"
      lower.contains("unlicense") -> "unlicense"
      lower.contains("isc") -> "isc"
      // Proprietary licenses should be kept separate per vendor
      lower == "proprietary" -> {
        val vendorSuffix = extractVendorFromUrl(licenseUrl)
        if (vendorSuffix != null) "proprietary-$vendorSuffix" else "proprietary"
      }
      // Ambiguous license names should be kept separate per vendor
      isAmbiguousLicenseName(lower) -> {
        val vendorSuffix = extractVendorFromUrl(licenseUrl)
        val baseName = lower.replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "unknown" }
        if (vendorSuffix != null) "$baseName-$vendorSuffix" else baseName
      }
      else ->
        licenseName
          .lowercase()
          .replace(Regex("[^a-z0-9]+"), "-")
          .trim('-')
    }
  }

  /**
   * Strips version information from a URL (e.g., version anchors or path segments).
   */
  fun stripVersionFromUrl(url: String): String = url
    .replace(Regex("#[\\d.]+$"), "") // Remove #1.7.6 style version anchors
    .replace(Regex("/[\\d.]+/?$"), "") // Remove /1.7.6 style version paths

  /**
   * Checks if a license name/URL combination is ambiguous and requires manual verification.
   * Returns true for licenses like "LICENSE", "LICENCE", or URLs pointing to repository LICENSE files.
   */
  fun isAmbiguousLicense(
    licenseName: String,
    licenseUrl: String?,
  ): Boolean {
    val nameLower = licenseName.lowercase().trim()

    // Check for generic license names that don't identify the actual license type
    if (nameLower in AMBIGUOUS_LICENSE_NAMES) {
      return true
    }

    // Check if URL points to a repository LICENSE file (not a known license URL)
    licenseUrl?.let { url ->
      val urlLower = url.lowercase()
      // GitHub/GitLab/Bitbucket LICENSE file patterns
      if (urlLower.contains("/license") &&
        (
          urlLower.contains("github.com") ||
            urlLower.contains("gitlab.com") ||
            urlLower.contains("bitbucket.org")
          )
      ) {
        // But not if it's a well-known license URL pattern
        if (WELL_KNOWN_LICENSE_URL_PATTERNS.none { urlLower.contains(it) }) {
          return true
        }
      }
    }

    return false
  }

  /**
   * Checks if a license name is ambiguous and doesn't identify a specific license type.
   */
  private fun isAmbiguousLicenseName(lowerName: String): Boolean = lowerName in AMBIGUOUS_LICENSE_NAMES

  /**
   * Extracts a vendor identifier from a URL for creating unique license keys.
   * Returns the domain name or GitHub org/repo as identifier.
   */
  private fun extractVendorFromUrl(url: String?): String? {
    if (url == null) return null
    return try {
      val urlLower = url.lowercase()
      when {
        // GitHub: extract org name
        urlLower.contains("github.com") -> {
          val match = Regex("github\\.com/([^/]+)").find(urlLower)
          match?.groupValues?.get(1)
        }
        // GitLab: extract org name
        urlLower.contains("gitlab.com") -> {
          val match = Regex("gitlab\\.com/([^/]+)").find(urlLower)
          match?.groupValues?.get(1)
        }
        // Other URLs: extract domain without TLD
        else -> {
          val match = Regex("://(?:www\\.)?([^./]+)").find(urlLower)
          match?.groupValues?.get(1)
        }
      }
    } catch (_: Exception) {
      // Safe to ignore - null triggers fallback in caller
      null
    }
  }
}
