package net.syarihu.licensescribe.gradle.task.model

import java.io.Serializable

/**
 * Serializable version of PomLicense for Configuration Cache compatibility.
 */
@Suppress("serial")
data class SerializablePomLicense(
  val name: String,
  val url: String?,
) : Serializable
