package net.syarihu.licensescribe.hilt

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

/**
 * Generates Hilt module for providing LicenseProvider.
 */
class HiltModuleGenerator {
  companion object {
    private const val CORE_PACKAGE = "net.syarihu.licensescribe"
    private const val HILT_PACKAGE = "dagger.hilt"
    private const val DAGGER_PACKAGE = "dagger"
  }

  fun generate(
    packageName: String,
    licensesClassName: String,
    outputDir: File,
  ) {
    val moduleName = "LicenseScribeHiltModule"
    val licenseProviderClassName = ClassName(CORE_PACKAGE, "LicenseProvider")
    val licensesObjectClassName = ClassName(packageName, licensesClassName)

    // Hilt annotations
    val moduleAnnotation = AnnotationSpec.builder(ClassName(DAGGER_PACKAGE, "Module")).build()
    val installInAnnotation = AnnotationSpec
      .builder(ClassName("$HILT_PACKAGE.components", "SingletonComponent"))
      .let { builder ->
        AnnotationSpec
          .builder(ClassName(HILT_PACKAGE, "InstallIn"))
          .addMember("%T::class", ClassName("$HILT_PACKAGE.components", "SingletonComponent"))
          .build()
      }
    val providesAnnotation = AnnotationSpec.builder(ClassName(DAGGER_PACKAGE, "Provides")).build()
    val singletonAnnotation = AnnotationSpec.builder(ClassName("javax.inject", "Singleton")).build()

    val provideFunction = FunSpec
      .builder("provideLicenseProvider")
      .addAnnotation(providesAnnotation)
      .addAnnotation(singletonAnnotation)
      .returns(licenseProviderClassName)
      .addStatement("return %T", licensesObjectClassName)
      .build()

    val moduleObject = TypeSpec
      .objectBuilder(moduleName)
      .addAnnotation(moduleAnnotation)
      .addAnnotation(installInAnnotation)
      .addFunction(provideFunction)
      .build()

    FileSpec
      .builder(packageName, moduleName)
      .addImport(CORE_PACKAGE, "LicenseProvider")
      .addType(moduleObject)
      .build()
      .writeTo(outputDir)
  }
}
