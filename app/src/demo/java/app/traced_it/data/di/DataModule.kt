package app.traced_it.data.di

import app.traced_it.data.EntryRepository
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
    fun provideEntryRepository(): EntryRepository =
        FakeEntryRepository(demoEntries)
}
