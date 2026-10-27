package app.traced_it.testing

import app.traced_it.data.di.ClockModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ClockModule::class])
object TestClockModule {
    @Provides
    fun provideClock(): Clock = TestClock.instance
}

object TestClock {
    val now = LocalDateTime(2026, 10, 31, 1, 30, 59)
    val timeZone = TimeZone.currentSystemDefault()
    val instance = object : Clock {
        override fun now() = now.toInstant(timeZone)
    }
}
