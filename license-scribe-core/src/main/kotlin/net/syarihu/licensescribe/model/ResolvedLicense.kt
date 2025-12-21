package net.syarihu.licensescribe.model

/**
 * Represents a fully resolved license for an artifact,
 * with all information needed for display or code generation.
 */
data class ResolvedLicense(
  val artifactId: ArtifactId,
  val artifactName: String,
  val artifactUrl: String?,
  val copyrightHolder: String?,
  val license: License,
)
