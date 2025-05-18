package com.mato.syai.presentation.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mato.syai.presentation.splash.SplashScreen
import com.mato.syai.ai_assistant.MainAssistantScreen
import com.mato.syai.presentation.bottomnavigation.MainScreenPreview
import com.mato.syai.dashboard.DashboardScreen
import com.mato.syai.step_tracker.StepCounterPreview

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

@Composable
fun BottomNavigationGraph(navController: NavHostController, paddingValues: PaddingValues){
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.padding(paddingValues)
    ) {

        composable ("dashboard") { DashboardScreen() }
        composable("notes") { DashboardScreen() }
        composable("settings") { DashboardScreen() }
        composable ("tools",) { StepCounterPreview() }
        composable("ai") { MainAssistantScreen() }
//        composable("edit") { SwappableCardsGridPreview() }

    }

}
