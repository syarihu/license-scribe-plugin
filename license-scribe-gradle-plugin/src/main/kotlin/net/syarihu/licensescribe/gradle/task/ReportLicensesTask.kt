package net.syarihu.licensescribe.gradle.task

import net.syarihu.licensescribe.gradle.LicenseScribeExtension
import net.syarihu.licensescribe.model.ArtifactId
import net.syarihu.licensescribe.model.DependencyTreeNode
import net.syarihu.licensescribe.model.DiffEntry
import net.syarihu.licensescribe.model.DiffReport
import net.syarihu.licensescribe.model.DiffStatus
import net.syarihu.licensescribe.model.DiffSummary
import net.syarihu.licensescribe.model.IgnoreRules
import net.syarihu.licensescribe.model.LicenseCatalog
import net.syarihu.licensescribe.report.HtmlDiffReportGenerator
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Instant

/**
 * Task to generate a visual diff report comparing Gradle dependencies
 * with YAML-defined license catalog in tree format.
 */
abstract class ReportLicensesTask : BaseLicenseTask() {
  /**
   * Directory where reports will be generated
   */
  @get:OutputDirectory
  abstract val reportDirectory: DirectoryProperty

  /**
   * Configuration name for the report header
   */
  @get:Input
  abstract val configurationName: Property<String>

  @get:Internal
  var configuration: Configuration? = null
    private set

  fun configureWith(
    extension: LicenseScribeExtension,
    configuration: Configuration?,
    variantName: String,
    project: Project,
  ) {
    super.configureWith(extension, configuration, variantName)
    this.configuration = configuration

    // Configuration name
    this.configurationName.set(configuration?.name ?: "unknown")

    // Output directory: build/reports/licensescribe/{variant}/
    val variantDir = if (variantName.isNotEmpty()) {
      "reports/licensescribe/$variantName"
    } else {
      "reports/licensescribe"
    }
    this.reportDirectory.set(project.layout.buildDirectory.dir(variantDir))
  }

  @TaskAction
  fun execute() {
    val catalog = loadLicenseCatalog()
    val ignoreRules = loadIgnoreRules()
    val config = configuration

    // Build dependency tree
    val dependencyTree = if (config != null) {
      buildDependencyTree(config, catalog, ignoreRules)
    } else {
      emptyList()
    }

    // Get current dependencies (filtered by ignore rules) for summary
    val currentDependencies = resolveDependencies()
      .filterNot { ignoreRules.shouldIgnore(it) }
      .map { "${it.group}:${it.name}" }
      .toSet()

    // Get catalog artifacts
    val catalogArtifacts = catalog.getAllArtifactIds()

    // Calculate diff for summary and extra entries
    val entries = mutableListOf<DiffEntry>()

    // Matched entries
    val matched = currentDependencies.intersect(catalogArtifacts)
    matched.forEach { coordinate ->
      val parts = coordinate.split(":")
      val groupId = parts[0]
      val artifactName = parts[1]
      val (licenseKey, _) = catalog.findArtifact(groupId, artifactName) ?: return@forEach
      val licenseEntry = catalog.getLicense(licenseKey)

      entries.add(
        DiffEntry(
          coordinate = coordinate,
          status = DiffStatus.MATCHED,
          licenseName = licenseEntry?.name,
          licenseKey = licenseKey,
        ),
      )
    }

    // Missing in catalog entries
    val missingInCatalog = currentDependencies - catalogArtifacts
    missingInCatalog.forEach { coordinate ->
      val parts = coordinate.split(":")
      val groupId = parts[0]
      val artifactName = parts[1]

      val deps = resolveDependencies()
      val artifact = deps.find { it.group == groupId && it.name == artifactName }
      val pomInfo = artifact?.let { resolvePomInfo(it) }
      val pomLicenseName = pomInfo?.licenses?.firstOrNull()?.name

      entries.add(
        DiffEntry(
          coordinate = coordinate,
          status = DiffStatus.MISSING_IN_CATALOG,
          pomLicenseName = pomLicenseName,
        ),
      )
    }

    // Extra in catalog entries
    val extraInCatalogSet = catalogArtifacts - currentDependencies
    val extraEntries = extraInCatalogSet.map { coordinate ->
      val parts = coordinate.split(":")
      val groupId = parts[0]
      val artifactName = parts[1]
      val (licenseKey, _) = catalog.findArtifact(groupId, artifactName) ?: return@map null
      val licenseEntry = catalog.getLicense(licenseKey)

      DiffEntry(
        coordinate = coordinate,
        status = DiffStatus.EXTRA_IN_CATALOG,
        licenseName = licenseEntry?.name,
        licenseKey = licenseKey,
      )
    }.filterNotNull()

    // Build report
    val report = DiffReport(
      variantName = variantName.get(),
      configurationName = configurationName.get(),
      summary = DiffSummary(
        totalGradleDependencies = currentDependencies.size,
        totalCatalogArtifacts = catalogArtifacts.size,
        matchedCount = matched.size,
        missingInCatalogCount = missingInCatalog.size,
        extraInCatalogCount = extraInCatalogSet.size,
      ),
      entries = entries.sortedWith(compareBy({ it.status.ordinal }, { it.coordinate })),
      dependencyTree = dependencyTree,
      extraInCatalog = extraEntries.sortedBy { it.coordinate },
      generatedAt = Instant.now().toString(),
    )

    // Generate HTML report
    val reportDir = reportDirectory.get().asFile
    reportDir.mkdirs()

    val generator = HtmlDiffReportGenerator()
    val file = File(reportDir, "license-diff-report.html")
    file.writeText(generator.generate(report))

    logger.lifecycle("Generated: ${file.absolutePath}")

    // Print summary to console
    logger.lifecycle("")
    logger.lifecycle("=== License Diff Summary ===")
    logger.lifecycle("Matched:    ${report.summary.matchedCount}")
    logger.lifecycle("Missing:    ${report.summary.missingInCatalogCount}")
    logger.lifecycle("Extra:      ${report.summary.extraInCatalogCount}")

    if (report.summary.missingInCatalogCount > 0 || report.summary.extraInCatalogCount > 0) {
      val variantSuffix = if (variantName.get().isNotEmpty()) {
        variantName.get().replaceFirstChar { it.uppercaseChar() }
      } else {
        ""
      }
      logger.warn("Run ./gradlew scribeLicenses${variantSuffix}Sync to synchronize.")
    }
  }

  private fun buildDependencyTree(
    config: Configuration,
    catalog: LicenseCatalog,
    ignoreRules: IgnoreRules,
  ): List<DependencyTreeNode> {
    val catalogArtifacts = catalog.getAllArtifactIds()

    return try {
      val root = config.incoming.resolutionResult.root
      val seen = mutableSetOf<String>()

      // Get first-level dependencies (skip root project)
      root.dependencies
        .filterIsInstance<ResolvedDependencyResult>()
        .mapNotNull { dep ->
          buildTreeNode(dep.selected, catalog, catalogArtifacts, ignoreRules, seen)
        }
    } catch (e: Exception) {
      logger.warn("Failed to build dependency tree: ${e.message}")
      emptyList()
    }
  }

  private fun buildTreeNode(
    component: ResolvedComponentResult,
    catalog: LicenseCatalog,
    catalogArtifacts: Set<String>,
    ignoreRules: IgnoreRules,
    seen: MutableSet<String>,
  ): DependencyTreeNode? {
    val id = component.id
    if (id !is ModuleComponentIdentifier) return null

    val coordinate = "${id.group}:${id.module}:${id.version}"
    val shortCoordinate = "${id.group}:${id.module}"

    // Check if ignored
    val artifactId = ArtifactId(id.group, id.module, id.version)
    if (ignoreRules.shouldIgnore(artifactId)) return null

    // Check if already visited (cycle detection)
    val isVisited = shortCoordinate in seen
    if (isVisited) {
      return DependencyTreeNode(
        coordinate = coordinate,
        licenseKey = catalog.findArtifact(id.group, id.module)?.first,
        isInCatalog = shortCoordinate in catalogArtifacts,
        children = emptyList(),
        isVisited = true,
      )
    }

    seen.add(shortCoordinate)

    // Get license key from catalog
    val licenseKey = catalog.findArtifact(id.group, id.module)?.first
    val isInCatalog = shortCoordinate in catalogArtifacts

    // Build children recursively
    val children = component.dependencies
      .filterIsInstance<ResolvedDependencyResult>()
      .mapNotNull { dep ->
        buildTreeNode(dep.selected, catalog, catalogArtifacts, ignoreRules, seen)
      }

    return DependencyTreeNode(
      coordinate = coordinate,
      licenseKey = licenseKey,
      isInCatalog = isInCatalog,
      children = children,
      isVisited = false,
    )
  }
}
