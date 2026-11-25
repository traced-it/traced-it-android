package app.traced_it.data.di

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import app.traced_it.data.DefaultUserPreferenceRepository
import app.traced_it.data.UserPreferenceRepository
import app.traced_it.data.database.ActiveEntryUnit
import app.traced_it.data.preferences.DefaultUnitPreference
import app.traced_it.data.preferences.UserPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
interface UserPreferenceModule {
    @Singleton
    @Binds
    fun bindUserPreferenceRepository(
        userPreferenceRepository: DefaultUserPreferenceRepository,
    ): UserPreferenceRepository
}

class FakeUserPreferenceRepository : UserPreferenceRepository {
    private val _preferences = MutableStateFlow<Preferences>(
        mutablePreferencesOf().also { preferences ->
            DefaultUnitPreference.setValue(preferences, DefaultUnitPreference.default)
        }
    )

    override val values = _preferences.map { preferences ->
        UserPreferences(
            defaultUnit = DefaultUnitPreference.getValue(preferences),
        )
    }

    override suspend fun setDefaultUnit(value: ActiveEntryUnit) {
        _preferences.value = _preferences.value.toMutablePreferences().also { preferences ->
            DefaultUnitPreference.setValue(preferences, value)
        }
    }
}
