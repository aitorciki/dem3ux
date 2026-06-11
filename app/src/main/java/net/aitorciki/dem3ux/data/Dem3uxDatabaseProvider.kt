package net.aitorciki.dem3ux.data

import android.content.Context
import androidx.room.Room

object Dem3uxDatabaseProvider {
    @Volatile
    private var instance: Dem3uxDatabase? = null

    fun get(context: Context): Dem3uxDatabase =
        instance ?: synchronized(this) {
            instance ?: Room
                .databaseBuilder(
                    context.applicationContext,
                    Dem3uxDatabase::class.java,
                    "dem3ux.db",
                ).build()
                .also { database -> instance = database }
        }
}
