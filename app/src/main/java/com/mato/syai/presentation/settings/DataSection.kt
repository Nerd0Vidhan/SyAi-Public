package com.mato.syai.presentation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Upload
import androidx.compose.runtime.Composable

@Composable
fun DataSection() {

    SettingsSectionHeader("Data")

    GlassSettingsCard {

        SettingsActionRow(
            Icons.Default.Upload,
            "Export Notes",
            "Export notes to file"
        ) {}

        SettingsActionRow(
            Icons.Default.Backup,
            "Backup",
            "Backup notes to cloud"
        ) {}
    }
}