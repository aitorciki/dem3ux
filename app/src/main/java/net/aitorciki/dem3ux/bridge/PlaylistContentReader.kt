package net.aitorciki.dem3ux.bridge

data class PlaylistContentResult(
    val content: String? = null,
    val securityException: SecurityException? = null,
)

interface PlaylistContentReader {
    suspend fun read(inputPath: String): PlaylistContentResult
}
