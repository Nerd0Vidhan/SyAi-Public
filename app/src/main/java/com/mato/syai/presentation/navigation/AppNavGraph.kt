package com.mato.syai.presentation.navigation

import FinanceTrackerScreen
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.syai.MainScreenPreview
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.splashScreen.ui.SplashScreen
import com.mato.syai.ai_assistant.MainAssistantScreen
import com.mato.syai.mood_tracker.MoodTrackerApp
import com.mato.syai.presentation.bottomnavigation.CustomBottomNavigation
import com.mato.syai.profile.LoginScreen
import com.mato.syai.profile.presentation.PhoneLoginScreen
import com.mato.syai.remainder.ReminderAlarmScreen
import com.mato.syai.tools.ToolsScreen
import com.mato.syai.voiceAssistant.VoiceAssistantScreen
import com.mato.syai.voiceAssistant.VoiceAssistantViewModel

@Composable
fun AppNavGraph(navController: NavHostController) {

    val currentUser = FirebaseAuth.getInstance().currentUser
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onSplashFinished = {
                if (currentUser != null) {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                } else{
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }

            })
        }
        composable("home") { MainScreenPreview() }
        composable("login") { LoginScreen(navController) }
        composable("phone_login") {
            PhoneLoginScreen(navController)
        }
    }

    }


@Composable
fun BottomNavigationGraph(navController: NavHostController, paddingValues: PaddingValues){
    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = Modifier.padding(paddingValues)
    ) {

        composable ("dashboard") { FinanceTrackerScreen() }
        composable ("finance") { FinanceTrackerScreen() }
        composable("notes") { MoodTrackerApp() }
        composable("digital_wellbeing") { MoodTrackerApp() }
        composable("step_counter") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show() }
        composable("remainder") { ReminderAlarmScreen() }
        composable("speech_to_voice") {
                val vm: VoiceAssistantViewModel = hiltViewModel()
                VoiceAssistantScreen(vm)
        }
        composable("menu") {  }
        composable ("tools",) { ToolsScreen(navController) }
        composable("ai") { MainAssistantScreen() }
        composable("premium") {  }
        composable("profile") { }
        composable("settings") {  }

    }

}