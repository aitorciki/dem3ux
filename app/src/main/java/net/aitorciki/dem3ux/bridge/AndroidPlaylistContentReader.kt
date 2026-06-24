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
                    when (val gamePath = GamePath.parse(inputPath)) {
                        is GamePath.ContentUri -> {
                            val readableInputPath = gamePath.raw.mapThroughPersistedTreeGrant()
                            context.contentResolver.openInputStream(readableInputPath.toUri())?.bufferedReader()?.use { reader ->
                                reader.readText()
                            }
                        }

                        is GamePath.FileUri -> {
                            File(requireNotNull(gamePath.uri.path)).readText()
                        }

                        is GamePath.RawPath -> {
                            val readableInputPath = gamePath.raw.mapThroughPersistedTreeGrant()
                            if (readableInputPath != gamePath.raw) {
                                context.contentResolver.openInputStream(readableInputPath.toUri())?.bufferedReader()?.use { reader ->
                                    reader.readText()
                                }
                            } else {
                                File(gamePath.raw).readText()
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
