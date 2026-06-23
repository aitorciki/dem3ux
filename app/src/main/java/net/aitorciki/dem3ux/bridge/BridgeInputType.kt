package net.aitorciki.dem3ux.bridge

import net.aitorciki.dem3ux.paths.PathCodec

object BridgeInputType {
    fun isPlaylist(inputPath: String): Boolean {
        val path = inputPath.substringBefore('?').substringBefore('#').let(PathCodec::decodeSafe)
        return path.endsWith(".m3u", ignoreCase = true) || path.endsWith(".m3u8", ignoreCase = true)
    }
}
