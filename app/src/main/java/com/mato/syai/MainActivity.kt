package com.mato.syai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.mato.syai.presentation.navigation.AppNavGraph
import com.mato.syai.presentation.settings.ThemeMode
import com.mato.syai.presentation.theme.ThemeViewModel
import com.mato.syai.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()

            val themeState by themeViewModel.themeState.collectAsState()

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeState.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            AppTheme(
                darkTheme = darkTheme,
                dynamicColor = themeState.dynamicColor
            ) {
//                DebugNotesScreen()
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }

        }
    }
}

data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false
)