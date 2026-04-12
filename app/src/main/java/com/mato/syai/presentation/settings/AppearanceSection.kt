package com.mato.syai.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AppearanceSection(
    viewModel: SettingsViewModel = hiltViewModel()
) {

    var darkMode by remember { mutableStateOf(false) }
    var dynamicColors by remember { mutableStateOf(true) }

    SettingsSectionHeader("Appearance")

    GlassSettingsCard {

        SettingsSwitchRow(
            Icons.Default.DarkMode,
            "Dark Mode",
            "Enable dark theme",
            darkMode
        ) {
            darkMode = it
            viewModel.toggleDarkMode(
                if (it) ThemeMode.DARK else ThemeMode.LIGHT
            )
        }

        SettingsSwitchRow(
            Icons.Default.Palette,
            "Dynamic Colors",
            "Use Material You colors",
            dynamicColors
        ) {
            dynamicColors = it
            viewModel.toggleDynamicColor(it)
        }
    }
}