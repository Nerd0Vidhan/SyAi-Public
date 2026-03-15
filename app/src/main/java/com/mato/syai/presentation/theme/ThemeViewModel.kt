package com.mato.syai.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mato.syai.ThemeState
import com.mato.syai.data.settings.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ThemeViewModel @Inject constructor(
    settings: SettingsDataStore
) : ViewModel() {

    val themeState = combine(
        settings.themeModeFlow,
        settings.dynamicColorFlow
    ) { mode, dynamic ->
        ThemeState(mode, dynamic)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ThemeState()
    )
}