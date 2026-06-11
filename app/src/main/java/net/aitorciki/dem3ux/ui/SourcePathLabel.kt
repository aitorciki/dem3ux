package net.aitorciki.dem3ux.ui

import java.net.URLDecoder

internal object SourcePathLabel {
    fun format(sourcePath: String): String {
        val documentPath = sourcePath.externalStorageDocumentPath()
        if (documentPath != null) {
            return documentPath.toTailLabel(alwaysPrefixEllipsis = true)
        }

        val decodedPath = sourcePath.substringBefore('?').substringBefore('#').decodeUrlComponent()
        return decodedPath.toTailLabel(alwaysPrefixEllipsis = sourcePath.contains('/'))
    }

    private fun String.externalStorageDocumentPath(): String? {
        val marker = "content://com.android.externalstorage.documents/document/"
        if (!startsWith(marker)) {
            return null
        }

        val documentId = removePrefix(marker).substringBefore('?').substringBefore('#').decodeUrlComponent()
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

    private fun String.decodeUrlComponent(): String = URLDecoder.decode(this, Charsets.UTF_8.name())
}
