package com.mato.syai.presentation.navigation

import FinanceTrackerScreen
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.splashScreen.ui.SplashScreen
import com.mato.syai.ai_assistant.MainAssistantScreen
import com.mato.syai.mood_tracker.MoodTrackerApp
import com.mato.syai.profile.LoginScreen
import com.mato.syai.tools.ToolsScreen
import com.mato.syai.R
import com.mato.syai.presentation.bottomnavigation.MainScreen

// MAIN APP ROUTES
sealed class Screen(val route: String) {

    object Splash : Screen("splash")
    object Login : Screen("login")
    object Home : Screen("home")

    object Dashboard : Screen("dashboard")
    object Notes : Screen("notes")
    object Tools : Screen("tools")
    object AI : Screen("ai")
    object Settings : Screen("settings")

    // NESTED: Tools graph parent
    object ToolsRoot : Screen("tools_root")

    // Tools nested screens
    object NotesTool : Screen("notes_tool")
    object StepCounter : Screen("step_counter")
    object DigitalWellbeing : Screen("digital_wellbeing")
    object Finance : Screen("finance")
    object TaskManager : Screen("task_management")
    object Remainder : Screen("remainder")
    object SpeechToVoice : Screen("speech_to_voice")
    object Prompts : Screen("prompts")
    object MoodTracker : Screen("mood_tracker")
    object WaterTracker : Screen("water_tracker")
    object VoiceAssistant : Screen("voice_assistant")
}



@Composable
fun AppNavGraph(navController: NavHostController) {

    val currentUser = FirebaseAuth.getInstance().currentUser

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(onSplashFinished = {
                if (currentUser != null) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }

            })
        }

        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Home.route) { MainScreen() }
    }

}


@Composable
fun HomeScreen() {
    val bottomNavController = rememberNavController()
    MainScreen()

//    Scaffold(
//        bottomBar = { BottomNavBar(bottomNavController) }
//    ) { padding ->
//        BottomNavigationGraph(
//            navController = bottomNavController,
//            paddingValues = padding
//        )
//    }
}


@Composable
fun BottomNavigationGraph(
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = Modifier.padding(paddingValues)
    ) {

        composable(Screen.Dashboard.route) { FinanceTrackerScreen() }
        composable(Screen.Notes.route) { MoodTrackerApp() }
//        composable(Screen.Menu.route) { Text("Menu") }
        composable(Screen.Tools.route) { ToolsScreen(navController) }
        composable(Screen.AI.route) { MainAssistantScreen() }
//        composable(Screen.Premium.route) { Text("Premium") }
//        composable(Screen.Profile.route) { Text("Profile") }
        composable(Screen.Settings.route) { Text("Settings") }

        // Tools pages
        composable(Screen.StepCounter.route) { Text("Step Counter") }
        composable(Screen.DigitalWellbeing.route) { Text("Digital Wellbeing") }
        composable(Screen.Finance.route) { FinanceTrackerScreen() }
        composable(Screen.Remainder.route) { Text("Remainder") }
        composable(Screen.SpeechToVoice.route) { Text("Speech To Voice") }
    }
}


@Composable
fun BottomNavBar(navController: NavHostController) {

    val items = listOf(
        Screen.Dashboard,
        Screen.Notes,
        Screen.Tools,
        Screen.AI,
        Screen.Settings
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            NavigationBarItem(
                selected = currentDestination.isTopLevelDestination(screen),
                onClick = {
                    navController.navigate(screen.route) {
                        // Avoid multiple copies of same destination
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = getIcon(screen),
                        contentDescription = screen.route
                    )
                },
                label = { Text(screen.route) }
            )
        }
    }
}

private fun NavDestination?.isTopLevelDestination(screen: Screen): Boolean {
    return this?.hierarchy?.any { it.route == screen.route } == true
}

private fun getIcon(screen: Screen): ImageVector {
    return when (screen) {
        Screen.Dashboard -> Icons.Default.Home
        Screen.Notes -> Icons.Default.Note
        Screen.Tools -> Icons.Default.Build
        Screen.AI -> Icons.Default.SmartToy
        Screen.Settings -> Icons.Default.Settings
        else -> Icons.Default.Info
    }
}

