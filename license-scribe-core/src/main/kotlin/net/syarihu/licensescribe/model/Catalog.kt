package net.syarihu.licensescribe.model

/**
 * Represents a catalog of licenses.
 */
data class Catalog(
  val licenses: Map<String, License>,
) {
  companion object {
    val EMPTY = Catalog(emptyMap())
  }

  fun getLicense(key: String): License? = licenses[key]

  fun containsKey(key: String): Boolean = licenses.containsKey(key)
}
