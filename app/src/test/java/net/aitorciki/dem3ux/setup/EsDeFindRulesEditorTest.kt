package net.aitorciki.dem3ux.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class EsDeFindRulesEditorTest {
    @Test
    fun `creates find rules document with selected alias`() {
        val output = EsDeFindRulesEditor.applySelections(inputXml = null, selections = listOf(duckStation(selected = true)))

        assertTrue(output.startsWith("<?xml version=\"1.0\"?>\n<ruleList"))
        assertAliasEntries(
            xml = output,
            emulatorName = "DUCKSTATION",
            expected = listOf("net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity"),
        )
    }

    @Test
    fun `adds selected alias before existing entries`() {
        val input =
            """
            <ruleList>
                <emulator name="DUCKSTATION">
                    <rule type="androidpackage">
                        <entry>com.github.stenzek.duckstation/.EmulationActivity</entry>
                    </rule>
                </emulator>
            </ruleList>
            """.trimIndent()

        val output = EsDeFindRulesEditor.applySelections(input, listOf(duckStation(selected = true)))

        assertAliasEntries(
            xml = output,
            emulatorName = "DUCKSTATION",
            expected =
                listOf(
                    "net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity",
                    "com.github.stenzek.duckstation/.EmulationActivity",
                ),
        )
    }

    @Test
    fun `does not duplicate selected alias`() {
        val input =
            """
            <ruleList>
                <emulator name="DUCKSTATION">
                    <rule type="androidpackage">
                        <entry>net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity</entry>
                        <entry>com.github.stenzek.duckstation/.EmulationActivity</entry>
                    </rule>
                </emulator>
            </ruleList>
            """.trimIndent()

        val output = EsDeFindRulesEditor.applySelections(input, listOf(duckStation(selected = true)))

        assertAliasEntries(
            xml = output,
            emulatorName = "DUCKSTATION",
            expected =
                listOf(
                    "net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity",
                    "com.github.stenzek.duckstation/.EmulationActivity",
                ),
        )
    }

    @Test
    fun `removes only dem3ux alias and preserves user entries`() {
        val input =
            """
            <ruleList>
                <emulator name="DUCKSTATION">
                    <rule type="androidpackage">
                        <entry>net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity</entry>
                        <entry>com.github.stenzek.duckstation/.EmulationActivity</entry>
                    </rule>
                </emulator>
                <emulator name="FLYCAST">
                    <rule type="androidpackage">
                        <entry>net.aitorciki.dem3ux/.presets.FlycastBridgeActivity</entry>
                    </rule>
                </emulator>
            </ruleList>
            """.trimIndent()

        val output = EsDeFindRulesEditor.applySelections(input, listOf(duckStation(selected = false)))

        assertAliasEntries(
            xml = output,
            emulatorName = "DUCKSTATION",
            expected = listOf("com.github.stenzek.duckstation/.EmulationActivity"),
        )
        assertAliasEntries(
            xml = output,
            emulatorName = "FLYCAST",
            expected = listOf("net.aitorciki.dem3ux/.presets.FlycastBridgeActivity"),
        )
    }

    @Test
    fun `removes empty dem3ux-owned emulator block on deselect`() {
        val input =
            """
            <ruleList>
                <emulator name="DUCKSTATION">
                    <rule type="androidpackage">
                        <entry>net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity</entry>
                    </rule>
                </emulator>
            </ruleList>
            """.trimIndent()

        val output = EsDeFindRulesEditor.applySelections(input, listOf(duckStation(selected = false)))

        assertFalse(
            parse(output).documentElement.childElements("emulator").any { emulator -> emulator.getAttribute("name") == "DUCKSTATION" },
        )
    }

    @Test
    fun `reports selected emulator names from existing rules`() {
        val input =
            """
            <ruleList>
                <emulator name="DUCKSTATION">
                    <rule type="androidpackage">
                        <entry>net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity</entry>
                    </rule>
                </emulator>
                <emulator name="FLYCAST">
                    <rule type="androidpackage">
                        <entry>com.flycast.emulator/com.flycast.emulator.MainActivity</entry>
                    </rule>
                </emulator>
            </ruleList>
            """.trimIndent()

        val selected =
            EsDeFindRulesEditor.selectedEmulatorNames(
                inputXml = input,
                selections = listOf(duckStation(selected = true), flycast(selected = true)),
            )

        assertEquals(setOf("DUCKSTATION"), selected)
    }

    @Test
    fun `applies multiple selections deterministically`() {
        val output = EsDeFindRulesEditor.applySelections(null, listOf(flycast(selected = true), duckStation(selected = true)))
        val emulatorNames = parse(output).documentElement.childElements("emulator").map { emulator -> emulator.getAttribute("name") }

        assertEquals(listOf("DUCKSTATION", "FLYCAST"), emulatorNames)
    }

    private fun duckStation(selected: Boolean): EsDeFindRuleSelection =
        EsDeFindRuleSelection(
            emulatorName = "DUCKSTATION",
            aliasEntry = "net.aitorciki.dem3ux/.presets.DuckStationBridgeActivity",
            selected = selected,
        )

    private fun flycast(selected: Boolean): EsDeFindRuleSelection =
        EsDeFindRuleSelection(
            emulatorName = "FLYCAST",
            aliasEntry = "net.aitorciki.dem3ux/.presets.FlycastBridgeActivity",
            selected = selected,
        )

    private fun assertAliasEntries(
        xml: String,
        emulatorName: String,
        expected: List<String>,
    ) {
        val entries =
            requireNotNull(
                parse(xml).documentElement.childElements("emulator").firstOrNull { emulator ->
                    emulator.getAttribute("name") == emulatorName
                },
            ).childElements("rule")
                .single { rule -> rule.getAttribute("type") == "androidpackage" }
                .childElements("entry")
                .map { entry -> entry.textContent.trim() }

        assertEquals(expected, entries)
    }

    private fun parse(xml: String) =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(InputSource(StringReader(xml)))
}

private fun Element.childElements(name: String? = null): List<Element> =
    childNodes
        .asSequence()
        .filterIsInstance<Element>()
        .filter { element -> name == null || element.tagName == name }
        .toList()

private fun NodeList.asSequence(): Sequence<Node> = (0 until length).asSequence().map(::item)
