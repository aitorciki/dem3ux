package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
                inputPath = inputPath,
                selectedEntry = selectedEntry,
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
    fun `preset ignores invalid target candidate entries`() {
        val preset =
            PresetBridge(
                id = "test",
                aliasClassName = "net.aitorciki.dem3ux.presets.TestBridgeActivity",
                targetActivities =
                    listOf(
                        "not-a-flattened-component",
                        "com.example/.TargetActivity",
                    ),
            )

        assertEquals(ComponentName("com.example", "com.example.TargetActivity"), preset.targetComponent)
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
                inputPath = inputPath,
                selectedEntry = selectedEntry,
            )

        assertEquals(Intent.ACTION_VIEW, targetIntent.action)
        assertEquals(PresetBridges.flycast.targetComponent, targetIntent.component)
        assertEquals(selectedEntry, targetIntent.data.toString())
    }
}
