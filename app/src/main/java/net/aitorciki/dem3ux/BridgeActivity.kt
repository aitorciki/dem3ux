package net.aitorciki.dem3ux

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.aitorciki.dem3ux.bridge.BridgeContract
import net.aitorciki.dem3ux.bridge.BridgeTargetIntentFactory
import net.aitorciki.dem3ux.data.Dem3uxDatabaseProvider
import net.aitorciki.dem3ux.data.PlaylistRepository
import java.io.File

class BridgeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetActivity = intent.getStringExtra(BridgeContract.EXTRA_TARGET_ACTIVITY)
        val inputPath = intent.getStringExtra(BridgeContract.EXTRA_INPUT_PATH)
        val targetComponent = targetActivity?.let(ComponentName::unflattenFromString)

        if (targetComponent == null || inputPath.isNullOrBlank()) {
            finish()
            return
        }

        lifecycleScope.launch {
            val playlistContent = readPlaylistContent(inputPath)
            val selectedEntry =
                playlistContent?.let { content ->
                    PlaylistRepository(Dem3uxDatabaseProvider.get(this@BridgeActivity))
                        .recordSeenPlaylist(sourcePath = inputPath, content = content)
                        ?.selectedEntryPath
                }

            if (selectedEntry.isNullOrBlank()) {
                finish()
                return@launch
            }

            val targetIntent =
                BridgeTargetIntentFactory.build(
                    sourceIntent = intent,
                    targetComponent = targetComponent,
                    inputPath = inputPath,
                    selectedEntry = selectedEntry,
                )
            startActivity(targetIntent)
            finish()
        }
    }

    private fun readPlaylistContent(inputPath: String): String? =
        runCatching {
            when {
                inputPath.startsWith("content://") -> {
                    contentResolver.openInputStream(inputPath.toUri())?.bufferedReader()?.use { reader ->
                        reader.readText()
                    }
                }

                inputPath.startsWith("file://") -> {
                    File(requireNotNull(inputPath.toUri().path)).readText()
                }

                else -> {
                    File(inputPath).readText()
                }
            }
        }.getOrNull()
}
