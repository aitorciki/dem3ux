package net.aitorciki.dem3ux.bridge

sealed interface PlaylistContentResult {
    data class Success(
        val content: String,
    ) : PlaylistContentResult

    data class NeedsPermission(
        val error: SecurityException,
    ) : PlaylistContentResult

    data class Failed(
        val error: Throwable,
    ) : PlaylistContentResult
}

interface PlaylistContentReader {
    suspend fun read(inputPath: String): PlaylistContentResult
}
