package net.aitorciki.dem3ux.di

import android.app.Application
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.aitorciki.dem3ux.bridge.AndroidPlaylistContentReader
import net.aitorciki.dem3ux.bridge.BridgeOrchestrator
import net.aitorciki.dem3ux.bridge.PersistedTreeUrisProvider
import net.aitorciki.dem3ux.bridge.PlaylistContentReader
import net.aitorciki.dem3ux.data.Dem3uxDatabase
import net.aitorciki.dem3ux.data.Dem3uxMigrations
import net.aitorciki.dem3ux.data.PlaylistRepository
import net.aitorciki.dem3ux.data.RoomPlaylistRepository
import net.aitorciki.dem3ux.setup.EsDeSetupRepository
import net.aitorciki.dem3ux.setup.EsDeSetupRepositoryImpl
import net.aitorciki.dem3ux.ui.Dem3uxViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val LOG_TAG = "dem3ux"

val appModule =
    module {
        single {
            Room
                .databaseBuilder(
                    androidContext(),
                    Dem3uxDatabase::class.java,
                    "dem3ux.db",
                ).addMigrations(*Dem3uxMigrations.ALL)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        }
        single<PlaylistRepository> { RoomPlaylistRepository(get()) }
        single<EsDeSetupRepository> { EsDeSetupRepositoryImpl(androidContext()) }
        single<CoroutineDispatcher> { Dispatchers.IO }
        single<PersistedTreeUrisProvider> {
            PersistedTreeUrisProvider {
                androidContext()
                    .contentResolver
                    .persistedUriPermissions
                    .filter { permission -> permission.isReadPermission }
                    .map { permission -> permission.uri.toString() }
            }
        }
        single<PlaylistContentReader> {
            AndroidPlaylistContentReader(
                context = androidContext(),
                persistedTreeUrisProvider = get(),
                ioDispatcher = get(),
                logger = { message, error ->
                    if (error == null) Log.w(LOG_TAG, message) else Log.w(LOG_TAG, message, error)
                },
            )
        }
        single {
            BridgeOrchestrator(
                playlistRepository = get(),
                playlistContentReader = get(),
                persistedTreeUrisProvider = get(),
                logger = { message, error ->
                    if (error == null) Log.w(LOG_TAG, message) else Log.w(LOG_TAG, message, error)
                },
            )
        }
        viewModel { Dem3uxViewModel(androidContext() as Application, get(), get(), get()) }
    }
