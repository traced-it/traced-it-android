package app.traced_it.data.di

import app.traced_it.data.DefaultEntryRepository
import app.traced_it.data.EntryRepository
import app.traced_it.data.local.database.EntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Singleton
    @Provides
    fun provideEntryRepository(entryDao: EntryDao): EntryRepository =
        DefaultEntryRepository(entryDao)
}
