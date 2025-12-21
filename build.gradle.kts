plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.spotless)
}

val ktlintVersion = libs.versions.ktlint.get()

// Check if git repository exists for ratchetFrom
val isGitRepository = rootDir.resolve(".git").exists()

allprojects {
  apply(plugin = "com.diffplug.spotless")

  configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    if (isGitRepository) {
      ratchetFrom("origin/main")
    }
    kotlin {
      target("**/*.kt")
      targetExclude("**/build/**/*.kt")
      targetExclude("bin/**/*.kt")
      targetExclude("**/generated/**/*.kt")
      ktlint(ktlintVersion)
        .editorConfigOverride(
          mapOf(
            "android" to "true",
            "experimental" to "true",
            "indent_size" to "2",
            "continuation_indent_size" to "2",
            "trim_trailing_whitespace" to "true",
            "max_line_length" to "140",
            "insert_final_newline" to "true",
            "ij_kotlin_allow_trailing_comma" to "true",
            "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
            "ktlint_standard_import-ordering" to "disabled",
            "ktlint_standard_function-signature" to "enabled",
            "ktlint_standard_function-naming" to "disabled",
            "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "2",
            "ktlint_standard_indent" to "enabled",
          ),
        )
    }
    kotlinGradle {
      target("**/*.gradle.kts")
      targetExclude("**/build/**")
      ktlint(ktlintVersion)
        .editorConfigOverride(
          mapOf(
            "android" to "true",
            "experimental" to "true",
            "indent_size" to "2",
            "continuation_indent_size" to "2",
            "trim_trailing_whitespace" to "true",
            "max_line_length" to "140",
            "insert_final_newline" to "true",
            "ij_kotlin_allow_trailing_comma" to "true",
            "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
            "ktlint_standard_import-ordering" to "disabled",
            "ktlint_standard_function-signature" to "enabled",
            "ktlint_standard_function-naming" to "disabled",
            "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "2",
            "ktlint_standard_indent" to "enabled",
          ),
        )
    }
  }
}
