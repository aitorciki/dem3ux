package net.aitorciki.dem3ux.bridge

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.aitorciki.dem3ux.data.PlaylistLaunchSelection
import net.aitorciki.dem3ux.data.PlaylistRepository
import net.aitorciki.dem3ux.data.PlaylistWithEntries
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Suppress("DEPRECATION")
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BridgeOrchestratorTest {
    private val targetComponent =
        requireNotNull(ComponentName.unflattenFromString("com.github.stenzek.duckstation/.EmulationActivity"))
    private val alternateComponent =
        requireNotNull(ComponentName.unflattenFromString("com.example.alternate/.GameActivity"))

    private fun newOrchestrator(
        reader: PlaylistContentReader,
        repository: PlaylistRepository,
        trees: PersistedTreeUrisProvider,
    ): BridgeOrchestrator =
        BridgeOrchestrator(
            playlistRepository = repository,
            playlistContentReader = reader,
            persistedTreeUrisProvider = trees,
            logger = { _, _ -> },
        )

    @Test
    fun `direct input launches target with selected entry equal to input path`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Disc 1.chd"
            val reader = FakePlaylistContentReader()
            val repository = FakePlaylistRepository()
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertEquals(BridgeOutcome.Launched, outcome)
            assertEquals(1, recorder.launches.size)
            assertEquals(targetComponent, recorder.launches.single().component)
            assertEquals(inputPath, recorder.launches.single().getStringExtra("bootPath"))
            assertTrue(repository.recordSeenPlaylistCalls.isEmpty())
        }

    @Test
    fun `playlist input records and launches with persisted selection`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Nebula Drift.m3u"
            val selectedEntry = "/storage/emulated/0/roms/psx/Disc 1.chd"
            val reader = FakePlaylistContentReader(content = "#EXTM3U\nDisc 1.chd\nDisc 2.chd")
            val repository =
                FakePlaylistRepository(
                    recordSeenPlaylistResult =
                        PlaylistLaunchSelection(
                            playlistId = 7,
                            selectedEntryPath = selectedEntry,
                        ),
                )
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertEquals(BridgeOutcome.Launched, outcome)
            assertEquals(1, repository.recordSeenPlaylistCalls.size)
            assertEquals(inputPath, repository.recordSeenPlaylistCalls.single().sourcePath)
            assertEquals("#EXTM3U\nDisc 1.chd\nDisc 2.chd", repository.recordSeenPlaylistCalls.single().content)
            assertEquals(selectedEntry, recorder.launches.single().getStringExtra("bootPath"))
        }

    @Test
    fun `security exception while reading playlist requests folder access`() =
        runTest {
            val inputPath = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FGame.m3u"
            val reader = FakePlaylistContentReader(securityException = SecurityException("denied"))
            val repository = FakePlaylistRepository()
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertTrue(outcome is BridgeOutcome.NeedsFolderAccess)
            assertEquals(bridgeLaunch, (outcome as BridgeOutcome.NeedsFolderAccess).bridgeLaunch)
            assertTrue(repository.recordSeenPlaylistCalls.isEmpty())
            assertTrue(recorder.launches.isEmpty())
        }

    @Test
    fun `non-permission failure while reading playlist returns failure`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Game.m3u"
            val reader = FakePlaylistContentReader(error = IllegalStateException("broken reader"))
            val repository = FakePlaylistRepository()
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertTrue(outcome is BridgeOutcome.Failed)
            assertEquals("Failed to read playlist content", (outcome as BridgeOutcome.Failed).message)
            assertTrue(outcome.error is IllegalStateException)
            assertTrue(repository.recordSeenPlaylistCalls.isEmpty())
            assertTrue(recorder.launches.isEmpty())
        }

    @Test
    fun `blank selected entry from repository finishes with failure`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Empty.m3u"
            val reader = FakePlaylistContentReader(content = "#EXTM3U")
            val repository =
                FakePlaylistRepository(
                    recordSeenPlaylistResult =
                        PlaylistLaunchSelection(
                            playlistId = 1,
                            selectedEntryPath = "",
                        ),
                )
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertTrue(outcome is BridgeOutcome.Failed)
            assertTrue(recorder.launches.isEmpty())
        }

    @Test
    fun `content selected entry without persisted tree grant requests folder access`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Game.m3u"
            val selectedEntry = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd"
            val reader = FakePlaylistContentReader(content = "#EXTM3U\nDisc 1.chd")
            val repository =
                FakePlaylistRepository(
                    recordSeenPlaylistResult =
                        PlaylistLaunchSelection(
                            playlistId = 1,
                            selectedEntryPath = selectedEntry,
                        ),
                )
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertTrue(outcome is BridgeOutcome.NeedsFolderAccess)
            assertTrue(recorder.launches.isEmpty())
        }

    @Test
    fun `content selected entry with persisted tree grant maps and launches`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Game.m3u"
            val selectedEntry = "content://com.android.externalstorage.documents/document/primary%3Aroms%2Fpsx%2FDisc%201.chd"
            val treeUri = "content://com.android.externalstorage.documents/tree/primary%3Aroms"
            val reader = FakePlaylistContentReader(content = "#EXTM3U\nDisc 1.chd")
            val repository =
                FakePlaylistRepository(
                    recordSeenPlaylistResult =
                        PlaylistLaunchSelection(
                            playlistId = 1,
                            selectedEntryPath = selectedEntry,
                        ),
                )
            val trees = FakePersistedTreeUrisProvider(listOf(treeUri))
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent =
                Intent(Intent.ACTION_VIEW)
                    .setData(inputPath.toUri())
                    .putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertEquals(BridgeOutcome.Launched, outcome)
            assertEquals(1, recorder.launches.size)
            assertTrue(
                recorder.launches
                    .single()
                    .dataString!!
                    .startsWith("content://com.android.externalstorage.documents/tree/"),
            )
        }

    @Test
    fun `activity not found for all targets returns failure`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Disc 1.chd"
            val reader = FakePlaylistContentReader()
            val repository = FakePlaylistRepository()
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder =
                RecordingTargetLauncher(
                    results =
                        listOf(
                            TargetLaunchResult.ActivityNotFound(ActivityNotFoundException("nope")),
                            TargetLaunchResult.ActivityNotFound(ActivityNotFoundException("nope-second")),
                        ),
                )
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent, alternateComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertTrue(outcome is BridgeOutcome.Failed)
            val failed = outcome as BridgeOutcome.Failed
            assertTrue(failed.error is ActivityNotFoundException)
            assertEquals(2, recorder.launches.size)
        }

    @Test
    fun `target launch error short-circuits remaining components`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Disc 1.chd"
            val reader = FakePlaylistContentReader()
            val repository = FakePlaylistRepository()
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder =
                RecordingTargetLauncher(
                    results =
                        listOf(
                            TargetLaunchResult.Error(SecurityException("denied on launch")),
                        ),
                )
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent, alternateComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            val outcome = orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertTrue(outcome is BridgeOutcome.Failed)
            val failed = outcome as BridgeOutcome.Failed
            assertTrue(failed.error is SecurityException)
            assertEquals(1, recorder.launches.size)
            assertEquals(targetComponent, recorder.launches.first().component)
        }

    @Test
    fun `string extra equal to input path is replaced with selected entry`() =
        runTest {
            val inputPath = "/storage/emulated/0/roms/psx/Disc 1.chd"
            val reader = FakePlaylistContentReader()
            val repository = FakePlaylistRepository()
            val trees = FakePersistedTreeUrisProvider(emptyList())
            val recorder = RecordingTargetLauncher()
            val orchestrator = newOrchestrator(reader, repository, trees)

            val sourceIntent = Intent(Intent.ACTION_VIEW).putExtra("bootPath", inputPath)
            val bridgeLaunch =
                BridgeLaunch(
                    inputPath = inputPath,
                    targetComponents = listOf(targetComponent),
                    targetAction = Intent.ACTION_VIEW,
                )

            orchestrator.runBridge(sourceIntent, bridgeLaunch, recorder)

            assertEquals(inputPath, recorder.launches.single().getStringExtra("bootPath"))
        }
}

private class FakePlaylistContentReader(
    val content: String? = null,
    val securityException: SecurityException? = null,
    val error: Throwable? = null,
) : PlaylistContentReader {
    override suspend fun read(inputPath: String): PlaylistContentResult =
        when {
            securityException != null -> PlaylistContentResult.NeedsPermission(securityException)
            error != null -> PlaylistContentResult.Failed(error)
            content != null -> PlaylistContentResult.Success(content)
            else -> PlaylistContentResult.Failed(IllegalStateException("No fake playlist content configured"))
        }
}

private class FakePlaylistRepository(
    val recordSeenPlaylistResult: PlaylistLaunchSelection? = null,
) : PlaylistRepository {
    val recordSeenPlaylistCalls = mutableListOf<RecordSeenPlaylistCall>()

    override fun observePlaylistsWithEntries(): Flow<List<PlaylistWithEntries>> = flowOf(emptyList())

    override suspend fun recordSeenPlaylist(
        sourcePath: String,
        content: String,
        now: Long,
    ): PlaylistLaunchSelection? {
        recordSeenPlaylistCalls.add(RecordSeenPlaylistCall(sourcePath, content))
        return recordSeenPlaylistResult
    }

    override suspend fun selectEntry(
        playlistId: Long,
        entryIndex: Int,
    ) {
    }

    override suspend fun deletePlaylist(playlistId: Long) {
    }
}

private data class RecordSeenPlaylistCall(
    val sourcePath: String,
    val content: String,
)

private class FakePersistedTreeUrisProvider(
    private val uris: List<String>,
) : PersistedTreeUrisProvider {
    override fun persistedReadableTreeUris(): List<String> = uris
}

private class RecordingTargetLauncher(
    private val results: List<TargetLaunchResult> = emptyList(),
) : TargetLauncher {
    val launches = mutableListOf<Intent>()
    private var resultIndex = 0

    override fun launch(targetIntent: Intent): TargetLaunchResult {
        launches.add(targetIntent)
        return results.getOrElse(resultIndex++) { TargetLaunchResult.Success }
    }
}
