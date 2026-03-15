package com.mato.syai.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Memory
import androidx.compose.runtime.Composable

@Composable
fun AISection() {

    SettingsSectionHeader("AI")

    GlassSettingsCard {

        SettingsNavigationRow(
            Icons.Default.Memory,
            "Offline Model",
            "Run AI locally on device"
        ) {}

        SettingsNavigationRow(
            Icons.Default.Cloud,
            "Cloud Model",
            "Use remote AI model"
        ) {}
    }
}