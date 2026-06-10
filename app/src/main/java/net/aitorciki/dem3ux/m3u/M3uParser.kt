package net.aitorciki.dem3ux.m3u

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
            sourcePath = normalizePath(sourcePath),
            entries = entries,
        )
    }

    private fun resolvePath(
        parentPath: String,
        entryPath: String,
    ): String {
        val file = File(entryPath)
        val resolvedPath = if (file.isAbsolute) entryPath else File(parentPath, entryPath).path
        return normalizePath(resolvedPath)
    }

    private fun normalizePath(path: String): String {
        val isAbsolute = path.startsWith("/")
        val normalizedSegments = ArrayDeque<String>()

        path.split("/").forEach { segment ->
            when {
                segment.isEmpty() || segment == "." -> {
                    return@forEach
                }

                segment == ".." && normalizedSegments.isNotEmpty() && normalizedSegments.last() != ".." -> {
                    normalizedSegments.removeLast()
                }

                segment == ".." && !isAbsolute -> {
                    normalizedSegments.addLast(segment)
                }

                segment != ".." -> {
                    normalizedSegments.addLast(segment)
                }
            }
        }

        val normalizedPath = normalizedSegments.joinToString("/")
        return when {
            isAbsolute && normalizedPath.isEmpty() -> "/"
            isAbsolute -> "/$normalizedPath"
            else -> normalizedPath
        }
    }
}
