package net.aitorciki.dem3ux.ui

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.aitorciki.dem3ux.bridge.PlaylistContentReader
import net.aitorciki.dem3ux.bridge.PlaylistContentResult
import net.aitorciki.dem3ux.bridge.PresetBridge
import net.aitorciki.dem3ux.data.PlaylistEntity
import net.aitorciki.dem3ux.data.PlaylistEntryEntity
import net.aitorciki.dem3ux.data.PlaylistLaunchSelection
import net.aitorciki.dem3ux.data.PlaylistRepository
import net.aitorciki.dem3ux.data.PlaylistWithEntries
import net.aitorciki.dem3ux.setup.EsDeSetupRepository
import net.aitorciki.dem3ux.setup.InstalledFrontend
import net.aitorciki.dem3ux.setup.InstalledPresetTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Dem3uxViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakePlaylistRepository: FakePlaylistRepository
    private lateinit var fakeEsDeSetupRepository: FakeEsDeSetupRepository
    private lateinit var fakePlaylistContentReader: FakePlaylistContentReader
    private lateinit var viewModel: Dem3uxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakePlaylistRepository = FakePlaylistRepository()
        fakeEsDeSetupRepository = FakeEsDeSetupRepository()
        fakePlaylistContentReader = FakePlaylistContentReader()
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel =
            Dem3uxViewModel(
                application,
                fakePlaylistRepository,
                fakeEsDeSetupRepository,
                fakePlaylistContentReader,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits playlists from repository`() =
        runTest(testDispatcher) {
            fakePlaylistRepository.playlists.value =
                listOf(
                    playlistWithEntries(id = 1, sourcePath = "content://test/playlist.m3u"),
                )

            val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.playlistsLoaded)
            assertEquals(1, state.playlists.size)

            collectJob.cancel()
        }

    @Test
    fun `selectPlaylist updates selectedPlaylist in uiState`() =
        runTest(testDispatcher) {
            fakePlaylistRepository.playlists.value =
                listOf(
                    playlistWithEntries(id = 1, sourcePath = "content://test/alpha.m3u"),
                    playlistWithEntries(id = 2, sourcePath = "content://test/beta.m3u"),
                )

            val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.selectPlaylist(2)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNotNull(state.selectedPlaylist)
            assertEquals(2L, state.selectedPlaylist?.id)

            collectJob.cancel()
        }

    @Test
    fun `clearSelectedPlaylist clears selection in uiState`() =
        runTest(testDispatcher) {
            fakePlaylistRepository.playlists.value =
                listOf(
                    playlistWithEntries(id = 1, sourcePath = "content://test/alpha.m3u"),
                )

            val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            viewModel.selectPlaylist(1)
            advanceUntilIdle()

            viewModel.clearSelectedPlaylist()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.selectedPlaylist)

            collectJob.cancel()
        }

    @Test
    fun `deletePlaylist delegates to repository`() =
        runTest(testDispatcher) {
            viewModel.deletePlaylist(42)
            advanceUntilIdle()
            assertTrue(fakePlaylistRepository.deletedPlaylistIds.contains(42L))
        }

    @Test
    fun `selectEntry delegates to repository`() =
        runTest(testDispatcher) {
            viewModel.selectEntry(playlistId = 7, entryIndex = 3)
            advanceUntilIdle()
            assertEquals(1, fakePlaylistRepository.selectEntryCalls.size)
            assertEquals(7L, fakePlaylistRepository.selectEntryCalls[0].playlistId)
            assertEquals(3, fakePlaylistRepository.selectEntryCalls[0].entryIndex)
        }

    @Test
    fun `importPlaylist success records playlist and updates selection`() =
        runTest(testDispatcher) {
            fakePlaylistRepository.playlists.value =
                listOf(
                    playlistWithEntries(id = 7, sourcePath = "content://test/playlist.m3u"),
                )
            val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            fakePlaylistContentReader.content = "game1.chd\ngame2.chd\n"
            fakePlaylistRepository.recordSeenPlaylistResult =
                PlaylistLaunchSelection(
                    playlistId = 7L,
                    selectedEntryPath = "content://test/game1.chd",
                )

            viewModel.importPlaylist(Uri.parse("content://test/playlist.m3u"))
            advanceUntilIdle()

            assertEquals(1, fakePlaylistRepository.recordSeenPlaylistCalls.size)
            assertEquals("content://test/playlist.m3u", fakePlaylistRepository.recordSeenPlaylistCalls[0].sourcePath)
            assertEquals("game1.chd\ngame2.chd\n", fakePlaylistRepository.recordSeenPlaylistCalls[0].content)
            assertEquals(
                7L,
                viewModel.uiState.value.selectedPlaylist
                    ?.id,
            )
            assertEquals("Playlist added.", viewModel.uiState.value.importMessage)

            collectJob.cancel()
        }

    @Test
    fun `importPlaylist security exception sets error message`() =
        runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            fakePlaylistContentReader.securityException = SecurityException("denied")

            viewModel.importPlaylist(Uri.parse("content://test/playlist.m3u"))
            advanceUntilIdle()

            assertTrue(fakePlaylistRepository.recordSeenPlaylistCalls.isEmpty())
            assertEquals("Could not import playlist.", viewModel.uiState.value.importMessage)

            collectJob.cancel()
        }

    @Test
    fun `importPlaylist null content sets error message`() =
        runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            fakePlaylistContentReader.content = null

            viewModel.importPlaylist(Uri.parse("content://test/playlist.m3u"))
            advanceUntilIdle()

            assertTrue(fakePlaylistRepository.recordSeenPlaylistCalls.isEmpty())
            assertEquals("Could not import playlist.", viewModel.uiState.value.importMessage)

            collectJob.cancel()
        }

    private fun playlistWithEntries(
        id: Long,
        sourcePath: String,
    ): PlaylistWithEntries =
        PlaylistWithEntries(
            playlist =
                PlaylistEntity(
                    id = id,
                    sourcePath = sourcePath,
                    displayName = sourcePath.substringAfterLast('/').substringBeforeLast('.'),
                    selectedEntryIndex = 0,
                    firstSeenAt = 1000L,
                    lastSeenAt = 1000L,
                    lastParsedAt = 1000L,
                ),
            entries =
                listOf(
                    PlaylistEntryEntity(
                        playlistId = id,
                        entryIndex = 0,
                        rawLine = "game1.chd",
                        resolvedPath = "content://test/game1.chd",
                        displayName = "game1",
                    ),
                ),
        )
}

private class FakePlaylistRepository : PlaylistRepository {
    val playlists = MutableStateFlow<List<PlaylistWithEntries>>(emptyList())
    val deletedPlaylistIds = mutableListOf<Long>()
    val selectEntryCalls = mutableListOf<SelectEntryCall>()
    val recordSeenPlaylistCalls = mutableListOf<RecordSeenPlaylistCall>()
    var recordSeenPlaylistResult: PlaylistLaunchSelection? = null

    override fun observePlaylistsWithEntries(): Flow<List<PlaylistWithEntries>> = playlists

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
        selectEntryCalls.add(SelectEntryCall(playlistId, entryIndex))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        deletedPlaylistIds.add(playlistId)
    }
}

private data class RecordSeenPlaylistCall(
    val sourcePath: String,
    val content: String,
)

private class FakePlaylistContentReader : PlaylistContentReader {
    var content: String? = null
    var securityException: SecurityException? = null

    override suspend fun read(inputPath: String): PlaylistContentResult =
        PlaylistContentResult(content = content, securityException = securityException)
}

private data class SelectEntryCall(
    val playlistId: Long,
    val entryIndex: Int,
)

private class FakeEsDeSetupRepository : EsDeSetupRepository {
    override fun persistCustomSystemsFolder(
        uri: Uri,
        grantFlags: Int,
    ) {
    }

    override fun persistedCustomSystemsFolder(): Uri? = null

    override fun readFindRules(treeUri: Uri): String? = null

    override fun saveFindRules(
        treeUri: Uri,
        content: String,
    ) {
    }

    override fun installedPresetTarget(preset: PresetBridge): InstalledPresetTarget? = null

    override fun installedFrontend(): InstalledFrontend? = null
}
