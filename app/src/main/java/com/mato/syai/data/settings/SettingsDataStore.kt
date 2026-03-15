package com.mato.syai.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.mato.syai.presentation.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsDataStore(private val context: Context) {

    private val Context.dataStore by preferencesDataStore("settings")

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val themeModeFlow: Flow<ThemeMode> =
        context.dataStore.data.map {
            ThemeMode.valueOf(
                it[THEME_MODE] ?: ThemeMode.SYSTEM.name
            )
        }

    val dynamicColorFlow: Flow<Boolean> =
        context.dataStore.data.map {
            it[DYNAMIC_COLOR] ?: true
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit {
            it[THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit {
            it[DYNAMIC_COLOR] = enabled
        }
    }
}