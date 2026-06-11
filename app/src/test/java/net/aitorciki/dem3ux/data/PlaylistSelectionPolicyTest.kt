package net.aitorciki.dem3ux.data

import net.aitorciki.dem3ux.m3u.M3uEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistSelectionPolicyTest {
    @Test
    fun `uses saved index when it still exists`() {
        assertEquals(1, PlaylistSelectionPolicy.selectedIndex(1, entries))
    }

    @Test
    fun `falls back to first entry when saved index no longer exists`() {
        assertEquals(0, PlaylistSelectionPolicy.selectedIndex(9, entries))
    }

    @Test
    fun `falls back to first entry when there is no saved index`() {
        assertEquals(0, PlaylistSelectionPolicy.selectedIndex(null, entries))
    }

    @Test
    fun `returns null when playlist has no entries`() {
        assertNull(PlaylistSelectionPolicy.selectedIndex(1, emptyList()))
    }

    private val entries =
        listOf(
            M3uEntry(index = 0, rawLine = "disc1.chd", resolvedPath = "/roms/disc1.chd"),
            M3uEntry(index = 1, rawLine = "disc2.chd", resolvedPath = "/roms/disc2.chd"),
        )
}
