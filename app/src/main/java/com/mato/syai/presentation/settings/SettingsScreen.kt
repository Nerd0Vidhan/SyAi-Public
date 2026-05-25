package com.mato.syai.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mato.syai.profile.presentation.LoginUiState
import com.mato.syai.profile.presentation.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var expressiveUI by remember { mutableStateOf(true) }
    var animations by remember { mutableStateOf(true) }
    var dynamicColors by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            SettingsSection("UI") {

                SettingsSwitchItem(
                    title = "Expressive UI",
                    subtitle = "Enable richer animations & visuals",
                    checked = expressiveUI,
                    onCheckedChange = { expressiveUI = it }
                )

                SettingsSwitchItem(
                    title = "Animations",
                    subtitle = "Enable motion and transitions",
                    checked = animations,
                    onCheckedChange = { animations = it }
                )
            }

            SettingsSection("Default") {

                SettingsSwitchItem(
                    title = "Dynamic Colors",
                    subtitle = "Use system Material You colors",
                    checked = dynamicColors,
                    onCheckedChange = { dynamicColors = it }
                )

                SettingsButtonItem(
                    title = "Reset Preferences",
                    subtitle = "Restore default settings"
                ) {}
            }

            SettingsSection("User") {

                SettingsButtonItem(
                    title = "Logout",
                    subtitle = "Sign out from your account",
                    textColor = MaterialTheme.colorScheme.error
                ) {
                    viewModel.logout(context)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}