package net.aitorciki.dem3ux

import android.content.Intent
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PresetBridgeActivityTest {
    @Test
    fun `unknown preset alias does not fall back to DuckStation`() {
        val sourceIntent =
            Intent().putExtra(
                "bootPath",
                "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u",
            )

        val bridgeLaunch =
            createPresetBridgeLaunch(
                sourceIntent = sourceIntent,
                aliasClassName = "net.aitorciki.dem3ux.presets.UnknownBridgeActivity",
            )

        assertNull(bridgeLaunch)
    }
}
