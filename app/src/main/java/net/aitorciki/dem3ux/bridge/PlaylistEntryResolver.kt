package net.aitorciki.dem3ux.bridge

import net.aitorciki.dem3ux.m3u.M3uEntry
import net.aitorciki.dem3ux.m3u.M3uParser
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object PlaylistEntryResolver {
    fun resolveEntries(
        sourcePath: String,
        content: String,
    ): List<M3uEntry> {
        val entryLines = entryLines(content)

        return when {
            sourcePath.startsWith("content://") -> {
                entryLines.mapIndexed { index, line ->
                    M3uEntry(
                        index = index,
                        rawLine = line,
                        resolvedPath = resolveContentUri(sourcePath, line),
                    )
                }
            }

            sourcePath.startsWith("file://") -> {
                entryLines.mapIndexed { index, line ->
                    M3uEntry(
                        index = index,
                        rawLine = line,
                        resolvedPath = resolveFileUri(sourcePath, line),
                    )
                }
            }

            else -> {
                M3uParser.parse(sourcePath = sourcePath, content = content).entries
            }
        }
    }

    private fun entryLines(content: String): List<String> =
        content
            .lineSequence()
            .map(String::trim)
            .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
            .toList()

    private fun resolveFileUri(
        sourcePath: String,
        entryPath: String,
    ): String {
        if (entryPath.startsWith("content://") || entryPath.startsWith("file://")) {
            return entryPath
        }

        val sourceUri = URI(sourcePath)
        val sourceFile = File(requireNotNull(sourceUri.path) { "file URI path is required" })
        val resolvedPath =
            M3uParser
                .parse(sourcePath = sourceFile.path, content = entryPath)
                .entries
                .single()
                .resolvedPath
        return URI("file", "", resolvedPath, null).toASCIIString()
    }

    private fun resolveContentUri(
        sourcePath: String,
        entryPath: String,
    ): String {
        if (entryPath.startsWith("content://") || entryPath.startsWith("file://") || entryPath.startsWith("/")) {
            return entryPath
        }

        val sourceUri = URI(sourcePath)
        if (sourceUri.authority != "com.android.externalstorage.documents") {
            return entryPath
        }

        val encodedDocumentId = sourceUri.path.substringAfterLast("/document/")
        val documentId = URLDecoder.decode(encodedDocumentId, StandardCharsets.UTF_8.name())
        val volumeSeparator = documentId.indexOf(':')
        if (volumeSeparator == -1) {
            return entryPath
        }

        val volume = documentId.substring(0, volumeSeparator)
        val documentPath = documentId.substring(volumeSeparator + 1)
        val parentPath = documentPath.substringBeforeLast('/', missingDelimiterValue = "")
        val resolvedPath = normalizePath(listOf(parentPath, entryPath).filter(String::isNotEmpty).joinToString("/"))
        val resolvedDocumentId = "$volume:$resolvedPath"
        val encodedResolvedDocumentId =
            URLEncoder
                .encode(resolvedDocumentId, StandardCharsets.UTF_8.name())
                .replace("+", "%20")

        return "${sourceUri.scheme}://${sourceUri.authority}/document/$encodedResolvedDocumentId"
    }

    private fun normalizePath(path: String): String {
        val normalizedSegments = ArrayDeque<String>()

        path.split("/").forEach { segment ->
            when {
                segment.isEmpty() || segment == "." -> {
                    return@forEach
                }

                segment == ".." && normalizedSegments.isNotEmpty() && normalizedSegments.last() != ".." -> {
                    normalizedSegments.removeLast()
                }

                segment == ".." -> {
                    normalizedSegments.addLast(segment)
                }

                else -> {
                    normalizedSegments.addLast(segment)
                }
            }
        }

        return normalizedSegments.joinToString("/")
    }
}
