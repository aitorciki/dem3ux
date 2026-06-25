package net.aitorciki.dem3ux.bridge

import java.net.URI

internal sealed interface GamePath {
    val raw: String

    data class ContentUri(
        override val raw: String,
        val uri: URI,
    ) : GamePath

    data class FileUri(
        override val raw: String,
        val uri: URI,
    ) : GamePath

    data class RawPath(
        override val raw: String,
    ) : GamePath

    companion object {
        fun parse(raw: String): GamePath {
            val uri = runCatching { URI(raw) }.getOrNull()
            return when (uri?.scheme) {
                "content" -> ContentUri(raw = raw, uri = uri)
                "file" -> FileUri(raw = raw, uri = uri)
                else -> RawPath(raw = raw)
            }
        }
    }
}

@JvmInline
value class BridgeInputPath(
    val raw: String,
) {
    fun isPlaylist(): Boolean = BridgeInputType.isPlaylist(raw)
}

@JvmInline
value class SelectedEntryPath(
    val raw: String,
) {
    val isContentUri: Boolean
        get() = GamePath.parse(raw) is GamePath.ContentUri

    fun requiresFolderAccessForForwarding(persistedTreeUris: List<String>): Boolean =
        ExternalStorageUriMapper.documentId(raw) != null &&
            !ExternalStorageUriMapper.hasPersistedTreeGrant(
                uriString = raw,
                persistedTreeUris = persistedTreeUris,
            )

    fun mapContentUriThroughPersistedTreeGrant(persistedTreeUris: List<String>): SelectedEntryPath =
        if (ExternalStorageUriMapper.documentId(raw) != null) {
            SelectedEntryPath(
                ExternalStorageUriMapper.mapToPersistedTreeUri(
                    uriString = raw,
                    persistedTreeUris = persistedTreeUris,
                ) ?: raw,
            )
        } else {
            this
        }
}
