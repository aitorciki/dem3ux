package net.aitorciki.dem3ux.build

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BridgePresetCatalogValidationTest {
    @Test
    fun `current catalog is valid`() {
        val catalog = File("../presets/bridge-presets.json").readBridgePresetCatalog()

        assertEquals(emptyList(), catalog.validationErrors())
    }

    @Test
    fun `duplicate ids fail validation`() {
        val catalog = catalogOf(validPreset(id = "duckstation"), validPreset(id = "duckstation", aliasClassName = alias("Other")))

        assertContainsError(catalog, "Duplicate preset id: duckstation")
    }

    @Test
    fun `duplicate alias class names fail validation`() {
        val catalog = catalogOf(validPreset(id = "alpha"), validPreset(id = "beta"))

        assertContainsError(catalog, "Duplicate alias class name: net.aitorciki.dem3ux.presets.TestBridgeActivity")
    }

    @Test
    fun `duplicate ES-DE emulator names fail validation`() {
        val catalog =
            catalogOf(
                validPreset(id = "alpha", aliasClassName = alias("Alpha"), esDeEmulatorName = "DUCKSTATION"),
                validPreset(id = "beta", aliasClassName = alias("Beta"), esDeEmulatorName = "DUCKSTATION"),
            )

        assertContainsError(catalog, "Duplicate ES-DE emulator name: DUCKSTATION")
    }

    @Test
    fun `unsorted catalog fails validation`() {
        val catalog =
            catalogOf(validPreset(id = "beta", aliasClassName = alias("Beta")), validPreset(id = "alpha", aliasClassName = alias("Alpha")))

        assertContainsError(catalog, "presets/bridge-presets.json must be sorted by preset id.")
    }

    @Test
    fun `invalid target activity fails validation`() {
        val catalog = catalogOf(validPreset(targetActivities = listOf("not-a-flattened-component")))

        assertContainsError(catalog, "target activity is not a valid flattened component: not-a-flattened-component")
    }

    @Test
    fun `unknown input type fails validation`() {
        val catalog = catalogOf(validPreset(input = BridgePresetInput(type = "mystery")))

        assertContainsError(catalog, "input type must be one of data, extra, extraPattern")
    }

    @Test
    fun `data input with stale key fails validation`() {
        val catalog = catalogOf(validPreset(input = BridgePresetInput(type = "data", key = "bootPath")))

        assertContainsError(catalog, "data input must not define key")
    }

    @Test
    fun `extra input without key fails validation`() {
        val catalog = catalogOf(validPreset(input = BridgePresetInput(type = "extra")))

        assertContainsError(catalog, "extra input must define a non-blank key")
    }

    @Test
    fun `extra input with stale patterns fails validation`() {
        val catalog =
            catalogOf(
                validPreset(
                    input =
                        BridgePresetInput(
                            type = "extra",
                            key = "bootPath",
                            patterns = listOf(BridgePresetInputPattern(regex = "(.+)")),
                        ),
                ),
            )

        assertContainsError(catalog, "extra input must not define patterns")
    }

    @Test
    fun `extraPattern input without patterns fails validation`() {
        val catalog = catalogOf(validPreset(input = BridgePresetInput(type = "extraPattern", key = "cli_params")))

        assertContainsError(catalog, "extraPattern input must define at least one pattern")
    }

    @Test
    fun `invalid regex fails validation`() {
        val catalog =
            catalogOf(
                validPreset(
                    input =
                        BridgePresetInput(
                            type = "extraPattern",
                            key = "cli_params",
                            patterns = listOf(BridgePresetInputPattern(regex = "(")),
                        ),
                ),
            )

        assertContainsError(catalog, "regex is invalid")
    }

    @Test
    fun `regex group out of range fails validation`() {
        val catalog =
            catalogOf(
                validPreset(
                    input =
                        BridgePresetInput(
                            type = "extraPattern",
                            key = "cli_params",
                            patterns = listOf(BridgePresetInputPattern(regex = "(.+)", group = 2)),
                        ),
                ),
            )

        assertContainsError(catalog, "group 2 exceeds regex capture group count 1")
    }

    @Test
    fun `regex group zero fails validation`() {
        val catalog =
            catalogOf(
                validPreset(
                    input =
                        BridgePresetInput(
                            type = "extraPattern",
                            key = "cli_params",
                            patterns = listOf(BridgePresetInputPattern(regex = "(.+)", group = 0)),
                        ),
                ),
            )

        assertContainsError(catalog, "group must be >= 1")
    }

    private fun assertContainsError(
        catalog: BridgePresetCatalog,
        expected: String,
    ) {
        val errors = catalog.validationErrors()
        assertTrue(errors.any { error -> expected in error }, "Expected '$expected' in validation errors: $errors")
    }

    private fun catalogOf(vararg presets: BridgePresetCatalogEntry): BridgePresetCatalog = BridgePresetCatalog(presets.toList())

    private fun validPreset(
        id: String = "test",
        displayName: String = "Test",
        aliasClassName: String = alias("Test"),
        targetActivities: List<String> = listOf("com.example/.EmulationActivity"),
        input: BridgePresetInput = BridgePresetInput(type = "data"),
        esDeEmulatorName: String? = null,
        status: String? = "generated",
    ): BridgePresetCatalogEntry =
        BridgePresetCatalogEntry(
            id = id,
            displayName = displayName,
            aliasClassName = aliasClassName,
            targetActivities = targetActivities,
            input = input,
            integrations = esDeEmulatorName?.let { emulator -> BridgePresetIntegrations(esDe = BridgePresetEsDeIntegration(emulator)) },
            status = status,
        )

    private fun alias(name: String): String = "net.aitorciki.dem3ux.presets.${name}BridgeActivity"
}
