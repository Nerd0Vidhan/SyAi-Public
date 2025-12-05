package com.mato.syai.presentation.navigation

import CuteAnimatedWaterTank
import FinanceTrackerScreen
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.syai.MainScreenPreview
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.CutePrompts.PromptUI
import com.mato.syai.splashScreen.ui.SplashScreen
import com.mato.syai.ai_assistant.MainAssistantScreen
import com.mato.syai.mood_tracker.MoodTrackerApp
import com.mato.syai.profile.LoginScreen
import com.mato.syai.remainder.ReminderAlarmScreen
import com.mato.syai.task_management.TaskManagementScreen
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
        composable("notes") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show()  }
        composable("digital_wellbeing") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show() }
        composable("step_counter") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show() }
        composable("remainder") { ReminderAlarmScreen() }
        composable("mood_tracker") { MoodTrackerApp() }
        composable("voice_assistant") {
                val vm: VoiceAssistantViewModel = hiltViewModel()
                VoiceAssistantScreen(vm)
        }
        composable("menu") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show()  }
        composable ("tools") { ToolsScreen(navController) }
        composable ("tasks_manager") { TaskManagementScreen() }
        composable ("prompt_box") { PromptUI() }
        composable("ai") { MainAssistantScreen() }
        composable("premium") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show()  }
        composable("profile") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show()  }
        composable("water_tracker") { CuteAnimatedWaterTank() }
        composable("settings") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show()  }
        composable("speech_to_voice") { Toast.makeText(LocalContext.current, "Coming Soon", Toast.LENGTH_SHORT).show()  }

    }

}