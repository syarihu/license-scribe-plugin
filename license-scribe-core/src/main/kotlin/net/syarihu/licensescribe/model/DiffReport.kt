package net.syarihu.licensescribe.model

/**
 * Represents the status of a dependency comparison.
 */
enum class DiffStatus {
  /** Dependency exists both in Gradle and YAML catalog */
  MATCHED,

  /** Dependency exists in Gradle but not in YAML catalog (needs to be added) */
  MISSING_IN_CATALOG,

  /** Dependency exists in YAML but not in Gradle dependencies (can be removed) */
  EXTRA_IN_CATALOG,
}

/**
 * Represents a single diff entry between Gradle dependency and YAML catalog.
 */
data class DiffEntry(
  /** Artifact coordinate in "group:name" format */
  val coordinate: String,
  /** Diff status */
  val status: DiffStatus,
  /** License name from catalog (if matched or extra) */
  val licenseName: String? = null,
  /** License key from catalog (if matched or extra) */
  val licenseKey: String? = null,
  /** License name from POM (for missing entries) */
  val pomLicenseName: String? = null,
)

/**
 * Represents a node in the dependency tree with license information.
 */
data class DependencyTreeNode(
  /** Artifact coordinate in "group:name:version" format */
  val coordinate: String,
  /** License key (e.g., "apache-2.0", "mit") or null if not found */
  val licenseKey: String? = null,
  /** Whether this dependency is recorded in YAML catalog */
  val isInCatalog: Boolean,
  /** Child dependencies */
  val children: List<DependencyTreeNode> = emptyList(),
  /** Whether this node was already visited (for cycle detection display) */
  val isVisited: Boolean = false,
  /** Version conflict info (e.g., "1.0.0 -> 2.0.0") */
  val versionConflict: String? = null,
)

/**
 * Summary statistics for the diff report.
 */
data class DiffSummary(
  /** Total number of Gradle dependencies (after ignore rules applied) */
  val totalGradleDependencies: Int,
  /** Total number of artifacts in YAML catalog */
  val totalCatalogArtifacts: Int,
  /** Number of matched dependencies */
  val matchedCount: Int,
  /** Number of dependencies missing in catalog */
  val missingInCatalogCount: Int,
  /** Number of extra entries in catalog */
  val extraInCatalogCount: Int,
)

/**
 * Complete diff report data.
 */
data class DiffReport(
  /** Variant name (e.g., "debug", "release") or empty for non-Android */
  val variantName: String,
  /** Configuration name (e.g., "debugRuntimeClasspath") */
  val configurationName: String,
  /** Summary statistics */
  val summary: DiffSummary,
  /** All diff entries (flat list) */
  val entries: List<DiffEntry>,
  /** Dependency tree with license annotations */
  val dependencyTree: List<DependencyTreeNode>,
  /** Extra entries in catalog (not in dependency tree) */
  val extraInCatalog: List<DiffEntry>,
  /** ISO timestamp when the report was generated */
  val generatedAt: String,
)
