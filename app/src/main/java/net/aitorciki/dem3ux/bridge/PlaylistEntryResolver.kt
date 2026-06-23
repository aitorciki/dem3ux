package net.aitorciki.dem3ux.bridge

import net.aitorciki.dem3ux.m3u.M3uEntry
import net.aitorciki.dem3ux.m3u.M3uParser
import net.aitorciki.dem3ux.paths.PathCodec
import java.io.File
import java.net.URI

object PlaylistEntryResolver {
    private const val ES_DE_FILE_PROVIDER_AUTHORITY = "org.es_de.frontend.files"
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

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
        if (sourceUri.authority == ES_DE_FILE_PROVIDER_AUTHORITY) {
            return resolveEsDeFileProviderUri(sourceUri, entryPath) ?: entryPath
        }
        if (sourceUri.authority != EXTERNAL_STORAGE_AUTHORITY) {
            return entryPath
        }

        val encodedDocumentId = sourceUri.path.substringAfterLast("/document/")
        val documentId = PathCodec.decodeStrict(encodedDocumentId)
        val volumeSeparator = documentId.indexOf(':')
        if (volumeSeparator == -1) {
            return entryPath
        }

        val volume = documentId.substring(0, volumeSeparator)
        val documentPath = documentId.substring(volumeSeparator + 1)
        val parentPath = documentPath.substringBeforeLast('/', missingDelimiterValue = "")
        val resolvedPath = PathCodec.normalizePath(listOf(parentPath, entryPath).filter(String::isNotEmpty).joinToString("/"))
        val resolvedDocumentId = "$volume:$resolvedPath"
        val encodedResolvedDocumentId = PathCodec.encode(resolvedDocumentId)

        return "${sourceUri.scheme}://${sourceUri.authority}/document/$encodedResolvedDocumentId"
    }

    private fun resolveEsDeFileProviderUri(
        sourceUri: URI,
        entryPath: String,
    ): String? {
        val providerPath = sourceUri.rawPath?.let(PathCodec::decodeStrict) ?: return null
        val externalPath = providerPath.removePrefix("/external/")
        if (externalPath == providerPath) {
            return null
        }

        val parentPath = externalPath.substringBeforeLast('/', missingDelimiterValue = "")
        val resolvedPath = PathCodec.normalizePath(listOf(parentPath, entryPath).filter(String::isNotEmpty).joinToString("/"))
        val resolvedDocumentId = "primary:$resolvedPath"

        return "content://$EXTERNAL_STORAGE_AUTHORITY/document/${PathCodec.encode(resolvedDocumentId)}"
    }
}
