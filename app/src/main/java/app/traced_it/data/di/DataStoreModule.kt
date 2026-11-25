package app.traced_it.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
class DataStoreModule {
    @Provides
    fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> =
        DataStore
            .Builder(
                storage = FileStorage(PreferencesFileSerializer) {
                    appContext.preferencesDataStoreFile("user_preferences.preferences_pb").absoluteFile
                },
                context = Dispatchers.IO + SupervisorJob(),
            )
            .setCorruptionHandler(ReplaceFileCorruptionHandler { emptyPreferences() })
            .build()
}
