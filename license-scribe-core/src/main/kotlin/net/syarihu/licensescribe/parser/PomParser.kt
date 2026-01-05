package net.syarihu.licensescribe.parser

import net.syarihu.licensescribe.model.PomInfo
import net.syarihu.licensescribe.model.PomLicense
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parser for Maven POM files.
 */
object PomParser {

  /**
   * Parse a POM file and extract metadata.
   *
   * @param pomFile The POM file to parse
   * @return PomInfo containing the extracted metadata, or null if parsing fails
   */
  fun parse(pomFile: File): PomInfo? = try {
    val doc = parseXml(pomFile)
    extractPomInfo(doc)
  } catch (_: Exception) {
    null
  }

  /**
   * Parse XML file with XXE attack prevention.
   */
  private fun parseXml(file: File): Document {
    val factory = DocumentBuilderFactory.newInstance().apply {
      // XXE (XML External Entity) attack prevention
      // See: https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html
      setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
      setFeature("http://xml.org/sax/features/external-general-entities", false)
      setFeature("http://xml.org/sax/features/external-parameter-entities", false)
      setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
      setXIncludeAware(false)
    }
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(file)
    doc.documentElement.normalize()
    return doc
  }

  /**
   * Extract PomInfo from a parsed XML document.
   */
  private fun extractPomInfo(doc: Document): PomInfo {
    val name = getTextContent(doc, "name")
    val url = getTextContent(doc, "url")
    val licenses = parseLicenses(doc)
    val developers = parseDevelopers(doc)
    val (parentGroupId, parentArtifactId, parentVersion) = parseParentInfo(doc)

    return PomInfo(
      name = name,
      url = url,
      licenses = licenses,
      developers = developers,
      parentGroupId = parentGroupId,
      parentArtifactId = parentArtifactId,
      parentVersion = parentVersion,
    )
  }

  /**
   * Parse licenses from the POM document.
   */
  private fun parseLicenses(doc: Document): List<PomLicense> = try {
    val licensesNodes = doc.getElementsByTagName("licenses")
    if (licensesNodes.length > 0) {
      val licensesNode = licensesNodes.item(0)
      val licenseNodes = (licensesNode as? Element)?.getElementsByTagName("license")
      (0 until (licenseNodes?.length ?: 0)).mapNotNull { i ->
        val licenseNode = licenseNodes?.item(i) as? Element
        val licenseName = getChildTextContent(licenseNode, "name")
        val licenseUrl = getChildTextContent(licenseNode, "url")
        if (licenseName != null) {
          PomLicense(licenseName, licenseUrl)
        } else {
          null
        }
      }
    } else {
      emptyList()
    }
  } catch (_: Exception) {
    emptyList()
  }

  /**
   * Parse developers from the POM document.
   */
  private fun parseDevelopers(doc: Document): List<String> = try {
    val developersNodes = doc.getElementsByTagName("developers")
    if (developersNodes.length > 0) {
      val developersNode = developersNodes.item(0)
      val developerNodes = (developersNode as? Element)?.getElementsByTagName("developer")
      (0 until (developerNodes?.length ?: 0)).mapNotNull { i ->
        val developerNode = developerNodes?.item(i) as? Element
        getChildTextContent(developerNode, "name")
      }
    } else {
      emptyList()
    }
  } catch (_: Exception) {
    emptyList()
  }

  /**
   * Parse parent POM info from the document.
   * Returns Triple of (groupId, artifactId, version), all nullable.
   */
  private fun parseParentInfo(doc: Document): Triple<String?, String?, String?> = try {
    val parentNodes = doc.getElementsByTagName("parent")
    if (parentNodes.length > 0) {
      val parentNode = parentNodes.item(0) as? Element
      val parentGroupId = getChildTextContent(parentNode, "groupId")
      val parentArtifactId = getChildTextContent(parentNode, "artifactId")
      val parentVersion = getChildTextContent(parentNode, "version")
      Triple(parentGroupId, parentArtifactId, parentVersion)
    } else {
      Triple(null, null, null)
    }
  } catch (_: Exception) {
    Triple(null, null, null)
  }

  /**
   * Get text content of a direct child element by tag name.
   */
  private fun getTextContent(
    doc: Document,
    tagName: String,
  ): String? {
    val nodes = doc.documentElement.childNodes
    for (i in 0 until nodes.length) {
      val node = nodes.item(i)
      if (node.nodeName == tagName) {
        return node.textContent?.takeIf { it.isNotBlank() }
      }
    }
    return null
  }

  /**
   * Get text content of a child element by tag name.
   */
  private fun getChildTextContent(
    element: Element?,
    tagName: String,
  ): String? {
    if (element == null) return null
    val nodes = element.getElementsByTagName(tagName)
    return if (nodes.length > 0) {
      nodes.item(0).textContent?.takeIf { it.isNotBlank() }
    } else {
      null
    }
  }
}
