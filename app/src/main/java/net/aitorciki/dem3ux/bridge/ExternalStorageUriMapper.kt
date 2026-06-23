package net.aitorciki.dem3ux.bridge

import net.aitorciki.dem3ux.paths.PathCodec
import java.net.URI

object ExternalStorageUriMapper {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun mapToPersistedTreeUri(
        uriString: String,
        persistedTreeUris: List<String>,
    ): String? {
        val documentId = documentId(uriString) ?: rawExternalStorageDocumentId(uriString) ?: return null
        val matchingTree =
            matchingTreeUri(
                documentId = documentId,
                persistedTreeUris = persistedTreeUris,
            )
                ?: return null

        return buildTreeDocumentUri(
            scheme = matchingTree.scheme,
            authority = matchingTree.authority,
            treeDocumentId = matchingTree.treeDocumentId,
            documentId = documentId,
        )
    }

    fun hasPersistedTreeGrant(
        uriString: String,
        persistedTreeUris: List<String>,
    ): Boolean {
        val documentId = documentId(uriString) ?: rawExternalStorageDocumentId(uriString) ?: return false
        return matchingTreeUri(
            documentId = documentId,
            persistedTreeUris = persistedTreeUris,
        ) != null
    }

    internal fun documentId(uriString: String): String? {
        val uri = runCatching { URI(uriString) }.getOrNull() ?: return null
        if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) {
            return null
        }

        val rawPath = uri.rawPath ?: return null
        val rawDocumentId = rawPath.substringAfterLast("/document/", missingDelimiterValue = "")
        if (rawDocumentId.isEmpty()) {
            return null
        }

        return PathCodec.decodeStrict(rawDocumentId)
    }

    internal fun rawExternalStorageDocumentId(path: String): String? {
        val normalizedPath = path.substringBefore('?').substringBefore('#')
        return when {
            normalizedPath.startsWith("/storage/emulated/0/") -> {
                "primary:${normalizedPath.removePrefix("/storage/emulated/0/")}"
            }

            normalizedPath.startsWith("/storage/") -> {
                val relativePath = normalizedPath.removePrefix("/storage/")
                val volume = relativePath.substringBefore('/', missingDelimiterValue = "")
                val pathInVolume = relativePath.substringAfter('/', missingDelimiterValue = "")
                if (volume.isBlank() || pathInVolume.isBlank() || volume == "emulated") {
                    null
                } else {
                    "$volume:$pathInVolume"
                }
            }

            else -> {
                null
            }
        }
    }

    private fun treeUri(uriString: String): TreeUri? {
        val uri = runCatching { URI(uriString) }.getOrNull() ?: return null
        if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) {
            return null
        }

        val rawPath = uri.rawPath ?: return null
        val rawTreeDocumentId =
            rawPath
                .substringAfter("/tree/", missingDelimiterValue = "")
                .substringBefore("/")
        if (rawTreeDocumentId.isEmpty()) {
            return null
        }

        return TreeUri(
            scheme = uri.scheme,
            authority = uri.authority,
            treeDocumentId = PathCodec.decodeStrict(rawTreeDocumentId),
        )
    }

    private fun matchingTreeUri(
        documentId: String,
        persistedTreeUris: List<String>,
    ): TreeUri? =
        persistedTreeUris
            .mapNotNull { treeUri -> treeUri(uriString = treeUri) }
            .filter { treeUri -> documentId.isDescendantOf(treeUri.treeDocumentId) }
            .maxByOrNull { treeUri -> treeUri.treeDocumentId.length }

    private fun buildTreeDocumentUri(
        scheme: String,
        authority: String,
        treeDocumentId: String,
        documentId: String,
    ): String = "$scheme://$authority/tree/${PathCodec.encode(treeDocumentId)}/document/${PathCodec.encode(documentId)}"

    private fun String.isDescendantOf(treeDocumentId: String): Boolean {
        val documentParts = splitDocumentId() ?: return false
        val treeParts = treeDocumentId.splitDocumentId() ?: return false
        if (documentParts.volume != treeParts.volume) {
            return false
        }

        return treeParts.path.isEmpty() ||
            documentParts.path == treeParts.path ||
            documentParts.path.startsWith("${treeParts.path}/")
    }

    private fun String.splitDocumentId(): DocumentId? {
        val volumeSeparator = indexOf(':')
        if (volumeSeparator == -1) {
            return null
        }

        return DocumentId(
            volume = substring(0, volumeSeparator),
            path = substring(volumeSeparator + 1),
        )
    }

    private data class TreeUri(
        val scheme: String,
        val authority: String,
        val treeDocumentId: String,
    )

    private data class DocumentId(
        val volume: String,
        val path: String,
    )
}
