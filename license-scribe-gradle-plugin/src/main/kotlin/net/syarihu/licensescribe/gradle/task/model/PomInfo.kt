package net.syarihu.licensescribe.gradle.task.model

/**
 * Represents metadata extracted from a POM file.
 */
data class PomInfo(
  val name: String?,
  val url: String?,
  val licenses: List<PomLicense>,
  val developers: List<String>,
) {
  fun toSerializable(): SerializablePomInfo = SerializablePomInfo(
    name = name,
    url = url,
    licenses = licenses.map { SerializablePomLicense(it.name, it.url) },
    developers = developers,
  )
}
