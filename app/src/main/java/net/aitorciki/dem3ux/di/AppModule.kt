package net.aitorciki.dem3ux.di

import android.app.Application
import androidx.room.Room
import net.aitorciki.dem3ux.data.Dem3uxDatabase
import net.aitorciki.dem3ux.data.PlaylistRepository
import net.aitorciki.dem3ux.data.RoomPlaylistRepository
import net.aitorciki.dem3ux.setup.EsDeSetupRepository
import net.aitorciki.dem3ux.setup.EsDeSetupRepositoryImpl
import net.aitorciki.dem3ux.ui.Dem3uxViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule =
    module {
        single {
            Room
                .databaseBuilder(
                    androidContext(),
                    Dem3uxDatabase::class.java,
                    "dem3ux.db",
                ).build()
        }
        single<PlaylistRepository> { RoomPlaylistRepository(get()) }
        single<EsDeSetupRepository> { EsDeSetupRepositoryImpl(androidContext()) }
        viewModel { Dem3uxViewModel(androidContext() as Application, get(), get()) }
    }
