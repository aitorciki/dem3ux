package net.aitorciki.dem3ux.bridge

import android.content.Intent
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PresetBridgeTest {
    @Test
    fun `DuckStation preset reads bridge input from bootPath extra`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u"
        val sourceIntent = Intent().putExtra("bootPath", inputPath)

        assertEquals(inputPath, PresetBridges.duckStation.inputPathFrom(sourceIntent))
    }

    @Test
    fun `DuckStation preset does not fall back to data when bootPath is missing`() {
        val sourceIntent =
            Intent().setData(
                "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u".toUri(),
            )

        assertNull(PresetBridges.duckStation.inputPathFrom(sourceIntent))
    }

    @Test
    fun `DuckStation preset maps alias to real target component`() {
        val preset = PresetBridges.fromAliasClassName("net.aitorciki.dem3ux.presets.DuckStationBridgeActivity")

        assertEquals(PresetBridges.duckStation, preset)
        assertEquals("com.github.stenzek.duckstation", preset?.targetComponent?.packageName)
        assertEquals("com.github.stenzek.duckstation.EmulationActivity", preset?.targetComponent?.className)
    }

    @Test
    fun `DuckStation preset keeps native bootPath intent shape`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u"
        val selectedEntry = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd"
        val sourceIntent =
            Intent()
                .putExtra("bootPath", inputPath)
                .putExtra("resumeState", false)

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = requireNotNull(PresetBridges.duckStation.targetComponent),
                inputPath = BridgeInputPath(inputPath),
                selectedEntry = SelectedEntryPath(selectedEntry),
            )

        assertNull(targetIntent.action)
        assertEquals(PresetBridges.duckStation.targetComponent, targetIntent.component)
        assertEquals(selectedEntry, targetIntent.getStringExtra("bootPath"))
        assertEquals(false, targetIntent.getBooleanExtra("resumeState", true))
        assertNull(targetIntent.data)
    }

    @Test
    fun `Flycast preset reads bridge input from intent data`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fdc%2FGame.m3u"
        val sourceIntent = Intent().setData(inputPath.toUri())

        assertEquals(inputPath, PresetBridges.flycast.inputPathFrom(sourceIntent))
    }

    @Test
    fun `Flycast preset maps alias to real target component`() {
        val preset = PresetBridges.fromAliasClassName("net.aitorciki.dem3ux.presets.FlycastBridgeActivity")

        assertEquals(PresetBridges.flycast, preset)
        assertEquals("com.flycast.emulator", preset?.targetComponent?.packageName)
        assertEquals("com.flycast.emulator.MainActivity", preset?.targetComponent?.className)
    }

    @Test
    fun `Flycast preset resolves first installed target candidate`() {
        val targetComponent =
            PresetBridges.flycast.resolveTargetComponent { component ->
                component.className == "com.flycast.emulator.MainActivity"
            }

        assertEquals("com.flycast.emulator.MainActivity", targetComponent?.className)
    }

    @Test
    fun `Flycast preset falls back to second installed target candidate`() {
        val targetComponent =
            PresetBridges.flycast.resolveTargetComponent { component ->
                component.className == "com.reicast.emulator.MainActivity"
            }

        assertEquals("com.reicast.emulator.MainActivity", targetComponent?.className)
    }

    @Test
    fun `Flycast preset returns null when no target candidate is installed`() {
        assertNull(PresetBridges.flycast.resolveTargetComponent { false })
    }

    @Test
    fun `preset rejects invalid target candidate entries`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                PresetBridge(
                    id = "test",
                    aliasClassName = "net.aitorciki.dem3ux.presets.TestBridgeActivity",
                    targetActivities =
                        listOf(
                            "not-a-flattened-component",
                            "com.example/.TargetActivity",
                        ),
                ).targetComponents
            }

        assertEquals("Invalid preset target activity: not-a-flattened-component", error.message)
    }

    @Test
    fun `preset maps valid target candidate entries`() {
        val preset =
            PresetBridge(
                id = "test",
                aliasClassName = "net.aitorciki.dem3ux.presets.TestBridgeActivity",
                targetActivities = listOf("com.example/.TargetActivity"),
            )

        assertEquals("com.example.TargetActivity", preset.targetComponent?.className)
    }

    @Test
    fun `embedded extra pattern extracts quoted filesystem path`() {
        val preset = mame4DroidPreset()
        val inputPath = "/storage/emulated/0/roms/cpc/Game Disk.m3u"
        val sourceIntent = Intent().putExtra("cli_params", "-rompath '/roms;cpc' -flop1 '$inputPath'")

        assertEquals(inputPath, preset.inputFrom(sourceIntent)?.inputPath?.raw)
    }

    @Test
    fun `embedded extra pattern extracts quoted SAF uri`() {
        val preset = mame4DroidPreset()
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fcpc%2FGame.m3u"
        val sourceIntent = Intent().putExtra("cli_params", "-rompath '/roms;cpc' -flop1 '$inputPath'")

        assertEquals(inputPath, preset.inputFrom(sourceIntent)?.inputPath?.raw)
    }

    @Test
    fun `embedded extra pattern extracts quoted FileProvider uri`() {
        val preset = mame4DroidPreset()
        val inputPath = "content://org.es_de.frontend.files/external/Documents/roms/cpc/Game.m3u"
        val sourceIntent = Intent().putExtra("cli_params", "-rompath '/roms;cpc' -flop1 '$inputPath'")

        assertEquals(inputPath, preset.inputFrom(sourceIntent)?.inputPath?.raw)
    }

    @Test
    fun `Flycast preset uses selected entry as target data with view action`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fdc%2FGame.m3u"
        val selectedEntry = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fdc%2FDisc%201.chd"
        val sourceIntent = Intent(Intent.ACTION_VIEW).setData(inputPath.toUri())

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = requireNotNull(PresetBridges.flycast.targetComponent),
                targetAction = sourceIntent.action,
                inputPath = BridgeInputPath(inputPath),
                selectedEntry = SelectedEntryPath(selectedEntry),
            )

        assertEquals(Intent.ACTION_VIEW, targetIntent.action)
        assertEquals(PresetBridges.flycast.targetComponent, targetIntent.component)
        assertEquals(selectedEntry, targetIntent.data.toString())
    }

    private fun mame4DroidPreset(): PresetBridge =
        PresetBridge(
            id = "mame4droid-current",
            aliasClassName = "net.aitorciki.dem3ux.presets.Mame4droidCurrentBridgeActivity",
            targetActivities = listOf("com.seleuco.mame4d2024/com.seleuco.mame4droid.MainActivity"),
            inputExtraPatterns =
                listOf(
                    EmbeddedExtraPattern(
                        key = "cli_params",
                        regex = "(?:^|\\s)${Regex.escape("-flop1")}\\s*'([^']+)'",
                    ),
                    EmbeddedExtraPattern(
                        key = "cli_params",
                        regex = "(?:^|\\s)${Regex.escape("-cart")}\\s*'([^']+)'",
                    ),
                ),
        )
}
