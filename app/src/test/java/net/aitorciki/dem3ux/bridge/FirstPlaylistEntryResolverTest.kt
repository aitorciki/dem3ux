package net.aitorciki.dem3ux.bridge

import org.junit.Assert.assertEquals
import org.junit.Test

class FirstPlaylistEntryResolverTest {
    @Test
    fun `plain filesystem input resolves first entry as filesystem path`() {
        val selectedEntry =
            FirstPlaylistEntryResolver.resolve(
                sourcePath = "/storage/emulated/0/roms/psx/Nebula Drift.m3u",
                content = "Disc 1.chd\nDisc 2.chd",
            )

        assertEquals(
            "/storage/emulated/0/roms/psx/Disc 1.chd",
            selectedEntry,
        )
    }

    @Test
    fun `file uri input resolves first entry as file uri`() {
        val selectedEntry =
            FirstPlaylistEntryResolver.resolve(
                sourcePath = "file:///storage/emulated/0/roms/psx/Nebula%20Drift.m3u",
                content = "Disc 1.chd\nDisc 2.chd",
            )

        assertEquals(
            "file:///storage/emulated/0/roms/psx/Disc%201.chd",
            selectedEntry,
        )
    }

    @Test
    fun `external storage document uri input resolves first entry as sibling document uri`() {
        val selectedEntry =
            FirstPlaylistEntryResolver.resolve(
                sourcePath =
                    "content://com.android.externalstorage.documents/document/" +
                        "primary%3Aroms%2Fpsx%2FNebula%20Drift.m3u",
                content = ".Nebula Drift/Nebula Drift (Disc 1).chd\n.Nebula Drift/Nebula Drift (Disc 2).chd",
            )

        assertEquals(
            "content://com.android.externalstorage.documents/document/" +
                "primary%3Aroms%2Fpsx%2F.Nebula%20Drift%2FNebula%20Drift%20%28Disc%201%29.chd",
            selectedEntry,
        )
    }

    @Test
    fun `external storage document uri input normalizes relative dot segments`() {
        val selectedEntry =
            FirstPlaylistEntryResolver.resolve(
                sourcePath =
                    "content://com.android.externalstorage.documents/document/" +
                        "primary%3Aroms%2Fpsx%2Fplaylists%2FNebula%20Drift.m3u",
                content = "../Nebula Drift/Disc 1.chd",
            )

        assertEquals(
            "content://com.android.externalstorage.documents/document/" +
                "primary%3Aroms%2Fpsx%2FNebula%20Drift%2FDisc%201.chd",
            selectedEntry,
        )
    }
}
