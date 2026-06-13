package net.aitorciki.dem3ux.bridge

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistEntryResolverTest {
    @Test
    fun `plain filesystem input resolves entries as filesystem paths`() {
        val entries =
            PlaylistEntryResolver.resolveEntries(
                sourcePath = "/storage/emulated/0/roms/psx/Nebula Drift.m3u",
                content = "Disc 1.chd\nDisc 2.chd",
            )

        assertEquals(
            "/storage/emulated/0/roms/psx/Disc 1.chd",
            entries.first().resolvedPath,
        )
    }

    @Test
    fun `file uri input resolves entries as file uris`() {
        val entries =
            PlaylistEntryResolver.resolveEntries(
                sourcePath = "file:///storage/emulated/0/roms/psx/Nebula%20Drift.m3u",
                content = "Disc 1.chd\nDisc 2.chd",
            )

        assertEquals(
            "file:///storage/emulated/0/roms/psx/Disc%201.chd",
            entries.first().resolvedPath,
        )
    }

    @Test
    fun `external storage document uri input resolves entries as sibling document uris`() {
        val entries =
            PlaylistEntryResolver.resolveEntries(
                sourcePath =
                    "content://com.android.externalstorage.documents/document/" +
                        "primary%3Aroms%2Fpsx%2FNebula%20Drift.m3u",
                content = ".Nebula Drift/Nebula Drift (Disc 1).chd\n.Nebula Drift/Nebula Drift (Disc 2).chd",
            )

        assertEquals(
            "content://com.android.externalstorage.documents/document/" +
                "primary%3Aroms%2Fpsx%2F.Nebula%20Drift%2FNebula%20Drift%20%28Disc%201%29.chd",
            entries.first().resolvedPath,
        )
    }

    @Test
    fun `external storage document uri input normalizes relative dot segments`() {
        val entries =
            PlaylistEntryResolver.resolveEntries(
                sourcePath =
                    "content://com.android.externalstorage.documents/document/" +
                        "primary%3Aroms%2Fpsx%2Fplaylists%2FNebula%20Drift.m3u",
                content = "../Nebula Drift/Disc 1.chd",
            )

        assertEquals(
            "content://com.android.externalstorage.documents/document/" +
                "primary%3Aroms%2Fpsx%2FNebula%20Drift%2FDisc%201.chd",
            entries.first().resolvedPath,
        )
    }

    @Test
    fun `external storage tree document uri input resolves entries as sibling document uris`() {
        val entries =
            PlaylistEntryResolver.resolveEntries(
                sourcePath =
                    "content://com.android.externalstorage.documents/tree/" +
                        "primary%3ADocuments%2Froms%2Fpsx/document/" +
                        "primary%3ADocuments%2Froms%2Fpsx%2FNebula%20Drift.m3u",
                content = ".Nebula Drift/Nebula Drift (Disc 1).chd",
            )

        assertEquals(
            "content://com.android.externalstorage.documents/document/" +
                "primary%3ADocuments%2Froms%2Fpsx%2F.Nebula%20Drift%2FNebula%20Drift%20%28Disc%201%29.chd",
            entries.first().resolvedPath,
        )
    }

    @Test
    fun `ES-DE file provider uri input resolves relative entries as external storage document uris`() {
        val entries =
            PlaylistEntryResolver.resolveEntries(
                sourcePath = "content://org.es_de.frontend.files/external/Documents/roms/gb/zelda.m3u",
                content = "zelda.gb",
            )

        assertEquals(
            "content://com.android.externalstorage.documents/document/" +
                "primary%3ADocuments%2Froms%2Fgb%2Fzelda.gb",
            entries.first().resolvedPath,
        )
    }

    @Test
    fun `ES-DE file provider uri input normalizes relative dot segments`() {
        val entries =
            PlaylistEntryResolver.resolveEntries(
                sourcePath = "content://org.es_de.frontend.files/external/Documents/roms/gb/playlists/zelda.m3u",
                content = "../zelda/zelda.gb",
            )

        assertEquals(
            "content://com.android.externalstorage.documents/document/" +
                "primary%3ADocuments%2Froms%2Fgb%2Fzelda%2Fzelda.gb",
            entries.first().resolvedPath,
        )
    }
}
