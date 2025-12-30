package net.syarihu.licensescribe.report

import net.syarihu.licensescribe.model.DependencyTreeNode
import net.syarihu.licensescribe.model.DiffReport

/**
 * Generates diff reports in HTML format with dependency tree view.
 * Similar to `./gradlew dependencies` output but with license annotations.
 */
class HtmlDiffReportGenerator : DiffReportGenerator {
  override val fileExtension = "html"

  override fun generate(report: DiffReport): String = buildString {
    val variantSuffix = if (report.variantName.isNotEmpty()) " - ${report.variantName}" else ""

    appendLine("<!DOCTYPE html>")
    appendLine("<html lang=\"en\">")
    appendLine("<head>")
    appendLine("  <meta charset=\"UTF-8\">")
    appendLine("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
    appendLine("  <title>License Diff Report$variantSuffix</title>")
    appendLine("  <style>")
    appendLine(CSS_STYLES)
    appendLine("  </style>")
    appendLine("</head>")
    appendLine("<body>")
    appendLine("  <div class=\"container\">")
    appendLine("    <header>")
    appendLine("      <h1>License Diff Report$variantSuffix</h1>")
    appendLine("      <p class=\"generated-at\">Generated: ${escapeHtml(report.generatedAt)}</p>")
    appendLine("    </header>")
    appendLine()

    // Summary and Legend row
    appendLine("    <section class=\"summary-row\">")
    appendLine("      <div class=\"summary\">")
    appendLine("        <div class=\"summary-item matched\">")
    appendLine("          <span class=\"count\">${report.summary.matchedCount}</span>")
    appendLine("          <span class=\"label\">Matched</span>")
    appendLine("        </div>")
    appendLine("        <div class=\"summary-item missing\">")
    appendLine("          <span class=\"count\">${report.summary.missingInCatalogCount}</span>")
    appendLine("          <span class=\"label\">Missing</span>")
    appendLine("        </div>")
    appendLine("        <div class=\"summary-item extra\">")
    appendLine("          <span class=\"count\">${report.summary.extraInCatalogCount}</span>")
    appendLine("          <span class=\"label\">Extra</span>")
    appendLine("        </div>")
    appendLine("      </div>")
    appendLine("      <div class=\"legend\">")
    appendLine("        <div class=\"legend-item\"><span class=\"license-tag matched\">[License]</span> In catalog</div>")
    appendLine("        <div class=\"legend-item\"><span class=\"license-tag missing\">[???]</span> Not in catalog</div>")
    appendLine("        <div class=\"legend-item\"><span class=\"visited\">(*)</span> Already listed</div>")
    appendLine("      </div>")
    appendLine("    </section>")
    appendLine()

    // Dependency tree
    appendLine("    <section class=\"tree-container\" id=\"tree-container\">")
    appendLine("      <div class=\"tree-toolbar\">")
    appendLine("        <div class=\"tree-header\">")
    appendLine("          <h2>Dependency Tree</h2>")
    appendLine("          <button class=\"expand-btn\" id=\"expand-btn\" title=\"Toggle fullwidth\">")
    appendLine("            <span class=\"expand-icon\">&#x26F6;</span> Expand")
    appendLine("          </button>")
    appendLine("        </div>")
    appendLine("        <div class=\"filters\">")
    appendLine("          <div class=\"filter-buttons\">")
    appendLine("            <button class=\"filter-btn active\" data-filter=\"all\">All</button>")
    appendLine("            <button class=\"filter-btn\" data-filter=\"matched\">Matched</button>")
    appendLine("            <button class=\"filter-btn\" data-filter=\"missing\">Missing</button>")
    appendLine("          </div>")
    appendLine("          <input type=\"text\" id=\"search\" class=\"search-input\" placeholder=\"Search artifacts...\">")
    appendLine("        </div>")
    appendLine("      </div>")
    appendLine("      <pre class=\"tree\" id=\"dependency-tree\">")
    appendLine("<span class=\"config-name\">${escapeHtml(report.configurationName)}</span>")

    report.dependencyTree.forEachIndexed { index, node ->
      val isLast = index == report.dependencyTree.lastIndex
      appendTreeNode(node, "", isLast)
    }

    appendLine("      </pre>")
    appendLine("    </section>")
    appendLine()

    // Extra in catalog section
    if (report.extraInCatalog.isNotEmpty()) {
      appendLine("    <section class=\"extra-section\">")
      appendLine("      <h2>Extra in Catalog (${report.extraInCatalog.size})</h2>")
      appendLine("      <p class=\"section-desc\">These entries exist in <code>scribe-licenses.yml</code> but the dependency was removed:</p>")
      appendLine("      <ul class=\"extra-list\">")
      report.extraInCatalog.sortedBy { it.coordinate }.forEach { entry ->
        val license = entry.licenseName ?: "Unknown"
        appendLine("        <li><span class=\"artifact\">${escapeHtml(entry.coordinate)}</span> - <span class=\"license\">${escapeHtml(license)}</span></li>")
      }
      appendLine("      </ul>")
      appendLine("    </section>")
      appendLine()
    }

    appendLine("  </div>")
    appendLine()
    appendLine("  <script>")
    appendLine(JS_SCRIPT)
    appendLine("  </script>")
    appendLine("</body>")
    appendLine("</html>")
  }

  private fun StringBuilder.appendTreeNode(
    node: DependencyTreeNode,
    prefix: String,
    isLast: Boolean,
  ) {
    val connector = if (isLast) "\\--- " else "+--- "
    val statusClass = when {
      node.isVisited -> "visited"
      node.isInCatalog -> "matched"
      else -> "missing"
    }
    val licenseTag = when {
      node.isVisited -> ""
      node.isInCatalog -> "<span class=\"license-tag matched\" data-status=\"matched\">[${escapeHtml(node.licenseKey ?: "???")}]</span> "
      else -> "<span class=\"license-tag missing\" data-status=\"missing\">[???]</span> "
    }
    val visitedSuffix = if (node.isVisited) " <span class=\"visited\">(*)</span>" else ""

    appendLine("$prefix$connector$licenseTag<span class=\"artifact $statusClass\" data-status=\"$statusClass\">${escapeHtml(node.coordinate)}</span>$visitedSuffix")

    val childPrefix = prefix + if (isLast) "     " else "|    "
    node.children.forEachIndexed { index, child ->
      val childIsLast = index == node.children.lastIndex
      appendTreeNode(child, childPrefix, childIsLast)
    }
  }

  private fun escapeHtml(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

  companion object {
    private val CSS_STYLES = """
      * {
        box-sizing: border-box;
        margin: 0;
        padding: 0;
      }
      body {
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
        background-color: #0d1117;
        color: #c9d1d9;
        line-height: 1.5;
      }
      .container {
        max-width: 1200px;
        margin: 0 auto;
        padding: 20px;
      }
      header {
        margin-bottom: 24px;
      }
      h1 {
        font-size: 24px;
        font-weight: 600;
        color: #f0f6fc;
        margin-bottom: 8px;
      }
      h2 {
        font-size: 18px;
        font-weight: 600;
        color: #f0f6fc;
        margin-bottom: 16px;
      }
      h3 {
        font-size: 14px;
        font-weight: 600;
        color: #8b949e;
        margin-bottom: 8px;
      }
      .generated-at {
        font-size: 14px;
        color: #8b949e;
      }
      .summary-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 24px;
        margin-bottom: 24px;
        flex-wrap: wrap;
      }
      .summary {
        display: flex;
        gap: 16px;
        flex-wrap: wrap;
      }
      .summary-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 16px 24px;
        border-radius: 8px;
        min-width: 100px;
      }
      .summary-item.matched { background-color: #238636; }
      .summary-item.missing { background-color: #9e6a03; }
      .summary-item.extra { background-color: #da3633; }
      .summary-item .count {
        font-size: 28px;
        font-weight: 600;
        color: #fff;
      }
      .summary-item .label {
        font-size: 14px;
        color: rgba(255,255,255,0.8);
      }
      .legend {
        background-color: #161b22;
        border: 1px solid #30363d;
        border-radius: 8px;
        padding: 12px 16px;
        display: flex;
        flex-direction: column;
        gap: 6px;
        flex-shrink: 0;
      }
      .legend-item {
        font-size: 12px;
        color: #8b949e;
        white-space: nowrap;
        display: flex;
        align-items: center;
        gap: 6px;
      }
      .filters {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-wrap: wrap;
        gap: 12px;
      }
      .filter-buttons {
        display: flex;
        gap: 8px;
      }
      .filter-btn {
        padding: 8px 16px;
        border: 1px solid #30363d;
        background-color: #21262d;
        color: #c9d1d9;
        border-radius: 6px;
        cursor: pointer;
        font-size: 14px;
        transition: all 0.2s;
      }
      .filter-btn:hover {
        background-color: #30363d;
      }
      .filter-btn.active {
        background-color: #238636;
        border-color: #238636;
        color: #fff;
      }
      .search-input {
        padding: 8px 12px;
        border: 1px solid #30363d;
        background-color: #0d1117;
        color: #c9d1d9;
        border-radius: 6px;
        font-size: 14px;
        width: 250px;
      }
      .search-input:focus {
        outline: none;
        border-color: #58a6ff;
        box-shadow: 0 0 0 3px rgba(88, 166, 255, 0.3);
      }
      .tree-container {
        background-color: #161b22;
        border: 1px solid #30363d;
        border-radius: 8px;
        padding: 16px;
        margin-bottom: 24px;
        overflow-x: auto;
        transition: all 0.3s ease;
      }
      .tree-container.expanded {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        max-width: none;
        width: 100vw;
        height: 100vh;
        margin: 0;
        border-radius: 0;
        z-index: 1000;
        overflow: auto;
      }
      .tree-toolbar {
        margin-bottom: 16px;
      }
      .tree-container.expanded .tree-toolbar {
        position: sticky;
        top: 0;
        background-color: #161b22;
        padding: 16px 0;
        margin: -16px -16px 16px -16px;
        padding: 16px;
        z-index: 10;
        border-bottom: 1px solid #30363d;
      }
      .tree-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
      }
      .tree-header h2 {
        margin-bottom: 0;
      }
      .expand-btn {
        padding: 8px 16px;
        border: 1px solid #30363d;
        background-color: #21262d;
        color: #c9d1d9;
        border-radius: 6px;
        cursor: pointer;
        font-size: 14px;
        display: flex;
        align-items: center;
        gap: 6px;
        transition: all 0.2s;
      }
      .expand-btn:hover {
        background-color: #30363d;
        border-color: #58a6ff;
      }
      .expand-icon {
        font-size: 16px;
      }
      .tree {
        font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, monospace;
        font-size: 13px;
        line-height: 1.6;
        white-space: pre;
        margin: 0;
      }
      .config-name {
        color: #58a6ff;
        font-weight: 600;
      }
      .artifact {
        color: #c9d1d9;
      }
      .artifact.matched {
        color: #7ee787;
      }
      .artifact.missing {
        color: #f0883e;
      }
      .artifact.visited {
        color: #8b949e;
      }
      .artifact.hidden {
        display: none;
      }
      .license-tag {
        font-weight: 600;
        padding: 0 4px;
        border-radius: 3px;
      }
      .license-tag.matched {
        color: #7ee787;
        background-color: rgba(35, 134, 54, 0.2);
      }
      .license-tag.missing {
        color: #f0883e;
        background-color: rgba(158, 106, 3, 0.2);
      }
      .visited {
        color: #8b949e;
        font-style: italic;
      }
      .extra-section {
        background-color: #161b22;
        border: 1px solid #da3633;
        border-radius: 8px;
        padding: 16px;
        margin-bottom: 24px;
      }
      .extra-section h2 {
        color: #f85149;
      }
      .section-desc {
        color: #8b949e;
        margin-bottom: 12px;
        font-size: 14px;
      }
      .extra-list {
        list-style: none;
        padding: 0;
      }
      .extra-list li {
        padding: 8px 0;
        border-bottom: 1px solid #21262d;
        font-family: ui-monospace, SFMono-Regular, 'SF Mono', Menlo, monospace;
        font-size: 13px;
      }
      .extra-list li:last-child {
        border-bottom: none;
      }
      .extra-list .artifact {
        color: #f85149;
      }
      .extra-list .license {
        color: #8b949e;
      }
    """.trimIndent()

    private val JS_SCRIPT = """
      document.addEventListener('DOMContentLoaded', function() {
        const filterButtons = document.querySelectorAll('.filter-btn');
        const searchInput = document.getElementById('search');
        const artifacts = document.querySelectorAll('.artifact');
        const licenseTags = document.querySelectorAll('.license-tag');
        const expandBtn = document.getElementById('expand-btn');
        const treeContainer = document.getElementById('tree-container');

        let currentFilter = 'all';
        let searchTerm = '';
        let isExpanded = false;

        function updateVisibility() {
          artifacts.forEach(artifact => {
            const status = artifact.dataset.status;
            const text = artifact.textContent.toLowerCase();

            const matchesFilter = currentFilter === 'all' || status === currentFilter;
            const matchesSearch = searchTerm === '' || text.includes(searchTerm.toLowerCase());

            // For tree view, we just highlight/dim instead of hiding
            if (matchesFilter && matchesSearch) {
              artifact.style.opacity = '1';
            } else {
              artifact.style.opacity = '0.3';
            }
          });

          // Also update license tags opacity
          licenseTags.forEach(tag => {
            const status = tag.dataset.status;
            const matchesFilter = currentFilter === 'all' || status === currentFilter;

            if (matchesFilter) {
              tag.style.opacity = '1';
            } else {
              tag.style.opacity = '0.3';
            }
          });
        }

        filterButtons.forEach(btn => {
          btn.addEventListener('click', function() {
            filterButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.dataset.filter;
            updateVisibility();
          });
        });

        searchInput.addEventListener('input', function() {
          searchTerm = this.value;
          updateVisibility();
        });

        // Expand/Collapse button
        expandBtn.addEventListener('click', function() {
          isExpanded = !isExpanded;
          treeContainer.classList.toggle('expanded', isExpanded);
          this.innerHTML = isExpanded
            ? '<span class="expand-icon">&#x2716;</span> Close'
            : '<span class="expand-icon">&#x26F6;</span> Expand';
        });

        // ESC key to close expanded view
        document.addEventListener('keydown', function(e) {
          if (e.key === 'Escape' && isExpanded) {
            isExpanded = false;
            treeContainer.classList.remove('expanded');
            expandBtn.innerHTML = '<span class="expand-icon">&#x26F6;</span> Expand';
          }
        });
      });
    """.trimIndent()
  }
}
