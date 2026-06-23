package net.aitorciki.dem3ux.m3u

import net.aitorciki.dem3ux.paths.PathCodec
import java.io.File

data class M3uPlaylist(
    val sourcePath: String,
    val entries: List<M3uEntry>,
)

data class M3uEntry(
    val index: Int,
    val rawLine: String,
    val resolvedPath: String,
)

object M3uParser {
    fun parse(
        sourcePath: String,
        content: String,
    ): M3uPlaylist {
        val playlistFile = File(sourcePath)
        val parentPath = playlistFile.parent.orEmpty()

        val entries =
            content
                .lineSequence()
                .map(String::trim)
                .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
                .mapIndexed { index, line ->
                    M3uEntry(
                        index = index,
                        rawLine = line,
                        resolvedPath = resolvePath(parentPath, line),
                    )
                }.toList()

        return M3uPlaylist(
            sourcePath = PathCodec.normalizePath(sourcePath),
            entries = entries,
        )
    }

    private fun resolvePath(
        parentPath: String,
        entryPath: String,
    ): String {
        val file = File(entryPath)
        val resolvedPath = if (file.isAbsolute) entryPath else File(parentPath, entryPath).path
        return PathCodec.normalizePath(resolvedPath)
    }
}
