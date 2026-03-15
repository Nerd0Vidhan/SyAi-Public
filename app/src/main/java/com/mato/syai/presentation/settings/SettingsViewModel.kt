package com.mato.syai.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mato.syai.data.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {

    val darkMode = settings.themeModeFlow
    val dynamicColor = settings.dynamicColorFlow

    fun toggleDarkMode(mode: ThemeMode) {
        viewModelScope.launch {
            settings.setThemeMode(mode)
        }
    }

    fun toggleDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settings.setDynamicColor(enabled)
        }
    }
}