package app.traced_it.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock

@Module
@InstallIn(SingletonComponent::class)
class ClockModule {
    @Provides
    fun provideClock(): Clock = Clock.System
}
