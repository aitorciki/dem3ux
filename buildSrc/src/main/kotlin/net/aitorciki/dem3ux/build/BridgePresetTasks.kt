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
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
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
        catalog.validateOrThrow()
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
        catalog.validateOrThrow()
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
        catalog.validateOrThrow()
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
    XML.v1 {
        indentString = "    "
        repairNamespaces = true
        xmlDeclMode = XmlDeclMode.None
        xmlVersion = XmlVersion.XML10
    }

internal fun java.io.File.readBridgePresetCatalog(): BridgePresetCatalog =
    bridgePresetJson.decodeFromString<BridgePresetCatalog>(readText())

internal fun BridgePresetCatalog.writeTo(file: java.io.File) {
    validateOrThrow()
    file.parentFile.mkdirs()
    file.writeText(bridgePresetJson.encodeToString(BridgePresetCatalog.serializer(), sorted()).plus("\n"))
}

internal fun BridgePresetCatalog.validateOrThrow() {
    val errors = validationErrors()
    if (errors.isNotEmpty()) {
        throw GradleException(
            buildString {
                appendLine("Bridge preset catalog validation failed.")
                errors.forEach { error -> appendLine("- $error") }
            },
        )
    }
}

internal fun BridgePresetCatalog.validationErrors(): List<String> =
    buildList {
        if (presets.isEmpty()) {
            add("Catalog must contain at least one preset.")
        }

        addDuplicateErrors(
            label = "preset id",
            values = presets.map { preset -> preset.id },
        )
        addDuplicateErrors(
            label = "alias class name",
            values = presets.map { preset -> preset.aliasClassName },
        )
        addDuplicateErrors(
            label = "ES-DE emulator name",
            values = presets.mapNotNull { preset -> preset.integrations?.esDe?.emulator },
        )

        if (presets != presets.sortedBy { preset -> preset.id }) {
            add("presets/bridge-presets.json must be sorted by preset id.")
        }

        presets.forEach { preset -> addAll(preset.validationErrors()) }
    }

private fun MutableList<String>.addDuplicateErrors(
    label: String,
    values: List<String>,
) {
    values
        .groupingBy { value -> value }
        .eachCount()
        .filterValues { count -> count > 1 }
        .keys
        .sorted()
        .forEach { value -> add("Duplicate $label: $value") }
}

private fun BridgePresetCatalogEntry.validationErrors(): List<String> =
    buildList {
        val prefix = "Preset '$id'"

        if (id.isBlank()) {
            add("Preset id must not be blank.")
        } else if (!id.matches(PRESET_ID_REGEX)) {
            add("$prefix id must match ${PRESET_ID_REGEX.pattern}.")
        }

        if (displayName.isBlank()) {
            add("$prefix displayName must not be blank.")
        }

        if (aliasClassName.isBlank()) {
            add("$prefix aliasClassName must not be blank.")
        } else {
            if (!aliasClassName.startsWith(PRESET_ALIAS_PACKAGE_PREFIX)) {
                add("$prefix aliasClassName must start with $PRESET_ALIAS_PACKAGE_PREFIX.")
            }
            if (!aliasClassName.endsWith(PRESET_ALIAS_SUFFIX)) {
                add("$prefix aliasClassName must end with $PRESET_ALIAS_SUFFIX.")
            }
        }

        if (targetActivities.isEmpty()) {
            add("$prefix targetActivities must not be empty.")
        }
        targetActivities.forEach { targetActivity ->
            if (!targetActivity.isValidFlattenedComponent()) {
                add("$prefix target activity is not a valid flattened component: $targetActivity")
            }
        }

        status?.let { value ->
            if (value !in ALLOWED_PRESET_STATUSES) {
                add("$prefix status must be one of ${ALLOWED_PRESET_STATUSES.sorted().joinToString()}.")
            }
        }

        integrations?.esDe?.emulator?.let { emulator ->
            if (emulator.isBlank()) {
                add("$prefix ES-DE emulator name must not be blank.")
            }
        }

        addAll(input.validationErrors(prefix))
    }

private fun BridgePresetInput.validationErrors(prefix: String): List<String> =
    buildList {
        when (type) {
            INPUT_TYPE_DATA -> {
                if (key != null) {
                    add("$prefix data input must not define key.")
                }
                if (patterns != null) {
                    add("$prefix data input must not define patterns.")
                }
            }

            INPUT_TYPE_EXTRA -> {
                if (key.isNullOrBlank()) {
                    add("$prefix extra input must define a non-blank key.")
                }
                if (patterns != null) {
                    add("$prefix extra input must not define patterns.")
                }
            }

            INPUT_TYPE_EXTRA_PATTERN -> {
                if (key.isNullOrBlank()) {
                    add("$prefix extraPattern input must define a non-blank key.")
                }
                if (patterns.isNullOrEmpty()) {
                    add("$prefix extraPattern input must define at least one pattern.")
                }
                patterns.orEmpty().forEachIndexed { index, pattern ->
                    addAll(pattern.validationErrors(prefix = prefix, index = index))
                }
            }

            else -> {
                add("$prefix input type must be one of ${ALLOWED_INPUT_TYPES.sorted().joinToString()}.")
            }
        }
    }

private fun BridgePresetInputPattern.validationErrors(
    prefix: String,
    index: Int,
): List<String> =
    buildList {
        val patternPrefix = "$prefix input pattern #${index + 1}"
        if (regex.isBlank()) {
            add("$patternPrefix regex must not be blank.")
            return@buildList
        }
        if (group < 1) {
            add("$patternPrefix group must be >= 1.")
        }

        val compiledPattern =
            runCatching { Regex(regex).toPattern() }
                .onFailure { error -> add("$patternPrefix regex is invalid: ${error.message}") }
                .getOrNull()

        val groupCount = compiledPattern?.matcher("")?.groupCount()
        if (groupCount != null && group > groupCount) {
            add("$patternPrefix group $group exceeds regex capture group count $groupCount.")
        }
    }

private fun String.isValidFlattenedComponent(): Boolean {
    val separatorIndex = indexOf('/')
    if (separatorIndex <= 0 || separatorIndex != lastIndexOf('/') || separatorIndex == lastIndex) {
        return false
    }

    val packageName = substring(0, separatorIndex)
    val className = substring(separatorIndex + 1)
    return packageName.isValidJavaPackageName() && className.isValidAndroidClassName()
}

private fun String.isValidJavaPackageName(): Boolean = split('.').all { segment -> segment.isValidJavaIdentifier() }

private fun String.isValidAndroidClassName(): Boolean =
    when {
        startsWith(".") -> drop(1).isValidRelativeClassName()
        else -> isValidRelativeClassName()
    }

private fun String.isValidRelativeClassName(): Boolean = split('.').all { segment -> segment.isValidJavaIdentifier() }

private fun String.isValidJavaIdentifier(): Boolean =
    isNotEmpty() && first().isJavaIdentifierStart() && drop(1).all { character -> character.isJavaIdentifierPart() }

private val PRESET_ID_REGEX = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
private const val PRESET_ALIAS_PACKAGE_PREFIX = "net.aitorciki.dem3ux.presets."
private const val PRESET_ALIAS_SUFFIX = "BridgeActivity"
private const val INPUT_TYPE_DATA = "data"
private const val INPUT_TYPE_EXTRA = "extra"
private const val INPUT_TYPE_EXTRA_PATTERN = "extraPattern"
private val ALLOWED_INPUT_TYPES = setOf(INPUT_TYPE_DATA, INPUT_TYPE_EXTRA, INPUT_TYPE_EXTRA_PATTERN)
private val ALLOWED_PRESET_STATUSES = setOf("generated", "validated")

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
