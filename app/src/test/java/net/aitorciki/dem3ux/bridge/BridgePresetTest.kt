package net.aitorciki.dem3ux.bridge

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
class BridgePresetTest {
    @Test
    fun `DuckStation preset reads bridge input from bootPath extra`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u"
        val sourceIntent = Intent().putExtra("bootPath", inputPath)

        assertEquals(inputPath, BridgePresets.duckStation.inputPathFrom(sourceIntent))
    }

    @Test
    fun `DuckStation preset does not fall back to data when bootPath is missing`() {
        val sourceIntent =
            Intent().setData(
                "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u".toUri(),
            )

        assertNull(BridgePresets.duckStation.inputPathFrom(sourceIntent))
    }

    @Test
    fun `DuckStation preset maps alias to real target component`() {
        val preset = BridgePresets.fromAliasClassName("net.aitorciki.dem3ux.presets.DuckStationBridgeActivity")

        assertEquals(BridgePresets.duckStation, preset)
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
                targetComponent = requireNotNull(BridgePresets.duckStation.targetComponent),
                targetAction = BridgePresets.duckStation.targetAction,
                inputPath = inputPath,
                selectedEntry = selectedEntry,
            )

        assertEquals(BridgePresets.duckStation.targetComponent, targetIntent.component)
        assertEquals(selectedEntry, targetIntent.getStringExtra("bootPath"))
        assertEquals(false, targetIntent.getBooleanExtra("resumeState", true))
        assertNull(targetIntent.data)
    }
}
