package net.aitorciki.dem3ux.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePathTest {
    @Test
    fun `parses content uri`() {
        val path = GamePath.parse("content://com.android.externalstorage.documents/document/primary%3Aroms%2Fgame.chd")

        assertTrue(path is GamePath.ContentUri)
        assertEquals("content", (path as GamePath.ContentUri).uri.scheme)
        assertEquals("content://com.android.externalstorage.documents/document/primary%3Aroms%2Fgame.chd", path.raw)
    }

    @Test
    fun `parses file uri`() {
        val path = GamePath.parse("file:///storage/emulated/0/roms/psx/Game%20Disc.chd")

        assertTrue(path is GamePath.FileUri)
        assertEquals("file", (path as GamePath.FileUri).uri.scheme)
        assertEquals("/storage/emulated/0/roms/psx/Game Disc.chd", path.uri.path)
    }

    @Test
    fun `parses filesystem path as raw path`() {
        val path = GamePath.parse("/storage/emulated/0/roms/psx/Game Disc.chd")

        assertEquals(GamePath.RawPath("/storage/emulated/0/roms/psx/Game Disc.chd"), path)
    }

    @Test
    fun `malformed uri shaped input falls back to raw path`() {
        val path = GamePath.parse("content://bad uri")

        assertEquals(GamePath.RawPath("content://bad uri"), path)
    }
}
