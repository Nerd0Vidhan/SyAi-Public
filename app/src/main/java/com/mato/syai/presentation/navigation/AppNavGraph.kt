package com.mato.syai.presentation.navigation

import CuteAnimatedWaterTank
import FinanceTrackerScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.CutePrompts.PromptUI
import com.mato.syai.splashScreen.ui.SplashScreen
import com.mato.syai.ai_assistant.MainAssistantScreen
import com.mato.syai.mood_tracker.MoodTrackerApp
import com.mato.syai.notes.ui.screen.DebugNotesScreen
import com.mato.syai.notes.ui.screen.NotesListScreen
import com.mato.syai.notes.ui.screen.NotesScreen
import com.mato.syai.presentation.main.HomeScreen
import com.mato.syai.profile.LoginScreen
import com.mato.syai.presentation.settings.SettingsScreen
import com.mato.syai.presentation.settings.SettingsScreenPremium
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
        composable("home") { HomeScreen(parentNavController = navController) }
        composable("login") { LoginScreen(navController) }

        composable("settings") {
            if(/*userIsPremium()*/true){
                SettingsScreenPremium(navController)
            } else {
                SettingsScreen(navController)
            }
        }
        composable ("note_editor/{noteId}") {
            DebugNotesScreen()
        }
    }

    }


@Composable
fun BottomNavigationGraph(
    navController: NavHostController,
    parentNavController: NavController,
    paddingValues: PaddingValues
){
    NavHost(
        navController = navController,
        startDestination = "notes_list",
        modifier = Modifier.padding(paddingValues)
    ) {
        composable ("notes_editor") {
            DebugNotesScreen()
        }
        composable ("finance") { FinanceTrackerScreen() }
        composable("digital_wellbeing") {
            ComingSoonScreen("Digital Wellbeing")
        }
        composable("step_counter") {
            ComingSoonScreen("step counter")
        }
        composable("remainder") { ReminderAlarmScreen() }
        composable("mood_tracker") { MoodTrackerApp() }
        composable("voice_assistant") {
                val vm: VoiceAssistantViewModel = hiltViewModel()
                VoiceAssistantScreen(vm)
        }
        composable("menu") { }
        composable ("tools") { ToolsScreen(navController) }
        composable ("tasks_manager") { TaskManagementScreen() }
        composable ("prompt_box") { PromptUI() }
        composable("ai") { MainAssistantScreen() }
        composable("premium") {
            ComingSoonScreen("premium")
        }
        composable("profile") {
            ComingSoonScreen("profile")
        }
        composable("water_tracker") { CuteAnimatedWaterTank() }
        composable("settings") {
            ComingSoonScreen("Settings")
        }
        composable("speech_to_voice") {
            ComingSoonScreen("Speech to Voice")
        }

        composable("notes_list") {
            val notesNavController = rememberNavController()
//            NotesListScreen(
//                parentNavController = parentNavController
//            )
            NotesScreen(
                parentNavController = parentNavController
            )
        }

    }

}

@Composable
fun ComingSoonScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$title – Coming Soon")
    }
}


@Composable
fun NotesNavGraph(
    navController: NavController,
    parentNavController: NavHostController
) {
    NavHost(
        navController = parentNavController,
        startDestination = "notes_list",
    ) {

        composable("notes_list") {
            NotesListScreen(
                parentNavController = parentNavController
            )
        }

        composable ("notes_editor") {
            DebugNotesScreen()
        }

    }
}