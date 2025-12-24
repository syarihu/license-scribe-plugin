package net.syarihu.licensescribe.gradle.task.model

/**
 * Container for POM data including parent reference.
 */
data class PomDataWithParent(
  val pomInfo: PomInfo,
  val parentInfo: ParentPomInfo?,
)
