package net.aitorciki.dem3ux.ui

import net.aitorciki.dem3ux.paths.PathCodec

internal object SourcePathLabel {
    fun format(sourcePath: String): String {
        val documentPath = sourcePath.externalStorageDocumentPath()
        if (documentPath != null) {
            return documentPath.toTailLabel(alwaysPrefixEllipsis = true)
        }

        val decodedPath = sourcePath.substringBefore('?').substringBefore('#').let(PathCodec::decodeSafe)
        return decodedPath.toTailLabel(alwaysPrefixEllipsis = sourcePath.contains('/'))
    }

    private fun String.externalStorageDocumentPath(): String? {
        val marker = "content://com.android.externalstorage.documents/document/"
        if (!startsWith(marker)) {
            return null
        }

        val documentId = removePrefix(marker).substringBefore('?').substringBefore('#').let(PathCodec::decodeSafe)
        return documentId.substringAfter(':', missingDelimiterValue = documentId)
    }

    private fun String.toTailLabel(alwaysPrefixEllipsis: Boolean): String {
        val segments = split('/').filter { segment -> segment.isNotBlank() }
        if (segments.isEmpty()) {
            return this
        }

        val tail = segments.takeLast(3).joinToString("/")
        val shouldPrefix = alwaysPrefixEllipsis || segments.size > 3
        return if (shouldPrefix) ".../$tail" else tail
    }
}
