package net.syarihu.licensescribe.model

/**
 * Represents an artifact definition with its license information.
 */
data class ArtifactDefinition(
  val name: String,
  val url: String? = null,
  val copyrightHolder: String? = null,
  val license: String,
)

/**
 * Represents a group of artifacts under the same group ID.
 */
data class ArtifactGroup(
  val groupId: String,
  val artifacts: List<ArtifactDefinition>,
)

/**
 * Represents artifact definitions organized by scope.
 */
data class ScopedArtifacts(
  val scope: String,
  val groups: List<ArtifactGroup>,
)
