package net.syarihu.licensescribe.resolver

import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.License
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.model.ResolvedLicense

/**
 * Resolves a LicenseCatalog into a list of ResolvedLicense instances.
 */
object LicenseCatalogResolver {

  /**
   * Convert a LicenseCatalog to a list of ResolvedLicense.
   *
   * @param catalog The license catalog to resolve
   * @return List of resolved licenses for all artifacts
   */
  fun resolve(catalog: LicenseCatalog): List<ResolvedLicense> {
    val resolvedLicenses = mutableListOf<ResolvedLicense>()

    catalog.licenses.forEach { (licenseKey, licenseEntry) ->
      val mainLicense = License(
        key = licenseKey,
        name = licenseEntry.name,
        url = licenseEntry.url,
      )

      licenseEntry.artifacts.forEach { (groupId, artifacts) ->
        artifacts.forEach { artifact ->
          // Resolve alternative licenses
          val alternativeLicenses = artifact.alternativeLicenses?.mapNotNull { altLicenseKey ->
            catalog.licenses[altLicenseKey]?.let { altLicenseEntry ->
              License(
                key = altLicenseKey,
                name = altLicenseEntry.name,
                url = altLicenseEntry.url,
              )
            }
          }

          // Resolve additional licenses
          val additionalLicenses = artifact.additionalLicenses?.mapNotNull { addLicenseKey ->
            catalog.licenses[addLicenseKey]?.let { addLicenseEntry ->
              License(
                key = addLicenseKey,
                name = addLicenseEntry.name,
                url = addLicenseEntry.url,
              )
            }
          }

          resolvedLicenses.add(
            ResolvedLicense(
              artifactId = ArtifactId(
                group = groupId,
                name = artifact.name,
              ),
              artifactName = artifact.name,
              artifactUrl = artifact.url,
              copyrightHolders = artifact.copyrightHolders,
              license = mainLicense,
              alternativeLicenses = alternativeLicenses?.takeIf { it.isNotEmpty() },
              additionalLicenses = additionalLicenses?.takeIf { it.isNotEmpty() },
            ),
          )
        }
      }
    }

    return resolvedLicenses
  }
}
