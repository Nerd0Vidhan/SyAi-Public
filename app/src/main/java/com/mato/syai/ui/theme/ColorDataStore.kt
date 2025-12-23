package com.mato.syai.ui.theme

import androidx.datastore.core.DataStore
import com.mato.syai.data.datastore.ColorPreferences
import kotlinx.coroutines.flow.map

class ColorDataStore(private val dataStore: DataStore<ColorPreferences>) {

    val colorSchemeFlow = dataStore.data.map { prefs ->
        AppColorScheme(
            primary = prefs.primary,
            onPrimary = prefs.onPrimary,
            secondary = prefs.secondary,
            onSecondary = prefs.onSecondary,
            background = prefs.background,
            onBackground = prefs.onBackground,
            logoTint = prefs.logoTint,
            accent = prefs.accent
        )
    }

    suspend fun updateColorScheme(newScheme: AppColorScheme) {
        dataStore.updateData { prefs ->
            prefs.toBuilder()
                .setPrimary(newScheme.primary)
                .setOnPrimary(newScheme.onPrimary)
                .setSecondary(newScheme.secondary)
                .setOnSecondary(newScheme.onSecondary)
                .setBackground(newScheme.background)
                .setOnBackground(newScheme.onBackground)
                .setLogoTint(newScheme.logoTint)
                .setAccent(newScheme.accent)
                .build()
        }
    }
}
