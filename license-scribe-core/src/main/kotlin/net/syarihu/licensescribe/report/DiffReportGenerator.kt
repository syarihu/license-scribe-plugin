package net.syarihu.licensescribe.report

import net.syarihu.licensescribe.model.DiffReport

/**
 * Interface for generating diff reports in various formats.
 */
interface DiffReportGenerator {
  /**
   * File extension for this report type (e.g., "html", "md")
   */
  val fileExtension: String

  /**
   * Generate report content from diff data.
   *
   * @param report The diff report data
   * @return The generated report content as a string
   */
  fun generate(report: DiffReport): String
}
