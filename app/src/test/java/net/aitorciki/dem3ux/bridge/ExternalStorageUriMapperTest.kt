package net.aitorciki.dem3ux.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalStorageUriMapperTest {
    @Test
    fun `maps ES-DE tree document URI through exact persisted tree grant`() {
        val uri =
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3ADocuments%2Froms%2Fpsx/document/" +
                "primary%3ADocuments%2Froms%2Fpsx%2FNebula%20Drift.m3u"

        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString = uri,
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/" +
                            "primary%3ADocuments%2Froms%2Fpsx",
                    ),
            )

        assertEquals(uri, mappedUri)
    }

    @Test
    fun `maps ES-DE tree document URI through parent persisted tree grant`() {
        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString =
                    "content://com.android.externalstorage.documents/tree/" +
                        "primary%3ADocuments%2Froms%2Fpsx/document/" +
                        "primary%3ADocuments%2Froms%2Fpsx%2FNebula%20Drift.m3u",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Froms",
                    ),
            )

        assertEquals(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3ADocuments%2Froms/document/" +
                "primary%3ADocuments%2Froms%2Fpsx%2FNebula%20Drift.m3u",
            mappedUri,
        )
    }

    @Test
    fun `maps plain document URI through persisted tree grant`() {
        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString =
                    "content://com.android.externalstorage.documents/document/" +
                        "primary%3ADocuments%2Froms%2Fpsx%2FDisc%201.chd",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/" +
                            "primary%3ADocuments%2Froms%2Fpsx",
                    ),
            )

        assertEquals(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3ADocuments%2Froms%2Fpsx/document/" +
                "primary%3ADocuments%2Froms%2Fpsx%2FDisc%201.chd",
            mappedUri,
        )
    }

    @Test
    fun `ignores unrelated persisted tree grant`() {
        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString =
                    "content://com.android.externalstorage.documents/document/" +
                        "primary%3ADocuments%2Froms%2Fpsx%2FDisc%201.chd",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fother",
                    ),
            )

        assertNull(mappedUri)
    }

    @Test
    fun `uses most specific persisted tree grant`() {
        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString =
                    "content://com.android.externalstorage.documents/document/" +
                        "primary%3ADocuments%2Froms%2Fpsx%2FDisc%201.chd",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Froms",
                        "content://com.android.externalstorage.documents/tree/" +
                            "primary%3ADocuments%2Froms%2Fpsx",
                    ),
            )

        assertEquals(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3ADocuments%2Froms%2Fpsx/document/" +
                "primary%3ADocuments%2Froms%2Fpsx%2FDisc%201.chd",
            mappedUri,
        )
    }

    @Test
    fun `detects matching persisted tree grant`() {
        val hasGrant =
            ExternalStorageUriMapper.hasPersistedTreeGrant(
                uriString =
                    "content://com.android.externalstorage.documents/tree/" +
                        "primary%3ADocuments%2Froms%2Fpsx/document/" +
                        "primary%3ADocuments%2Froms%2Fpsx%2FDisc%201.chd",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Froms",
                    ),
            )

        assertEquals(true, hasGrant)
    }

    @Test
    fun `detects missing persisted tree grant`() {
        val hasGrant =
            ExternalStorageUriMapper.hasPersistedTreeGrant(
                uriString =
                    "content://com.android.externalstorage.documents/tree/" +
                        "primary%3ADocuments%2Froms%2Fpsx/document/" +
                        "primary%3ADocuments%2Froms%2Fpsx%2FDisc%201.chd",
                persistedTreeUris = emptyList(),
            )

        assertEquals(false, hasGrant)
    }

    @Test
    fun `maps primary raw external storage path through persisted tree grant`() {
        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString = "/storage/emulated/0/Documents/roms/amstradcpc/the pawn.m3u",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Froms",
                    ),
            )

        assertEquals(
            "content://com.android.externalstorage.documents/tree/" +
                "primary%3ADocuments%2Froms/document/" +
                "primary%3ADocuments%2Froms%2Famstradcpc%2Fthe%20pawn.m3u",
            mappedUri,
        )
    }

    @Test
    fun `maps removable raw external storage path through persisted tree grant`() {
        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString = "/storage/F9EA-FDD4/ROMs/psx/Game.m3u",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/F9EA-FDD4%3AROMs",
                    ),
            )

        assertEquals(
            "content://com.android.externalstorage.documents/tree/" +
                "F9EA-FDD4%3AROMs/document/" +
                "F9EA-FDD4%3AROMs%2Fpsx%2FGame.m3u",
            mappedUri,
        )
    }

    @Test
    fun `ignores unrelated persisted tree grant for raw external storage path`() {
        val mappedUri =
            ExternalStorageUriMapper.mapToPersistedTreeUri(
                uriString = "/storage/emulated/0/Documents/roms/amstradcpc/the pawn.m3u",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Fother",
                    ),
            )

        assertNull(mappedUri)
    }

    @Test
    fun `detects matching persisted tree grant for raw external storage path`() {
        val hasGrant =
            ExternalStorageUriMapper.hasPersistedTreeGrant(
                uriString = "/storage/emulated/0/Documents/roms/amstradcpc/the pawn.m3u",
                persistedTreeUris =
                    listOf(
                        "content://com.android.externalstorage.documents/tree/primary%3ADocuments%2Froms",
                    ),
            )

        assertEquals(true, hasGrant)
    }
}
