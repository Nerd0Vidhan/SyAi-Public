package com.mato.syai.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mato.syai.mood_tracker.MoodTrackerApp
import com.mato.syai.notes.presentation.main.NotesScreen
import com.mato.syai.step_tracker.StepCounterScreen
import com.mato.syai.tools.ToolsScreen

@Composable
fun ToolsNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "tools") {
        composable("tools") { ToolsScreen(navController) }
//        composable("notes") { NotesScreen() }
        composable("mood") { MoodTrackerApp() }
        composable("step_counter") { StepCounterScreen() }
//        composable("digital_wellbeing") { DigitalWellbeingScreen() }
//        composable("finance") { FinanceScreen() }
    }
}
