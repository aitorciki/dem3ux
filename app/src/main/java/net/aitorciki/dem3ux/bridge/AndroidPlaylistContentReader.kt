package net.aitorciki.dem3ux.bridge

import android.content.Context
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class AndroidPlaylistContentReader(
    private val context: Context,
    private val persistedTreeUrisProvider: PersistedTreeUrisProvider,
    private val ioDispatcher: CoroutineDispatcher,
    private val logger: (String, Throwable?) -> Unit,
) : PlaylistContentReader {
    override suspend fun read(inputPath: String): PlaylistContentResult =
        withContext(ioDispatcher) {
            val result =
                runCatching {
                    when {
                        inputPath.startsWith("content://") -> {
                            val readableInputPath = inputPath.mapThroughPersistedTreeGrant()
                            context.contentResolver.openInputStream(readableInputPath.toUri())?.bufferedReader()?.use { reader ->
                                reader.readText()
                            }
                        }

                        inputPath.startsWith("file://") -> {
                            File(requireNotNull(inputPath.toUri().path)).readText()
                        }

                        else -> {
                            val readableInputPath = inputPath.mapThroughPersistedTreeGrant()
                            if (readableInputPath != inputPath) {
                                context.contentResolver.openInputStream(readableInputPath.toUri())?.bufferedReader()?.use { reader ->
                                    reader.readText()
                                }
                            } else {
                                File(inputPath).readText()
                            }
                        }
                    }
                }

            result.fold(
                onSuccess = { content -> PlaylistContentResult(content = content) },
                onFailure = { error ->
                    logger("Failed to read playlist content", error)
                    PlaylistContentResult(securityException = error.asPermissionException())
                },
            )
        }

    private fun String.mapThroughPersistedTreeGrant(): String =
        ExternalStorageUriMapper.mapToPersistedTreeUri(
            uriString = this,
            persistedTreeUris = persistedTreeUrisProvider.persistedReadableTreeUris(),
        ) ?: this

    private fun Throwable.asPermissionException(): SecurityException? =
        when {
            this is SecurityException -> this
            this is FileNotFoundException && message?.contains("EACCES") == true -> SecurityException(message, this)
            else -> null
        }
}
