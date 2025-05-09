package com.mato.syai.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mato.syai.presentation.splash.SplashScreen
import com.mato.syai.presentation.*
import com.mato.syai.presentation.AIAssistant.GeminiTextGeneratorUI
import com.mato.syai.presentation.AIAssistant.GeminiTextGeneratorUIPreview
import com.mato.syai.presentation.bottomnavigation.MainScreen
import com.mato.syai.presentation.bottomnavigation.MainScreenPreview
import com.mato.syai.presentation.dashboard.Place

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                navController.navigate("home") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }

        composable("home") { MainScreenPreview() }

        composable ("dashboard") { Place() }
        composable("notes") { Place()}
        composable("settings") { Place()}
        composable ("tools",) { GeminiTextGeneratorUI()}
        composable("ai") { GeminiTextGeneratorUI() }
    }
    }
