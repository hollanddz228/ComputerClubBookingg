package com.example.computerclubbooking.uii.tutorial

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("tutorial_prefs")

object AppPreferences {

    private val KEY_TUTORIAL_SHOWN = booleanPreferencesKey("tutorial_shown")

    fun isTutorialShown(context: Context) =
        context.dataStore.data.map { prefs ->
            prefs[KEY_TUTORIAL_SHOWN] ?: false
        }

    suspend fun setTutorialShown(context: Context, value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TUTORIAL_SHOWN] = value
        }
    }
}
