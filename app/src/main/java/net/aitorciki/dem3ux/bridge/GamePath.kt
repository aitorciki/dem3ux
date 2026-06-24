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
