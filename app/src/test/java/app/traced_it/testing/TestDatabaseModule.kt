package app.traced_it.testing

import android.content.Context
import androidx.room.Room
import app.traced_it.data.database.AppDatabase
import app.traced_it.data.database.EntryDao
import app.traced_it.data.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @Provides
    fun provideEntryDao(db: AppDatabase): EntryDao = db.entryDao()
}
