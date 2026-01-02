package net.syarihu.licensescribe.model

import java.io.Serializable

/**
 * Represents parsed POM information for a Maven artifact.
 */
data class PomInfo(
  val name: String?,
  val url: String?,
  val licenses: List<PomLicense>,
  val developers: List<String>,
  val parentGroupId: String? = null,
  val parentArtifactId: String? = null,
  val parentVersion: String? = null,
) : Serializable

/**
 * Represents a license entry from a POM file.
 */
data class PomLicense(
  val name: String,
  val url: String?,
) : Serializable
