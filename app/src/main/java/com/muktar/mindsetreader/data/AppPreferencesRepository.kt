package com.muktar.mindsetreader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

private const val APP_PREFERENCES_NAME = "app_preferences"

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = APP_PREFERENCES_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "library",
                keysToMigrate = setOf(
                    "last_opened_book_uri",
                    "library_sort_position"
                )
            )
        )
    }
)

class AppPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val lastOpenedBookUri: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LAST_OPENED_BOOK_URI]
        }

    val librarySortPosition: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_LIBRARY_SORT_POSITION] ?: 0
        }

    suspend fun getLastOpenedBookUri(): String? = withContext(ioDispatcher) {
        lastOpenedBookUri.first()
    }

    suspend fun getLibrarySortPosition(): Int = withContext(ioDispatcher) {
        librarySortPosition.first()
    }

    suspend fun setLastOpenedBookUri(uri: String) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_OPENED_BOOK_URI] = uri
        }
    }

    suspend fun clearLastOpenedBookUri() = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences.remove(KEY_LAST_OPENED_BOOK_URI)
        }
    }

    suspend fun setLibrarySortPosition(position: Int) = withContext(ioDispatcher) {
        dataStore.edit { preferences ->
            preferences[KEY_LIBRARY_SORT_POSITION] = position
        }
    }

    companion object {
        private val KEY_LAST_OPENED_BOOK_URI = stringPreferencesKey("last_opened_book_uri")
        private val KEY_LIBRARY_SORT_POSITION = intPreferencesKey("library_sort_position")

        @Volatile
        private var INSTANCE: AppPreferencesRepository? = null

        fun getRepository(context: Context): AppPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferencesRepository(context.applicationContext.dataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}
