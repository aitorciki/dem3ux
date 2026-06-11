package net.aitorciki.dem3ux.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SourcePathLabelTest {
    @Test
    fun `formats filesystem path as tail label`() {
        val label = SourcePathLabel.format("/storage/emulated/0/roms/psx/Nebula Drift (Demo).m3u")

        assertEquals(".../roms/psx/Nebula Drift (Demo).m3u", label)
    }

    @Test
    fun `formats external storage document uri as decoded document tail label`() {
        val label =
            SourcePathLabel.format(
                "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FArcade%20Sampler.m3u",
            )

        assertEquals(".../roms/psx/Arcade Sampler.m3u", label)
    }

    @Test
    fun `formats opaque uri using decoded last segment`() {
        val label = SourcePathLabel.format("content://provider/tree/Library%20Playlist.m3u")

        assertEquals(".../provider/tree/Library Playlist.m3u", label)
    }
}
