package net.syarihu.licensescribe.model

/**
 * Represents a record with its license information.
 */
data class Record(
  val name: String,
  val url: String? = null,
  val copyrightHolder: String? = null,
  val license: String,
)

/**
 * Represents a group of records under the same group ID.
 */
data class RecordGroup(
  val groupId: String,
  val records: List<Record>,
)

/**
 * Represents records organized by scope.
 */
data class ScopedRecords(
  val scope: String,
  val groups: List<RecordGroup>,
)
