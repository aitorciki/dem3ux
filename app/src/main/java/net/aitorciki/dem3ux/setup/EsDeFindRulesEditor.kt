package net.aitorciki.dem3ux.setup

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

data class EsDeFindRuleSelection(
    val emulatorName: String,
    val aliasEntry: String,
    val selected: Boolean,
)

object EsDeFindRulesEditor {
    fun applySelections(
        inputXml: String?,
        selections: List<EsDeFindRuleSelection>,
    ): String {
        val document = inputXml.toRuleListDocument()
        val ruleList = document.documentElement

        selections
            .sortedBy { selection -> selection.emulatorName }
            .forEach { selection ->
                if (selection.selected) {
                    ruleList.ensureAliasEntry(selection)
                } else {
                    ruleList.removeAliasEntry(selection)
                }
            }

        return document.toXmlString()
    }

    fun selectedEmulatorNames(
        inputXml: String?,
        selections: List<EsDeFindRuleSelection>,
    ): Set<String> {
        val document = inputXml.toRuleListDocument()
        val ruleList = document.documentElement

        return selections
            .filter { selection -> ruleList.hasAliasEntry(selection) }
            .map { selection -> selection.emulatorName }
            .toSet()
    }
}

private fun String?.toRuleListDocument(): Document =
    if (isNullOrBlank()) {
        newRuleListDocument()
    } else {
        parseRuleListDocument(this)
    }

private fun newRuleListDocument(): Document =
    documentBuilderFactory().newDocumentBuilder().newDocument().apply {
        appendChild(createElement("ruleList"))
    }

private fun parseRuleListDocument(xml: String): Document {
    val document = documentBuilderFactory().newDocumentBuilder().parse(InputSource(StringReader(xml)))
    document.documentElement.normalize()
    document.documentElement.removeBlankTextNodes()

    if (document.documentElement.tagName != "ruleList") {
        return newRuleListDocument()
    }

    return document
}

private fun documentBuilderFactory(): DocumentBuilderFactory =
    DocumentBuilderFactory.newInstance().apply {
        trySetFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        trySetFeature("http://xml.org/sax/features/external-general-entities", false)
        trySetFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isExpandEntityReferences = false
    }

private fun DocumentBuilderFactory.trySetFeature(
    name: String,
    value: Boolean,
) {
    try {
        setFeature(name, value)
    } catch (_: ParserConfigurationException) {
        // Android XML parser implementations vary; unsupported hardening flags should not block setup.
    } catch (_: SAXNotRecognizedException) {
        // Android XML parser implementations vary; unsupported hardening flags should not block setup.
    } catch (_: SAXNotSupportedException) {
        // Android XML parser implementations vary; unsupported hardening flags should not block setup.
    }
}

private fun Element.ensureAliasEntry(selection: EsDeFindRuleSelection) {
    val emulator =
        findEmulator(selection.emulatorName) ?: appendElement("emulator").also { emulator ->
            emulator.setAttribute("name", selection.emulatorName)
        }
    val rule =
        emulator.findAndroidPackageRule() ?: emulator.appendElement("rule").also { rule ->
            rule.setAttribute("type", "androidpackage")
        }

    rule.removeEntry(selection.aliasEntry)
    val entry = ownerDocument.createElement("entry").apply { textContent = selection.aliasEntry }
    rule.insertBefore(entry, rule.firstElementChild())
}

private fun Element.removeAliasEntry(selection: EsDeFindRuleSelection) {
    val emulator = findEmulator(selection.emulatorName) ?: return
    val rules = emulator.childElements("rule").filter { rule -> rule.getAttribute("type") == "androidpackage" }

    rules.forEach { rule ->
        rule.removeEntry(selection.aliasEntry)
        if (!rule.hasElementChildren()) {
            emulator.removeChild(rule)
        }
    }

    if (!emulator.hasElementChildren()) {
        removeChild(emulator)
    }
}

private fun Element.hasAliasEntry(selection: EsDeFindRuleSelection): Boolean =
    findEmulator(selection.emulatorName)
        ?.childElements("rule")
        ?.filter { rule -> rule.getAttribute("type") == "androidpackage" }
        ?.flatMap { rule -> rule.childElements("entry") }
        ?.any { entry -> entry.textContent.trim() == selection.aliasEntry }
        ?: false

private fun Element.findEmulator(name: String): Element? =
    childElements("emulator").firstOrNull { emulator -> emulator.getAttribute("name") == name }

private fun Element.findAndroidPackageRule(): Element? =
    childElements("rule").firstOrNull { rule -> rule.getAttribute("type") == "androidpackage" }

private fun Element.removeEntry(value: String) {
    childElements("entry")
        .filter { entry -> entry.textContent.trim() == value }
        .forEach(::removeChild)
}

private fun Element.appendElement(name: String): Element = ownerDocument.createElement(name).also(::appendChild)

private fun Element.firstElementChild(): Element? = childElements().firstOrNull()

private fun Element.hasElementChildren(): Boolean = childElements().isNotEmpty()

private fun Element.childElements(name: String? = null): List<Element> =
    childNodes
        .asSequence()
        .filterIsInstance<Element>()
        .filter { element -> name == null || element.tagName == name }
        .toList()

private fun Node.removeBlankTextNodes() {
    childNodes
        .asSequence()
        .filter { node -> node.nodeType == Node.TEXT_NODE && node.textContent.isBlank() }
        .toList()
        .forEach(::removeChild)

    childNodes.asSequence().forEach { node -> node.removeBlankTextNodes() }
}

private fun NodeList.asSequence(): Sequence<Node> = (0 until length).asSequence().map(::item)

private fun Document.toXmlString(): String {
    val transformer =
        TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty(OutputKeys.ENCODING, "utf-8")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        }
    val output = StringWriter()
    transformer.transform(DOMSource(this), StreamResult(output))
    return "<?xml version=\"1.0\"?>\n$output"
}
