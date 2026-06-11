package net.aitorciki.dem3ux.bridge

import android.content.ComponentName
import android.content.Intent
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
    fun `replaces forwarded m3u extra with selected entry and preserves DuckStation extras`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u"
        val selectedEntry = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd"
        val sourceIntent =
            Intent()
                .putExtra(BridgeContract.EXTRA_TARGET_ACTION, Intent.ACTION_VIEW)
                .putExtra("${BridgeContract.TARGET_EXTRA_PREFIX}bootPath", inputPath)
                .putExtra("${BridgeContract.TARGET_EXTRA_PREFIX}resumeState", false)
                .putExtra(BridgeContract.TARGET_FLAG_CLEAR_TASK, true)
                .putExtra(BridgeContract.TARGET_FLAG_CLEAR_TOP, true)

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = inputPath,
                selectedEntry = selectedEntry,
            )

        assertEquals(Intent.ACTION_VIEW, targetIntent.action)
        assertEquals(targetComponent, targetIntent.component)
        assertEquals(selectedEntry, targetIntent.getStringExtra("bootPath"))
        assertFalse(targetIntent.getBooleanExtra("resumeState", true))
        assertEquals(
            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            targetIntent.flags and (Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        assertNull(targetIntent.data)
    }

    @Test
    fun `uses selected entry as data when no forwarded extra contains input path`() {
        val sourceIntent = Intent()
        val selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd"

        val targetIntent =
            BridgeTargetIntentFactory.build(
                sourceIntent = sourceIntent,
                targetComponent = targetComponent,
                inputPath = "/storage/emulated/0/roms/psx/Game.m3u",
                selectedEntry = selectedEntry,
            )

        assertEquals(selectedEntry, targetIntent.data.toString())
    }

    @Test
    fun `proxies direct rom path unchanged through forwarded extra`() {
        val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd"
        val sourceIntent =
            Intent()
                .putExtra("${BridgeContract.TARGET_EXTRA_PREFIX}bootPath", inputPath)

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
}
