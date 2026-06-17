package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent
import androidx.core.net.toUri
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeTargetIntentFactoryTest {
    private val targetComponent =
        requireNotNull(ComponentName.unflattenFromString("com.github.stenzek.duckstation/.EmulationActivity"))

    @Test
    fun `replaces input path in arbitrary string extra and leaves target data unset`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u"
        val selectedEntry = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd"
        val sourceIntent =
            Intent(Intent.ACTION_VIEW)
                .putExtra("bootPath", inputPath)
                .putExtra("resumeState", false)
                .putExtra("slot", 1)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                targetAction = sourceIntent.action,
                inputPath = inputPath,
                selectedEntry = selectedEntry,
            )

        assertEquals(Intent.ACTION_VIEW, targetIntent.action)
        assertEquals(targetComponent, targetIntent.component)
        assertEquals(selectedEntry, targetIntent.getStringExtra("bootPath"))
        assertFalse(targetIntent.getBooleanExtra("resumeState", true))
        assertEquals(1, targetIntent.getIntExtra("slot", 0))
        assertEquals(
            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            targetIntent.flags and (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        assertNull(targetIntent.data)
    }

    @Test
    fun `replaces source data when it matches input path`() {
        val inputPath = "/storage/emulated/0/roms/psx/Game.m3u"
        val selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd"
        val sourceIntent = Intent().setData(inputPath.toUri())

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = inputPath,
                selectedEntry = selectedEntry,
            )

        assertEquals(selectedEntry, targetIntent.data.toString())
    }

    @Test
    fun `preserves source data when it does not match input path`() {
        val sourceIntent = Intent().setData("cpc6128".toUri())
        val selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd"

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = "/storage/emulated/0/roms/psx/Game.m3u",
                selectedEntry = selectedEntry,
            )

        assertEquals("cpc6128", targetIntent.data.toString())
    }

    @Test
    fun `leaves target data unset when no source data is provided`() {
        val sourceIntent = Intent()

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = "/storage/emulated/0/roms/psx/Game.m3u",
                selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd",
            )

        assertNull(targetIntent.data)
    }

    @Test
    fun `proxies direct rom path unchanged through forwarded extra`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd"
        val sourceIntent =
            Intent()
                .putExtra("bootPath", inputPath)

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = inputPath,
                selectedEntry = inputPath,
            )

        assertEquals(inputPath, targetIntent.getStringExtra("bootPath"))
        assertNull(targetIntent.data)
    }

    @Test
    fun `proxies supported ES-DE activity flags`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u"
        val sourceIntent =
            Intent()
                .putExtra("bootPath", inputPath)
                .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = inputPath,
                selectedEntry = inputPath,
            )

        assertEquals(Intent.FLAG_ACTIVITY_NO_HISTORY, targetIntent.flags and Intent.FLAG_ACTIVITY_NO_HISTORY)
        assertNull(targetIntent.data)
    }

    @Test
    fun `does not proxy dem3ux bridge extras`() {
        val sourceIntent =
            Intent()
                .putExtra(BridgeContract.EXTRA_TARGET_ACTIVITY, "com.github.stenzek.duckstation/.EmulationActivity")
                .putExtra(BridgeContract.EXTRA_INPUT_PATH, "/storage/emulated/0/roms/psx/Game.m3u")
                .putExtra("dem3ux.test", "reserved")

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = "/storage/emulated/0/roms/psx/Game.m3u",
                selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd",
            )

        assertFalse(targetIntent.hasExtra(BridgeContract.EXTRA_TARGET_ACTIVITY))
        assertFalse(targetIntent.hasExtra(BridgeContract.EXTRA_INPUT_PATH))
        assertFalse(targetIntent.hasExtra("dem3ux.test"))
    }

    @Test
    fun `replaces input path inside arbitrary string array extra`() {
        val inputPath = "/storage/emulated/0/roms/psx/Game.m3u"
        val selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd"
        val sourceIntent = Intent().putExtra("paths", arrayOf("--boot", inputPath))

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = inputPath,
                selectedEntry = selectedEntry,
            )

        assertArrayEquals(arrayOf("--boot", selectedEntry), targetIntent.getStringArrayExtra("paths"))
        assertNull(targetIntent.data)
    }

    @Test
    fun `does not proxy unsupported source activity flags`() {
        val sourceIntent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd"

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = "/storage/emulated/0/roms/psx/Game.m3u",
                selectedEntry = selectedEntry,
            )

        assertEquals(0, targetIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun `proxies source intent categories`() {
        val sourceIntent =
            Intent()
                .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val selectedEntry = "/storage/emulated/0/roms/gc/Game.iso"

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = "/storage/emulated/0/roms/gc/Game.m3u",
                selectedEntry = selectedEntry,
            )

        assertEquals(setOf(Intent.CATEGORY_LEANBACK_LAUNCHER), targetIntent.categories)
    }

    @Test
    fun `replaces embedded input path inside configured extra and preserves source data`() {
        val inputPath = "/storage/emulated/0/roms/cpc/Game Disk.m3u"
        val selectedEntry = "/storage/emulated/0/roms/cpc/Game Disk Side A.dsk"
        val sourceIntent =
            Intent(Intent.ACTION_VIEW)
                .setData("cpc6128".toUri())
                .putExtra("cli_params", "-rompath '/roms;cpc' -flop1 '$inputPath'")

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                targetAction = sourceIntent.action,
                inputPath = inputPath,
                selectedEntry = selectedEntry,
                embeddedExtraReplacement =
                    EmbeddedExtraPattern(
                        key = "cli_params",
                        regex = "(?:^|\\s)${Regex.escape("-flop1")}\\s*'([^']+)'",
                    ),
            )

        assertEquals(Intent.ACTION_VIEW, targetIntent.action)
        assertEquals("cpc6128", targetIntent.data.toString())
        assertEquals("-rompath '/roms;cpc' -flop1 '$selectedEntry'", targetIntent.getStringExtra("cli_params"))
    }
}
