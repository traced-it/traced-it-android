package app.traced_it.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import app.traced_it.data.database.ActiveEntryUnit
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.units

object DefaultUnitPreference {
    val default = ClothingSizeUnit
    val key = stringPreferencesKey("default_unit")

    fun getValue(preferences: Preferences): ActiveEntryUnit =
        preferences[key]?.let { id ->
            units
                .filterIsInstance<ActiveEntryUnit>()
                .firstOrNull { it.id == id }
        } ?: default

    fun setValue(preferences: MutablePreferences, value: ActiveEntryUnit) {
        preferences[key] = value.id
    }
}

data class UserPreferences(
    val defaultUnit: ActiveEntryUnit,
)
