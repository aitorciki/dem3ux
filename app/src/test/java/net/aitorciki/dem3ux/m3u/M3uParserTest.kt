package net.aitorciki.dem3ux.m3u

import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {
    private val nebulaDriftDirectory = "/project/roms/psx/.Nebula Drift (Demo) (Rev 1)"

    @Test
    fun `parses relative entries from fixture`() {
        val playlist =
            M3uParser.parse(
                sourcePath = "/project/roms/psx/Nebula Drift (Demo) (Rev 1).m3u",
                content = readFixture("/m3u/nebula-drift.m3u"),
            )

        assertEquals(
            M3uPlaylist(
                sourcePath = "/project/roms/psx/Nebula Drift (Demo) (Rev 1).m3u",
                entries =
                    listOf(
                        M3uEntry(
                            index = 0,
                            rawLine = ".Nebula Drift (Demo) (Rev 1)/Nebula Drift (Demo) (Disc 1) (Rev 1).chd",
                            resolvedPath = "$nebulaDriftDirectory/Nebula Drift (Demo) (Disc 1) (Rev 1).chd",
                        ),
                        M3uEntry(
                            index = 1,
                            rawLine = ".Nebula Drift (Demo) (Rev 1)/Nebula Drift (Demo) (Disc 2) (Rev 1).chd",
                            resolvedPath = "$nebulaDriftDirectory/Nebula Drift (Demo) (Disc 2) (Rev 1).chd",
                        ),
                    ),
            ),
            playlist,
        )
    }

    @Test
    fun `ignores blank lines and comments`() {
        val playlist =
            M3uParser.parse(
                sourcePath = "/roms/psx/game.m3u",
                content =
                    """
                    #EXTM3U

                    disc1.chd
                      # comment with leading whitespace
                    disc2.chd

                    """.trimIndent(),
            )

        assertEquals(
            listOf(
                M3uEntry(index = 0, rawLine = "disc1.chd", resolvedPath = "/roms/psx/disc1.chd"),
                M3uEntry(index = 1, rawLine = "disc2.chd", resolvedPath = "/roms/psx/disc2.chd"),
            ),
            playlist.entries,
        )
    }

    @Test
    fun `keeps absolute entries unchanged`() {
        val playlist =
            M3uParser.parse(
                sourcePath = "/roms/psx/game.m3u",
                content = "/storage/emulated/0/roms/psx/disc1.chd",
            )

        assertEquals(
            M3uEntry(
                index = 0,
                rawLine = "/storage/emulated/0/roms/psx/disc1.chd",
                resolvedPath = "/storage/emulated/0/roms/psx/disc1.chd",
            ),
            playlist.entries.single(),
        )
    }

    @Test
    fun `normalizes relative dot segments`() {
        val playlist =
            M3uParser.parse(
                sourcePath = "/roms/psx/playlists/game.m3u",
                content = "./../Game/disc1.chd",
            )

        assertEquals(
            "/roms/psx/Game/disc1.chd",
            playlist.entries.single().resolvedPath,
        )
    }

    private fun readFixture(path: String): String = requireNotNull(javaClass.getResource(path)).readText()
}
