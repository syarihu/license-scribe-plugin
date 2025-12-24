package net.syarihu.licensescribe.gradle.task.model

import java.io.Serializable

/**
 * Serializable version of PomInfo for Configuration Cache compatibility.
 */
@Suppress("serial")
data class SerializablePomInfo(
  val name: String?,
  val url: String?,
  val licenses: List<SerializablePomLicense>,
  val developers: List<String>,
) : Serializable {
  fun toPomInfo(): PomInfo = PomInfo(
    name = name,
    url = url,
    licenses = licenses.map { PomLicense(it.name, it.url) },
    developers = developers,
  )
}
