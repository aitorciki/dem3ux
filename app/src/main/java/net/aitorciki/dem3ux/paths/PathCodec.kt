package net.aitorciki.dem3ux.paths

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object PathCodec {
    private val UTF_8 = StandardCharsets.UTF_8.name()

    fun encode(value: String): String =
        URLEncoder
            .encode(value, UTF_8)
            .replace("+", "%20")

    fun decodeStrict(value: String): String = URLDecoder.decode(value, UTF_8)

    fun decodeSafe(value: String): String = runCatching { decodeStrict(value) }.getOrDefault(value)

    fun normalizePath(path: String): String {
        val isAbsolute = path.startsWith("/")
        val normalizedSegments = ArrayDeque<String>()

        path.split("/").forEach { segment ->
            when {
                segment.isEmpty() || segment == "." -> {
                    return@forEach
                }

                segment == ".." && normalizedSegments.isNotEmpty() && normalizedSegments.last() != ".." -> {
                    normalizedSegments.removeLast()
                }

                segment == ".." && !isAbsolute -> {
                    normalizedSegments.addLast(segment)
                }

                segment != ".." -> {
                    normalizedSegments.addLast(segment)
                }
            }
        }

        val normalizedPath = normalizedSegments.joinToString("/")
        return when {
            isAbsolute && normalizedPath.isEmpty() -> "/"
            isAbsolute -> "/$normalizedPath"
            else -> normalizedPath
        }
    }
}
