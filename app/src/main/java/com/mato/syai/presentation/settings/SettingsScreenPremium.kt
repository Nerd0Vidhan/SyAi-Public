package com.mato.syai.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.mato.syai.profile.presentation.LoginUiState
import com.mato.syai.profile.presentation.LoginViewModel
import com.mato.syai.utils.animatedBackground.DeepSpaceWaveBackground

@Composable
fun SettingsScreenPremium(
    navController: NavHostController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    DeepSpaceWaveBackground {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {

            SettingsTopBar(navController)

            Column(
                modifier = Modifier
                    .verticalScroll(scroll)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                AppearanceSection()

                Spacer(Modifier.height(18.dp))

                AISection()

                Spacer(Modifier.height(18.dp))

                DataSection()

                Spacer(Modifier.height(18.dp))

                AboutSection()

                Spacer(Modifier.height(18.dp))

                UserSection(viewModel,context)

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}