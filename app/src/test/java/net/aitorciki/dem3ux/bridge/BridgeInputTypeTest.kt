package net.aitorciki.dem3ux.bridge

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeInputTypeTest {
    @Test
    fun `classifies filesystem m3u paths as playlists`() {
        assertTrue(BridgeInputType.isPlaylist("/storage/emulated/0/roms/psx/Game.m3u"))
        assertTrue(BridgeInputType.isPlaylist("/storage/emulated/0/roms/psx/Game.M3U"))
        assertTrue(BridgeInputType.isPlaylist("/storage/emulated/0/roms/psx/Game.m3u8"))
    }

    @Test
    fun `classifies encoded file uri m3u paths as playlists`() {
        assertTrue(BridgeInputType.isPlaylist("file:///storage/emulated/0/roms/psx/Nebula%20Drift.m3u"))
    }

    @Test
    fun `classifies encoded external storage document m3u paths as playlists`() {
        assertTrue(
            BridgeInputType.isPlaylist(
                "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FArcade%20Sampler.m3u",
            ),
        )
    }

    @Test
    fun `classifies direct rom images as non-playlists`() {
        assertFalse(BridgeInputType.isPlaylist("/storage/emulated/0/roms/psx/Disc 1.chd"))
        assertFalse(BridgeInputType.isPlaylist("/storage/emulated/0/roms/psx/Game.cue"))
        assertFalse(BridgeInputType.isPlaylist("/storage/emulated/0/roms/psx/Game.iso"))
        assertFalse(
            BridgeInputType.isPlaylist(
                "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd",
            ),
        )
    }
}
