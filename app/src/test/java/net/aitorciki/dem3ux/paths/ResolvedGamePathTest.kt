package net.aitorciki.dem3ux.paths

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolvedGamePathTest {
    @Test
    fun `raw path is the absolute path`() {
        val path =
            ResolvedGamePath(
                absolutePath = "/storage/emulated/0/roms/psx/Nebula Drift.chd",
            )

        assertEquals(
            "/storage/emulated/0/roms/psx/Nebula Drift.chd",
            path.rawPath,
        )
    }

    @Test
    fun `file URI encodes path characters safely`() {
        val path =
            ResolvedGamePath(
                absolutePath =
                    "/project/roms/psx/.Nebula Drift (Demo) (Rev 1)/" +
                        "Nebula Drift (Demo) (Disc 1) (Rev 1).chd",
            )

        assertEquals(
            "file:///project/roms/psx/.Nebula%20Drift%20(Demo)%20(Rev%201)/" +
                "Nebula%20Drift%20(Demo)%20(Disc%201)%20(Rev%201).chd",
            path.fileUri,
        )
    }
}
