package com.mato.syai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mato.syai.presentation.splash.SplashScreen
import com.mato.syai.presentation.*
import com.mato.syai.presentation.bottomnavigation.MainScreen
import com.mato.syai.presentation.bottomnavigation.MainScreenPreview

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
    }
}
