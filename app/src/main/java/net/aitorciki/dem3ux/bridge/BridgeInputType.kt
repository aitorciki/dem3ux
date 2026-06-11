package net.aitorciki.dem3ux.bridge

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object BridgeInputType {
    fun isPlaylist(inputPath: String): Boolean {
        val path = inputPath.substringBefore('?').substringBefore('#').decodeUrlComponent()
        return path.endsWith(".m3u", ignoreCase = true) || path.endsWith(".m3u8", ignoreCase = true)
    }

    private fun String.decodeUrlComponent(): String =
        runCatching { URLDecoder.decode(this, StandardCharsets.UTF_8.name()) }
            .getOrDefault(this)
}
