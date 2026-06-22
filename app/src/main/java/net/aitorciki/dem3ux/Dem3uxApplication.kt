package net.aitorciki.dem3ux

import android.app.Application
import net.aitorciki.dem3ux.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class Dem3uxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@Dem3uxApplication)
            modules(appModule)
        }
    }
}
