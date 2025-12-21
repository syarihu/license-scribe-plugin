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
  fun generate(
    licenses: List<ResolvedLicense>,
    packageName: String,
    className: String,
    outputDir: File,
  ) {
    val licenseInfoClass = createLicenseInfoDataClass(packageName)
    val licensesObject = createLicensesObject(licenses, packageName, className)

    // Write LicenseInfo data class
    FileSpec
      .builder(packageName, "LicenseInfo")
      .addType(licenseInfoClass)
      .build()
      .writeTo(outputDir)

    // Write Licenses object
    FileSpec
      .builder(packageName, className)
      .addType(licensesObject)
      .build()
      .writeTo(outputDir)
  }

  private fun createLicenseInfoDataClass(packageName: String): TypeSpec = TypeSpec
    .classBuilder("LicenseInfo")
    .addModifiers(KModifier.DATA)
    .primaryConstructor(
      FunSpec
        .constructorBuilder()
        .addParameter("artifactId", String::class)
        .addParameter("artifactName", String::class)
        .addParameter("artifactUrl", String::class.asClassName().copy(nullable = true))
        .addParameter("copyrightHolder", String::class.asClassName().copy(nullable = true))
        .addParameter("licenseName", String::class)
        .addParameter("licenseUrl", String::class.asClassName().copy(nullable = true))
        .build(),
    ).addProperty(
      PropertySpec
        .builder("artifactId", String::class)
        .initializer("artifactId")
        .build(),
    ).addProperty(
      PropertySpec
        .builder("artifactName", String::class)
        .initializer("artifactName")
        .build(),
    ).addProperty(
      PropertySpec
        .builder("artifactUrl", String::class.asClassName().copy(nullable = true))
        .initializer("artifactUrl")
        .build(),
    ).addProperty(
      PropertySpec
        .builder("copyrightHolder", String::class.asClassName().copy(nullable = true))
        .initializer("copyrightHolder")
        .build(),
    ).addProperty(
      PropertySpec
        .builder("licenseName", String::class)
        .initializer("licenseName")
        .build(),
    ).addProperty(
      PropertySpec
        .builder("licenseUrl", String::class.asClassName().copy(nullable = true))
        .initializer("licenseUrl")
        .build(),
    ).build()

  private fun createLicensesObject(
    licenses: List<ResolvedLicense>,
    packageName: String,
    className: String,
  ): TypeSpec {
    val licenseInfoClassName = ClassName(packageName, "LicenseInfo")
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
      .addProperty(
        PropertySpec
          .builder("all", listType)
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
