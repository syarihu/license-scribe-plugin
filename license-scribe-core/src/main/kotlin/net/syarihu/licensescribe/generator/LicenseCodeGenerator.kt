package net.syarihu.licensescribe.generator

import com.squareup.kotlinpoet.ClassName
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

    val listInitializer =
      if (licenses.isEmpty()) {
        "listOf()"
      } else {
        buildString {
          appendLine("listOf(")
          licenses.sortedBy { it.artifactId.coordinate }.forEachIndexed { index, license ->
            append("    LicenseInfo(")
            append("artifactId = ${license.artifactId.coordinate.toKotlinString()}, ")
            append("artifactName = ${license.artifactName.toKotlinString()}, ")
            append("artifactUrl = ${license.artifactUrl.toNullableKotlinString()}, ")
            append("copyrightHolder = ${license.copyrightHolder.toNullableKotlinString()}, ")
            append("licenseName = ${license.license.name.toKotlinString()}, ")
            append("licenseUrl = ${license.license.url.toNullableKotlinString()}")
            append(")")
            if (index < licenses.size - 1) {
              appendLine(",")
            } else {
              appendLine()
            }
          }
          append(")")
        }
      }

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
          .addParameter("artifactId", String::class)
          .returns(licenseInfoClassName.copy(nullable = true))
          .addStatement("return all.find { it.artifactId == artifactId }")
          .build(),
      ).addFunction(
        FunSpec
          .builder("findByLicenseName")
          .addParameter("licenseName", String::class)
          .returns(listType)
          .addStatement("return all.filter { it.licenseName == licenseName }")
          .build(),
      ).build()
  }

  private fun String.toKotlinString(): String = "\"${this.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""

  private fun String?.toNullableKotlinString(): String = this?.toKotlinString() ?: "null"
}
