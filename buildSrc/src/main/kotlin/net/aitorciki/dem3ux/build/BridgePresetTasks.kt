package net.aitorciki.dem3ux.build

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateBridgePresetsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val catalogFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val catalog = catalogFile.get().asFile.readBridgePresetCatalog()
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(catalog.generatedPresetBridgesSource().toString())
    }
}

abstract class GenerateBridgePresetManifestTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val catalogFile: RegularFileProperty

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val catalog = catalogFile.get().asFile.readBridgePresetCatalog()
        val output = manifestFile.get().asFile
        output.parentFile.mkdirs()
        output.writeText(catalog.generatedPresetManifestXml())
    }
}

abstract class ValidateBridgePresetAliasesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val catalogFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedManifestFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val mainManifestFile: RegularFileProperty

    @TaskAction
    fun validate() {
        val catalog = catalogFile.get().asFile.readBridgePresetCatalog()
        val expectedAliases =
            catalog
                .sorted()
                .presets
                .map { preset -> preset.aliasClassName }
                .toSet()
        val actualAliases =
            Regex("android:name=\"(\\.presets\\.[^\"]+)\"")
                .findAll(generatedManifestFile.get().asFile.readText())
                .map { match -> "net.aitorciki.dem3ux${match.groupValues[1]}" }
                .toSet()
        val mainManifestAliases = Regex("android:name=\"\\.presets\\.[^\"]+\"").findAll(mainManifestFile.get().asFile.readText()).toList()

        val missingAliases = expectedAliases - actualAliases
        val extraAliases = actualAliases - expectedAliases
        val isCatalogSorted = catalog.presets == catalog.sorted().presets

        if (missingAliases.isNotEmpty() || extraAliases.isNotEmpty() || mainManifestAliases.isNotEmpty() || !isCatalogSorted) {
            throw GradleException(
                buildString {
                    appendLine("Bridge preset validation failed.")
                    if (!isCatalogSorted) appendLine("presets/bridge-presets.json must be sorted by preset id.")
                    if (missingAliases.isNotEmpty()) appendLine("Missing aliases: ${missingAliases.sorted().joinToString()}")
                    if (extraAliases.isNotEmpty()) appendLine("Extra aliases: ${extraAliases.sorted().joinToString()}")
                    if (mainManifestAliases.isNotEmpty()) {
                        appendLine(
                            "Preset aliases must be generated, not declared in app/src/main/AndroidManifest.xml.",
                        )
                    }
                },
            )
        }
    }
}

@Serializable
data class BridgePresetCatalog(
    val presets: List<BridgePresetCatalogEntry>,
)

@Serializable
data class BridgePresetCatalogEntry(
    val id: String,
    val displayName: String,
    val aliasClassName: String,
    val targetActivities: List<String>,
    val input: BridgePresetInput,
    val integrations: BridgePresetIntegrations? = null,
    val status: String? = null,
) {
    val inputExtraKey: String? = input.key.takeIf { input.type == "extra" }
    val inputExtraPatterns: List<BridgePresetInputPattern> =
        input.patterns
            .orEmpty()
            .takeIf { input.type == "extraPattern" }
            .orEmpty()
    val propertyName: String = displayName.presetPropertyName()
}

@Serializable
data class BridgePresetInput(
    val type: String,
    val key: String? = null,
    val patterns: List<BridgePresetInputPattern>? = null,
)

@Serializable
data class BridgePresetInputPattern(
    val regex: String,
    val group: Int = 1,
)

@Serializable
data class BridgePresetIntegrations(
    val esDe: BridgePresetEsDeIntegration? = null,
)

@Serializable
data class BridgePresetEsDeIntegration(
    val emulator: String,
)

@Serializable
@XmlSerialName("manifest", "", "")
private data class PresetManifest(
    val queries: PresetManifestQueries,
    val application: PresetManifestApplication,
)

@Serializable
@XmlSerialName("queries", "", "")
private data class PresetManifestQueries(
    @SerialName("package")
    val packages: List<PresetManifestPackage>,
)

@Serializable
@XmlSerialName("package", "", "")
private data class PresetManifestPackage(
    @XmlElement(false)
    @XmlSerialName("name", ANDROID_XML_NAMESPACE, "android")
    val name: String,
)

@Serializable
@XmlSerialName("application", "", "")
private data class PresetManifestApplication(
    @SerialName("activity-alias")
    val aliases: List<PresetManifestActivityAlias>,
)

@Serializable
@XmlSerialName("activity-alias", "", "")
private data class PresetManifestActivityAlias(
    @XmlElement(false)
    @XmlSerialName("name", ANDROID_XML_NAMESPACE, "android")
    val name: String,
    @XmlElement(false)
    @XmlSerialName("targetActivity", ANDROID_XML_NAMESPACE, "android")
    val targetActivity: String,
    @XmlElement(false)
    @XmlSerialName("exported", ANDROID_XML_NAMESPACE, "android")
    val exported: Boolean,
)

private const val ANDROID_XML_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val PRESET_BRIDGE_PACKAGE = "net.aitorciki.dem3ux.bridge"

internal val bridgePresetJson =
    Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

private val xml =
    XML {
        indentString = "    "
        repairNamespaces = true
    }

internal fun java.io.File.readBridgePresetCatalog(): BridgePresetCatalog =
    bridgePresetJson.decodeFromString<BridgePresetCatalog>(readText())

internal fun BridgePresetCatalog.writeTo(file: java.io.File) {
    file.parentFile.mkdirs()
    file.writeText(bridgePresetJson.encodeToString(BridgePresetCatalog.serializer(), sorted()).plus("\n"))
}

private fun BridgePresetCatalog.sorted(): BridgePresetCatalog = copy(presets = presets.sortedBy { preset -> preset.id })

private fun BridgePresetCatalog.generatedPresetBridgesSource(): FileSpec {
    val sortedCatalog = sorted()
    val presetBridgeClass = ClassName(PRESET_BRIDGE_PACKAGE, "PresetBridge")
    val presetBridges =
        TypeSpec
            .objectBuilder("PresetBridges")
            .addModifiers(KModifier.INTERNAL)

    sortedCatalog.presets.forEach { preset ->
        presetBridges.addProperty(
            PropertySpec
                .builder(preset.propertyName, presetBridgeClass)
                .addModifiers(KModifier.INTERNAL)
                .initializer("\n%L", preset.presetBridgeInitializer(presetBridgeClass))
                .build(),
        )
    }

    presetBridges.addProperty(
        PropertySpec
            .builder("all", ClassName("kotlin.collections", "List").parameterizedBy(presetBridgeClass))
            .addModifiers(KModifier.INTERNAL)
            .initializer(sortedCatalog.allPresetsCode())
            .build(),
    )

    presetBridges.addFunction(
        com.squareup.kotlinpoet.FunSpec
            .builder("fromAliasClassName")
            .addModifiers(KModifier.INTERNAL)
            .addParameter("className", String::class)
            .returns(presetBridgeClass.copy(nullable = true))
            .addCode(sortedCatalog.aliasLookupCode())
            .build(),
    )

    return FileSpec
        .builder(PRESET_BRIDGE_PACKAGE, "PresetBridges")
        .indent("    ")
        .addType(presetBridges.build())
        .build()
}

private fun BridgePresetCatalogEntry.presetBridgeInitializer(presetBridgeClass: ClassName): CodeBlock =
    CodeBlock
        .builder()
        .add("%T(\n", presetBridgeClass)
        .indent()
        .add("id = %S,\n", id)
        .add("displayName = %S,\n", displayName)
        .add("aliasClassName = %S,\n", aliasClassName)
        .add("targetActivities = %L,\n", targetActivities.listOfStringsCode())
        .apply {
            if (inputExtraKey != null) {
                add("inputExtraKey = %S,\n", inputExtraKey)
            }
            if (inputExtraPatterns.isNotEmpty()) {
                add("inputExtraPatterns = %L,\n", inputExtraPatternsCode())
            }
            integrations?.esDe?.emulator?.let { emulator -> add("esDeEmulatorName = %S,\n", emulator) }
        }.unindent()
        .add(")")
        .build()

private fun BridgePresetCatalogEntry.inputExtraPatternsCode(): CodeBlock {
    val patternClass = ClassName(PRESET_BRIDGE_PACKAGE, "EmbeddedExtraPattern")
    val inputKey = requireNotNull(input.key)

    return CodeBlock
        .builder()
        .add("%M(\n", LIST_OF)
        .indent()
        .apply {
            inputExtraPatterns.forEach { pattern ->
                add(
                    "%T(key = %S, regex = %S, group = %L),\n",
                    patternClass,
                    inputKey,
                    pattern.regex,
                    pattern.group,
                )
            }
        }.unindent()
        .add(")")
        .build()
}

private fun List<String>.listOfStringsCode(): CodeBlock =
    CodeBlock
        .builder()
        .add("%M(\n", LIST_OF)
        .indent()
        .apply {
            forEach { value -> add("%S,\n", value) }
        }.unindent()
        .add(")")
        .build()

private fun BridgePresetCatalog.aliasLookupCode(): CodeBlock =
    CodeBlock
        .builder()
        .beginControlFlow("return when (className)")
        .apply {
            presets.forEach { preset -> addStatement("%S -> %N", preset.aliasClassName, preset.propertyName) }
            addStatement("else -> null")
        }.endControlFlow()
        .build()

private fun BridgePresetCatalog.allPresetsCode(): CodeBlock =
    CodeBlock
        .builder()
        .add("%M(\n", LIST_OF)
        .indent()
        .apply {
            sorted().presets.forEach { preset -> add("%N,\n", preset.propertyName) }
        }.unindent()
        .add(")")
        .build()

private val LIST_OF = MemberName("kotlin.collections", "listOf")

private fun BridgePresetCatalog.generatedPresetManifestXml(): String =
    xml
        .encodeToString(
            PresetManifest.serializer(),
            PresetManifest(
                queries =
                    PresetManifestQueries(
                        packages = sorted().targetPackageNames().map { packageName -> PresetManifestPackage(name = packageName) },
                    ),
                application =
                    PresetManifestApplication(
                        aliases =
                            sorted().presets.map { preset ->
                                PresetManifestActivityAlias(
                                    name = preset.aliasClassName.manifestAliasName(),
                                    targetActivity = ".PresetBridgeActivity",
                                    exported = true,
                                )
                            },
                    ),
            ),
        ).prependXmlDeclaration()

private fun BridgePresetCatalog.targetPackageNames(): List<String> =
    presets
        .flatMap { preset -> preset.targetActivities }
        .mapNotNull { targetActivity -> targetActivity.substringBefore('/').takeIf(String::isNotBlank) }
        .distinct()
        .sorted()

private fun String.prependXmlDeclaration(): String = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$this\n"

private fun String.presetPropertyName(): String =
    split(Regex("[^A-Za-z0-9]+"))
        .filter(String::isNotBlank)
        .mapIndexed { index, part ->
            if (index == 0) {
                part.replaceFirstChar(Char::lowercaseChar)
            } else {
                part.replaceFirstChar(Char::uppercaseChar)
            }
        }.joinToString(separator = "")

private fun String.manifestAliasName(): String = removePrefix("net.aitorciki.dem3ux")
