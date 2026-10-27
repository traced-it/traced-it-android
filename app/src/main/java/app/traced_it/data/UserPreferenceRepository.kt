package app.traced_it.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.traced_it.data.database.ActiveEntryUnit
import app.traced_it.data.preferences.DefaultUnitPreference
import app.traced_it.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

interface UserPreferenceRepository {
    val values: Flow<UserPreferences>

    suspend fun setDefaultUnit(value: ActiveEntryUnit)
}

class DefaultUserPreferenceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferenceRepository {
    override val values: Flow<UserPreferences> =
        dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                UserPreferences(
                    defaultUnit = DefaultUnitPreference.getValue(preferences),
                )
            }

    override suspend fun setDefaultUnit(value: ActiveEntryUnit) {
        dataStore.edit { preferences ->
            DefaultUnitPreference.setValue(preferences, value)
        }
    }
}
