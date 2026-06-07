package net.aitorciki.dem3ux.paths

import java.net.URI

data class ResolvedGamePath(
    val absolutePath: String,
) {
    val rawPath: String = absolutePath

    val fileUri: String = URI("file", "", absolutePath, null).toASCIIString()
}
