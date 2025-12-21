package net.syarihu.licensescribe.model

/**
 * Represents a Maven artifact identifier.
 */
data class ArtifactId(
  val group: String,
  val name: String,
  val version: String? = null,
) {
  val coordinate: String
    get() = if (version != null) "$group:$name:$version" else "$group:$name"

  companion object {
    fun parse(coordinate: String): ArtifactId {
      val parts = coordinate.split(":")
      return when (parts.size) {
        2 -> ArtifactId(parts[0], parts[1])
        3 -> ArtifactId(parts[0], parts[1], parts[2])
        else -> throw IllegalArgumentException("Invalid coordinate: $coordinate")
      }
    }
  }
}
