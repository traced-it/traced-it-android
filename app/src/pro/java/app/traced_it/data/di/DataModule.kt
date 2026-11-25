package app.traced_it.data.di

import app.traced_it.data.DefaultEntryRepository
import app.traced_it.data.EntryRepository
import app.traced_it.data.database.EntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
class DataModule {
    @Provides
    @Singleton
    fun provideEntryRepository(clock: Clock, entryDao: EntryDao): EntryRepository =
        DefaultEntryRepository(clock, entryDao)
}
