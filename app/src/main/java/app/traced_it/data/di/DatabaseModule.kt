package app.traced_it.data.di

import android.content.Context
import androidx.room.Room
import app.traced_it.data.database.AppDatabase
import app.traced_it.data.database.EntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "Entry").build()

    @Provides
    fun provideEntryDao(db: AppDatabase): EntryDao = db.entryDao()
}
