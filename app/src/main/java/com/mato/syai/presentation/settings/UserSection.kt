package com.mato.syai.presentation.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mato.syai.profile.presentation.LoginViewModel
import com.mato.syai.ui.theme.errorLight

@Composable
fun UserSection(viewModel: LoginViewModel,context: Context) {

    SettingsSectionHeader("User")

    GlassSettingsCard {

        SettingsActionRow(
            Icons.Default.Logout,
            "Logout",
            "Sign out from your account",
            color = errorLight
        ) {
            viewModel.logout(context)
        }
    }
}