package net.aitorciki.dem3ux.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.w3c.dom.Element
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

@UntrackedTask(because = "Fetches remote ES-DE metadata whose content is not represented by Gradle inputs.")
abstract class RefreshBridgePresetsFromEsDeTask : DefaultTask() {
    @get:Input
    abstract val esSystemsUrl: Property<String>

    @get:Input
    abstract val esFindRulesUrl: Property<String>

    @get:OutputFile
    abstract val catalogFile: RegularFileProperty

    @TaskAction
    fun refresh() {
        val systemsXml = fetch(esSystemsUrl.get())
        val findRulesXml = fetch(esFindRulesUrl.get())
        val emulatorRules = parseEmulatorRules(findRulesXml)
        val analyses =
            parseSystems(systemsXml)
                .filter { system -> ".m3u" in system.extensions }
                .flatMap { system ->
                    system.commands.map { command -> analyzeCommand(system = system, command = command, emulatorRules = emulatorRules) }
                }
        val presets = analyses.generatedPresetCatalogEntries()

        BridgePresetCatalog(presets = presets).writeTo(catalogFile.get().asFile)
        logger.lifecycle("Generated ${presets.size} bridge presets from ES-DE metadata.")
    }
}

private data class EsSystem(
    val name: String,
    val extensions: Set<String>,
    val commands: List<EsCommand>,
)

private data class EsCommand(
    val value: String,
)

private data class EmulatorRule(
    val androidPackages: List<String>,
)

private data class RomCarrier(
    val kind: String,
    val key: String? = null,
    val value: String,
    val embedded: Boolean,
)

private data class CommandAnalysis(
    val emulatorName: String?,
    val targetPackages: List<String>,
    val carriers: List<RomCarrier>,
    val status: Status,
)

private enum class Status {
    Likely,
    Review,
    Unsupported,
    Skipped,
}

private val emulatorRegex = Regex("%EMULATOR_([^%]+)%")
private val dataRegex = Regex("%DATA%=(\"[^\"]*\"|[^\\s]+)")
private val stringExtraRegex = Regex("%EXTRA_([^%\\s=]+)%=(\"[^\"]*\"|[^\\s]+)")
private val stringArrayExtraRegex = Regex("%EXTRAARRAY_([^%\\s=]+)%=(\"[^\"]*\"|[^\\s]+)")
private val mimeTypeRegex = Regex("%MIMETYPE%=(\"[^\"]*\"|[^\\s]+)")
private val romVariables = listOf("%ROM%", "%ROMSAF%", "%ROMPROVIDER%", "%ROMRAW%", "%ROMRAWWIN%")

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
                extensions =
                    system
                        .childText("extension")
                        .orEmpty()
                        .split(Regex("\\s+"))
                        .filter(String::isNotBlank)
                        .map(String::lowercase)
                        .toSet(),
                commands = system.children("command").map { command -> EsCommand(value = command.textContent.trim()) },
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

            name to EmulatorRule(androidPackages = androidPackages)
        }

private fun analyzeCommand(
    system: EsSystem,
    command: EsCommand,
    emulatorRules: Map<String, EmulatorRule>,
): CommandAnalysis {
    val emulatorName = emulatorRegex.find(command.value)?.groupValues?.get(1)
    val rule = emulatorName?.let(emulatorRules::get)
    val targetPackages = rule?.androidPackages.orEmpty()
    val carriers = command.value.romCarriers()
    var status = Status.Likely

    fun skip() {
        status = Status.Skipped
    }

    fun unsupported() {
        if (status != Status.Skipped) status = Status.Unsupported
    }

    fun review() {
        if (status == Status.Likely) status = Status.Review
    }

    when {
        emulatorName == null -> skip()
        emulatorName.contains("RETROARCH") -> skip()
        rule == null -> unsupported()
        targetPackages.isEmpty() -> unsupported()
    }

    if (status != Status.Skipped) {
        if (carriers.isEmpty()) unsupported()
        if (carriers.size > 1) review()
        if (carriers.any { carrier -> carrier.embedded }) review()
        if (targetPackages.any { target -> "/" !in target }) review()
        if ("%ANDROIDPACKAGE%" in command.value) review()
        if ("%INJECT%" in command.value || "%FILEINJECT%" in command.value) review()
        if (mimeTypeRegex.containsMatchIn(command.value)) review()
        if (system.name.isBlank()) review()
    }

    return CommandAnalysis(
        emulatorName = emulatorName,
        targetPackages = targetPackages,
        carriers = carriers,
        status = status,
    )
}

private fun String.romCarriers(): List<RomCarrier> =
    buildList {
        dataRegex.findAll(this@romCarriers).forEach { match ->
            val value = match.groupValues[1].unquote()
            if (value.containsRomVariable()) add(RomCarrier(kind = "data", value = value, embedded = value.hasEmbeddedRomVariable()))
        }

        stringExtraRegex.findAll(this@romCarriers).forEach { match ->
            val value = match.groupValues[2].unquote()
            if (value.containsRomVariable()) {
                add(RomCarrier(kind = "extra", key = match.groupValues[1], value = value, embedded = value.hasEmbeddedRomVariable()))
            }
        }

        stringArrayExtraRegex.findAll(this@romCarriers).forEach { match ->
            val value = match.groupValues[2].unquote()
            if (value.containsRomVariable()) {
                add(RomCarrier(kind = "extraArray", key = match.groupValues[1], value = value, embedded = value.hasEmbeddedRomVariable()))
            }
        }
    }

private fun List<CommandAnalysis>.generatedPresetCatalogEntries(): List<BridgePresetCatalogEntry> =
    filter { analysis -> analysis.status == Status.Likely || analysis.canGenerateEmbeddedExtraPreset() }
        .filter { analysis -> analysis.carriers.singleOrNull()?.canGeneratePresetInput() == true }
        .groupBy { analysis -> requireNotNull(analysis.emulatorName) }
        .mapNotNull { (emulatorName, analyses) ->
            val first = analyses.first()
            val input = analyses.bridgePresetInput() ?: return@mapNotNull null
            val id = emulatorName.presetId()
            val override = validatedPresetOverrides[emulatorName]
            BridgePresetCatalogEntry(
                id = id,
                displayName = override?.displayName ?: emulatorName.presetDisplayName(),
                aliasClassName = override?.aliasClassName ?: "net.aitorciki.dem3ux${emulatorName.suggestedAlias()}",
                targetActivities = first.targetPackages,
                input = input,
                integrations = BridgePresetIntegrations(esDe = BridgePresetEsDeIntegration(emulator = emulatorName)),
                status = if (override == null) "generated" else "validated",
            )
        }.sortedBy { preset -> preset.id }

private fun List<CommandAnalysis>.bridgePresetInput(): BridgePresetInput? {
    val carriers = mapNotNull { analysis -> analysis.carriers.singleOrNull() }
    val first = carriers.firstOrNull() ?: return null

    return if (carriers.all { carrier -> carrier.canMergeWith(first) }) {
        when {
            first.kind == "extra" && first.embedded -> {
                BridgePresetInput(
                    type = "extraPattern",
                    key = requireNotNull(first.key),
                    patterns =
                        carriers
                            .flatMap(RomCarrier::supportedEmbeddedExtraPatterns)
                            .distinct(),
                )
            }

            else -> {
                first.bridgePresetInput()
            }
        }
    } else {
        null
    }
}

private fun RomCarrier.canMergeWith(other: RomCarrier): Boolean =
    kind == other.kind &&
        key == other.key &&
        embedded == other.embedded

private data class ValidatedPresetOverride(
    val displayName: String,
    val aliasClassName: String,
)

private val validatedPresetOverrides =
    mapOf(
        "DUCKSTATION" to
            ValidatedPresetOverride(
                displayName = "DuckStation",
                aliasClassName = "net.aitorciki.dem3ux.presets.DuckStationBridgeActivity",
            ),
        "FLYCAST" to
            ValidatedPresetOverride(
                displayName = "Flycast",
                aliasClassName = "net.aitorciki.dem3ux.presets.FlycastBridgeActivity",
            ),
    )

private fun RomCarrier.bridgePresetInput(): BridgePresetInput =
    when (kind) {
        "data" -> {
            BridgePresetInput(type = "data")
        }

        "extra" -> {
            if (embedded) {
                BridgePresetInput(
                    type = "extraPattern",
                    key = requireNotNull(key),
                    patterns = supportedEmbeddedExtraPatterns(),
                )
            } else {
                BridgePresetInput(type = "extra", key = requireNotNull(key))
            }
        }

        else -> {
            error("Unsupported preset input carrier: $kind")
        }
    }

private fun CommandAnalysis.canGenerateEmbeddedExtraPreset(): Boolean {
    val carrier = carriers.singleOrNull() ?: return false
    return status == Status.Review && carrier.canGenerateEmbeddedExtraPresetInput()
}

private fun RomCarrier.canGeneratePresetInput(): Boolean =
    kind == "data" || (kind == "extra" && (!embedded || canGenerateEmbeddedExtraPresetInput()))

private fun RomCarrier.canGenerateEmbeddedExtraPresetInput(): Boolean =
    kind == "extra" &&
        key == "cli_params" &&
        embedded &&
        supportedEmbeddedExtraPatterns().isNotEmpty()

private fun RomCarrier.supportedEmbeddedExtraPatterns(): List<BridgePresetInputPattern> =
    mame4DroidEmbeddedInputFlags
        .filter { flag -> value.contains(flag) }
        .map { flag ->
            BridgePresetInputPattern(
                regex = "(?:^|\\s)${Regex.escape(flag)}\\s*'([^']+)'",
                group = 1,
            )
        }

private val mame4DroidEmbeddedInputFlags = listOf("-flop1", "-cart")

private fun String.containsRomVariable(): Boolean = romVariables.any { variable -> variable in this }

private fun String.hasEmbeddedRomVariable(): Boolean = this !in romVariables

private fun String.unquote(): String = removeSurrounding("\"")

private fun String.presetId(): String =
    lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter(String::isNotBlank)
        .joinToString(separator = "-")

private fun String.suggestedAlias(): String =
    lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter(String::isNotBlank)
        .joinToString(separator = "") { part -> part.replaceFirstChar(Char::uppercaseChar) }
        .let { name -> ".presets.${name}BridgeActivity" }

private fun String.presetDisplayName(): String =
    lowercase()
        .split(Regex("[^a-z0-9]+"))
        .filter(String::isNotBlank)
        .joinToString(separator = " ") { part -> part.replaceFirstChar(Char::uppercaseChar) }
