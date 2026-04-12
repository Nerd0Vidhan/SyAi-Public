package com.mato.syai.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.runtime.Composable

@Composable
fun AboutSection() {

    SettingsSectionHeader("About")

    GlassSettingsCard {

        SettingsNavigationRow(
            Icons.Default.Info,
            "Version",
            "1.0.0"
        ) {}

        SettingsNavigationRow(
            Icons.Default.Description,
            "Licenses",
            "Open source licenses"
        ) {}

        SettingsNavigationRow(
            Icons.Default.PrivacyTip,
            "Privacy Policy",
            "View privacy policy"
        ) {}
    }
}