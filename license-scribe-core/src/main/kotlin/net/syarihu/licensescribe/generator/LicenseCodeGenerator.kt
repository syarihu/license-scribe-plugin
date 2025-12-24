package net.syarihu.licensescribe.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import net.syarihu.licensescribe.model.ResolvedLicense
import java.io.File

/**
 * Generates Kotlin code for license information.
 */
class LicenseCodeGenerator {
  companion object {
    private const val CORE_PACKAGE = "net.syarihu.licensescribe"
  }

  fun generate(
    licenses: List<ResolvedLicense>,
    packageName: String,
    className: String,
    outputDir: File,
  ) {
    val licensesObject = createLicensesObject(licenses, className)

    // Write Licenses object (implements LicenseProvider from core)
    FileSpec
      .builder(packageName, className)
      .addImport(CORE_PACKAGE, "LicenseInfo")
      .addImport(CORE_PACKAGE, "LicenseProvider")
      .addImport(CORE_PACKAGE, "AlternativeLicenseInfo")
      .addImport(CORE_PACKAGE, "AdditionalLicenseInfo")
      .addType(licensesObject)
      .build()
      .writeTo(outputDir)
  }

  private fun createLicensesObject(
    licenses: List<ResolvedLicense>,
    className: String,
  ): TypeSpec {
    val licenseInfoClassName = ClassName(CORE_PACKAGE, "LicenseInfo")
    val licenseProviderClassName = ClassName(CORE_PACKAGE, "LicenseProvider")
    val listType = List::class.asClassName().parameterizedBy(licenseInfoClassName)

    val listInitializer = buildListInitializer(licenses, licenseInfoClassName)

    return TypeSpec
      .objectBuilder(className)
      .addSuperinterface(licenseProviderClassName)
      .addProperty(
        PropertySpec
          .builder("all", listType)
          .addModifiers(KModifier.OVERRIDE)
          .initializer(listInitializer)
          .build(),
      ).addFunction(
        FunSpec
          .builder("findByArtifactId")
          .addParameter("id", String::class)
          .returns(licenseInfoClassName.copy(nullable = true))
          .addStatement("return all.find { it.artifactId == id }")
          .build(),
      ).addFunction(
        FunSpec
          .builder("findByLicenseName")
          .addParameter("name", String::class)
          .returns(listType)
          .addStatement("return all.filter { it.licenseName == name }")
          .build(),
      ).build()
  }

  private fun buildListInitializer(
    licenses: List<ResolvedLicense>,
    licenseInfoClassName: ClassName,
  ): CodeBlock {
    if (licenses.isEmpty()) {
      return CodeBlock.of("listOf()")
    }

    val sortedLicenses = licenses.sortedBy { it.artifactId.coordinate }
    val alternativeLicenseInfoClassName = ClassName(CORE_PACKAGE, "AlternativeLicenseInfo")
    val additionalLicenseInfoClassName = ClassName(CORE_PACKAGE, "AdditionalLicenseInfo")

    return CodeBlock.builder()
      .add("listOf(\n")
      .indent()
      .apply {
        sortedLicenses.forEachIndexed { index, license ->
          add("%T(\n", licenseInfoClassName)
          indent()
          add("artifactId = %S,\n", license.artifactId.coordinate)
          add("artifactName = %S,\n", license.artifactName)
          if (license.artifactUrl != null) {
            add("artifactUrl = %S,\n", license.artifactUrl)
          } else {
            add("artifactUrl = null,\n")
          }
          // copyrightHolders
          if (license.copyrightHolders.isEmpty()) {
            add("copyrightHolders = emptyList(),\n")
          } else {
            add("copyrightHolders = listOf(")
            license.copyrightHolders.forEachIndexed { i, holder ->
              add("%S", holder)
              if (i < license.copyrightHolders.size - 1) add(", ")
            }
            add("),\n")
          }
          add("licenseName = %S,\n", license.license.name)
          if (license.license.url != null) {
            add("licenseUrl = %S,\n", license.license.url)
          } else {
            add("licenseUrl = null,\n")
          }
          // alternativeLicenses
          if (license.alternativeLicenses != null && license.alternativeLicenses.isNotEmpty()) {
            add("alternativeLicenses = listOf(\n")
            indent()
            license.alternativeLicenses.forEachIndexed { i, alt ->
              add(
                "%T(licenseName = %S, licenseUrl = %S)",
                alternativeLicenseInfoClassName,
                alt.name,
                alt.url,
              )
              if (i < license.alternativeLicenses.size - 1) add(",")
              add("\n")
            }
            unindent()
            add("),\n")
          }
          // additionalLicenses
          if (license.additionalLicenses != null && license.additionalLicenses.isNotEmpty()) {
            add("additionalLicenses = listOf(\n")
            indent()
            license.additionalLicenses.forEachIndexed { i, add ->
              add(
                "%T(licenseName = %S, licenseUrl = %S)",
                additionalLicenseInfoClassName,
                add.name,
                add.url,
              )
              if (i < license.additionalLicenses.size - 1) add(",")
              add("\n")
            }
            unindent()
            add("),\n")
          }
          unindent()
          add("),\n")
        }
      }
      .unindent()
      .add(")")
      .build()
  }
}
