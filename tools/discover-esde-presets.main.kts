#!/usr/bin/env kotlin

import org.w3c.dom.Element
import java.io.File
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

private val esSystemsUrl =
    "https://gitlab.com/es-de/emulationstation-de/-/raw/master/resources/systems/android/es_systems.xml"
private val esFindRulesUrl =
    "https://gitlab.com/es-de/emulationstation-de/-/raw/master/resources/systems/android/es_find_rules.xml"
private val bridgePresetCatalogFile = File("presets/bridge-presets.json")

private val emulatorRegex = Regex("%EMULATOR_([^%]+)%")
private val dataRegex = Regex("%DATA%=(\"[^\"]*\"|[^\\s]+)")
private val actionRegex = Regex("%ACTION%=(\"[^\"]*\"|[^\\s]+)")
private val categoryRegex = Regex("%CATEGORY%=(\"[^\"]*\"|[^\\s]+)")
private val mimeTypeRegex = Regex("%MIMETYPE%=(\"[^\"]*\"|[^\\s]+)")
private val stringExtraRegex = Regex("%EXTRA_([^%\\s=]+)%=(\"[^\"]*\"|[^\\s]+)")
private val stringArrayExtraRegex = Regex("%EXTRAARRAY_([^%\\s=]+)%=(\"[^\"]*\"|[^\\s]+)")

private data class EsSystem(
    val name: String,
    val fullName: String,
    val extensions: Set<String>,
    val commands: List<EsCommand>,
)

private data class EsCommand(
    val label: String,
    val value: String,
)

private data class EmulatorRule(
    val name: String,
    val androidPackages: List<String>,
)

private data class RomCarrier(
    val kind: String,
    val key: String? = null,
    val value: String,
    val embedded: Boolean,
) {
    val display: String = if (key == null) "$kind = $value" else "$kind $key = $value"
}

private data class CommandAnalysis(
    val system: EsSystem,
    val command: EsCommand,
    val emulatorName: String?,
    val targetPackages: List<String>,
    val action: String?,
    val category: String?,
    val mimeType: String?,
    val carriers: List<RomCarrier>,
    val flags: List<String>,
    val status: Status,
    val reasons: List<String>,
    val supportedPreset: SupportedPreset?,
)

private data class SupportedPreset(
    val aliasClassName: String,
    val status: String,
)

private enum class Status(
    val title: String,
) {
    Likely("Likely Presets"),
    Review("Needs Review"),
    Unsupported("Unsupported"),
    Skipped("Skipped"),
}

private fun fetch(url: String): String = URI(url).toURL().readText()

private fun parseXml(xml: String): Element =
    DocumentBuilderFactory
        .newInstance()
        .apply {
            isIgnoringComments = true
            isCoalescing = true
        }.newDocumentBuilder()
        .parse(xml.byteInputStream())
        .documentElement

private fun Element.childText(tagName: String): String? =
    getElementsByTagName(tagName)
        .takeIf { nodes -> nodes.length > 0 }
        ?.item(0)
        ?.textContent
        ?.trim()
        ?.takeIf(String::isNotBlank)

private fun Element.children(tagName: String): List<Element> {
    val nodes = getElementsByTagName(tagName)
    return (0 until nodes.length).mapNotNull { index -> nodes.item(index) as? Element }
}

private fun parseSystems(xml: String): List<EsSystem> =
    parseXml(xml)
        .children("system")
        .map { system ->
            EsSystem(
                name = system.childText("name").orEmpty(),
                fullName = system.childText("fullname").orEmpty(),
                extensions =
                    system
                        .childText("extension")
                        .orEmpty()
                        .split(Regex("\\s+"))
                        .filter(String::isNotBlank)
                        .map(String::lowercase)
                        .toSet(),
                commands =
                    system.children("command").map { command ->
                        EsCommand(
                            label = command.getAttribute("label").ifBlank { "(unlabeled)" },
                            value = command.textContent.trim(),
                        )
                    },
            )
        }

private fun parseEmulatorRules(xml: String): Map<String, EmulatorRule> =
    parseXml(xml)
        .children("emulator")
        .associate { emulator ->
            val name = emulator.getAttribute("name")
            val androidPackages =
                emulator
                    .children("rule")
                    .filter { rule -> rule.getAttribute("type") == "androidpackage" }
                    .flatMap { rule -> rule.children("entry") }
                    .map { entry -> entry.textContent.trim() }
                    .filter(String::isNotBlank)

            name to EmulatorRule(name = name, androidPackages = androidPackages)
        }

private fun analyzeCommand(
    system: EsSystem,
    command: EsCommand,
    emulatorRules: Map<String, EmulatorRule>,
    supportedPresets: Map<String, SupportedPreset>,
): CommandAnalysis {
    val emulatorName = emulatorRegex.find(command.value)?.groupValues?.get(1)
    val rule = emulatorName?.let(emulatorRules::get)
    val targetPackages = rule?.androidPackages.orEmpty()
    val carriers = command.value.romCarriers()
    val reasons = mutableListOf<String>()
    var status = Status.Likely

    fun skip(reason: String) {
        status = Status.Skipped
        reasons += reason
    }

    fun unsupported(reason: String) {
        if (status != Status.Skipped) {
            status = Status.Unsupported
            reasons += reason
        }
    }

    fun review(reason: String) {
        if (status == Status.Likely) {
            status = Status.Review
        }
        if (status == Status.Review) {
            reasons += reason
        }
    }

    when {
        emulatorName == null -> skip("No `%EMULATOR_*%` token.")
        emulatorName.contains("RETROARCH") -> skip("RetroArch is skipped because it has native playlist support.")
        rule == null -> unsupported("No matching `es_find_rules.xml` emulator rule.")
        targetPackages.isEmpty() -> unsupported("No Android package/activity rule.")
    }

    if (status != Status.Skipped) {
        if (carriers.isEmpty()) {
            unsupported("No ROM carrier using `%ROM%`, `%ROMSAF%`, or related variables was found.")
        }
        if (carriers.size > 1) {
            review("Multiple ROM carriers found: ${carriers.joinToString { carrier -> carrier.display }}.")
        }
        if (carriers.any { carrier -> carrier.embedded }) {
            review("ROM variable is embedded inside a larger carrier value.")
        }
        if (targetPackages.any { target -> "/" !in target }) {
            review("Android target is package-only instead of explicit package/activity.")
        }
        if ("%ANDROIDPACKAGE%" in command.value) {
            review("Uses `%ANDROIDPACKAGE%`, which would expand to dem3ux in preset mode.")
        }
        if ("%INJECT%" in command.value || "%FILEINJECT%" in command.value) {
            review("Uses injected file content.")
        }
        if (mimeTypeRegex.containsMatchIn(command.value)) {
            review("Uses explicit MIME type.")
        }
    }

    return CommandAnalysis(
        system = system,
        command = command,
        emulatorName = emulatorName,
        targetPackages = targetPackages,
        action =
            actionRegex
                .find(command.value)
                ?.groupValues
                ?.get(1)
                ?.unquote(),
        category =
            categoryRegex
                .find(command.value)
                ?.groupValues
                ?.get(1)
                ?.unquote(),
        mimeType =
            mimeTypeRegex
                .find(command.value)
                ?.groupValues
                ?.get(1)
                ?.unquote(),
        carriers = carriers,
        flags = command.value.flags(),
        status = status,
        reasons = reasons.ifEmpty { listOf("No blocking issues detected.") },
        supportedPreset = emulatorName?.let(supportedPresets::get),
    )
}

private fun readSupportedPresets(file: File): Map<String, SupportedPreset> {
    if (!file.exists()) return emptyMap()

    return presetObjectRegex
        .findAll(file.readText())
        .mapNotNull { match -> match.value.toSupportedPresetEntry() }
        .toMap()
}

private fun String.toSupportedPresetEntry(): Pair<String, SupportedPreset>? {
    val emulatorName = esDeEmulatorRegex.find(this)?.groupValues?.get(1) ?: return null
    val aliasClassName = aliasClassNameRegex.find(this)?.groupValues?.get(1) ?: return null
    val status = statusRegex.find(this)?.groupValues?.get(1) ?: "unknown"

    return emulatorName to SupportedPreset(aliasClassName = aliasClassName, status = status)
}

private val presetObjectRegex = Regex("\\{(?:(?!\\n        \\{).)*?\\n        \\}", RegexOption.DOT_MATCHES_ALL)
private val aliasClassNameRegex = Regex("\"aliasClassName\"\\s*:\\s*\"([^\"]+)\"")
private val esDeEmulatorRegex = Regex("\"esDe\"\\s*:\\s*\\{\\s*\"emulator\"\\s*:\\s*\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
private val statusRegex = Regex("\"status\"\\s*:\\s*\"([^\"]+)\"")

private fun String.romCarriers(): List<RomCarrier> =
    buildList {
        dataRegex.findAll(this@romCarriers).forEach { match ->
            val value = match.groupValues[1].unquote()
            if (value.containsRomVariable()) {
                add(RomCarrier(kind = "data", value = value, embedded = value.hasEmbeddedRomVariable()))
            }
        }

        stringExtraRegex.findAll(this@romCarriers).forEach { match ->
            val value = match.groupValues[2].unquote()
            if (value.containsRomVariable()) {
                add(
                    RomCarrier(
                        kind = "extra",
                        key = match.groupValues[1],
                        value = value,
                        embedded = value.hasEmbeddedRomVariable(),
                    ),
                )
            }
        }

        stringArrayExtraRegex.findAll(this@romCarriers).forEach { match ->
            val value = match.groupValues[2].unquote()
            if (value.containsRomVariable()) {
                add(
                    RomCarrier(
                        kind = "extraArray",
                        key = match.groupValues[1],
                        value = value,
                        embedded = value.hasEmbeddedRomVariable(),
                    ),
                )
            }
        }
    }

private val romVariables = listOf("%ROM%", "%ROMSAF%", "%ROMPROVIDER%", "%ROMRAW%", "%ROMRAWWIN%")

private fun String.containsRomVariable(): Boolean = romVariables.any { variable -> variable in this }

private fun String.hasEmbeddedRomVariable(): Boolean = this !in romVariables

private fun String.flags(): List<String> =
    buildList {
        if ("%ACTIVITY_CLEAR_TASK%" in this@flags) add("clearTask")
        if ("%ACTIVITY_CLEAR_TOP%" in this@flags) add("clearTop")
        if ("%ACTIVITY_NO_HISTORY%" in this@flags) add("noHistory")
    }

private fun String.unquote(): String = removeSurrounding("\"")

private fun suggestedAlias(emulatorName: String): String =
    emulatorName
        .lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter(String::isNotBlank)
        .joinToString(separator = "") { part -> part.replaceFirstChar(Char::uppercaseChar) }
        .let { name -> ".presets.${name}BridgeActivity" }

private fun printReport(analyses: List<CommandAnalysis>) {
    val m3uSystems = analyses.map { analysis -> analysis.system.name }.distinct().size
    val supportedAnalyses = analyses.filter { analysis -> analysis.supportedPreset != null }
    val supportedUniqueEmulators = supportedAnalyses.mapNotNull { analysis -> analysis.emulatorName }.distinct().size
    println("# ES-DE dem3ux Preset Discovery")
    println()
    println("Source:")
    println("- es_systems.xml: $esSystemsUrl")
    println("- es_find_rules.xml: $esFindRulesUrl")
    println()
    println("## Summary")
    println()
    println("- Systems with .m3u: $m3uSystems")
    println("- Commands scanned: ${analyses.size}")
    println("- Commands covered by current dem3ux presets: ${supportedAnalyses.size}")
    println("- Unique ES-DE emulators covered by current dem3ux presets: $supportedUniqueEmulators")
    supportedAnalyses
        .mapNotNull { analysis -> analysis.supportedPreset?.status }
        .groupingBy { status -> status }
        .eachCount()
        .entries
        .sortedBy { entry -> entry.key }
        .forEach { (status, count) -> println("- Commands covered by $status presets: $count") }
    Status.entries.forEach { status ->
        println("- ${status.title}: ${analyses.count { analysis -> analysis.status == status }}")
    }
    println()
    printReasonBreakdown(analyses)
    println()

    Status.entries.forEach { status ->
        val sectionAnalyses = analyses.filter { analysis -> analysis.status == status }
        if (sectionAnalyses.isNotEmpty()) {
            println("## ${status.title}")
            println()
            sectionAnalyses.forEach(::printAnalysis)
        }
    }
}

private fun printReasonBreakdown(analyses: List<CommandAnalysis>) {
    println("## Reason Breakdown")
    println()
    Status.entries.forEach { status ->
        val reasonCounts =
            analyses
                .filter { analysis -> analysis.status == status }
                .flatMap { analysis -> analysis.reasons }
                .groupingBy { reason -> reason }
                .eachCount()
                .entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { entry -> entry.value }.thenBy { entry -> entry.key })

        if (reasonCounts.isNotEmpty()) {
            println("### ${status.title}")
            println()
            reasonCounts.forEach { (reason, count) ->
                println("- $reason: $count")
            }
            println()
        }
    }
}

private fun printAnalysis(analysis: CommandAnalysis) {
    val title = analysis.emulatorName ?: analysis.command.label
    println("### $title")
    println()
    println("- System: ${analysis.system.fullName.ifBlank { analysis.system.name }}")
    println("- Command label: ${analysis.command.label}")
    println("- Target: ${analysis.targetPackages.firstOrNull() ?: "unknown"}")
    if (analysis.targetPackages.size > 1) {
        println("- Target candidates: ${analysis.targetPackages.joinToString()}")
    }
    println("- Input: ${analysis.carriers.joinToString { carrier -> carrier.display }.ifBlank { "unknown" }}")
    println("- Action: ${analysis.action ?: "none"}")
    println("- Category: ${analysis.category ?: "none"}")
    println("- MIME type: ${analysis.mimeType ?: "none"}")
    println("- Flags: ${analysis.flags.joinToString().ifBlank { "none" }}")
    analysis.supportedPreset?.let { preset ->
        println("- dem3ux preset: supported (${preset.status}), `${preset.aliasClassName}`")
    }
    if (analysis.status == Status.Likely && analysis.emulatorName != null) {
        println("- Suggested alias: ${suggestedAlias(analysis.emulatorName)}")
    }
    println("- Reasons: ${analysis.reasons.joinToString()}")
    println("- Command: `${analysis.command.value}`")
    println()
}

run {
    val systemsXml = fetch(esSystemsUrl)
    val findRulesXml = fetch(esFindRulesUrl)
    val emulatorRules = parseEmulatorRules(findRulesXml)
    val supportedPresets = readSupportedPresets(bridgePresetCatalogFile)
    val analyses =
        parseSystems(systemsXml)
            .filter { system -> ".m3u" in system.extensions }
            .flatMap { system ->
                system.commands.map { command ->
                    analyzeCommand(
                        system = system,
                        command = command,
                        emulatorRules = emulatorRules,
                        supportedPresets = supportedPresets,
                    )
                }
            }

    printReport(analyses)
}
