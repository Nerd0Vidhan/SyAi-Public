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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.CutePrompts.PromptUI
import com.mato.syai.splashScreen.ui.SplashScreen
import com.mato.syai.ai_assistant.MainAssistantScreen
import com.mato.syai.mood_tracker.MoodTrackerApp
import com.mato.syai.note.ui.editor.NoteEditorScreen
import com.mato.syai.note.ui.editor.PagePreviewScreen
import com.mato.syai.note.ui.home.NotesHomeScreen
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
        navigation(startDestination = "note_editor/{noteId}", route = "note_editor_flow/{noteId}") {
            composable("note_editor/{noteId}") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("note_editor_flow/{noteId}")
                }
                val noteId = parentEntry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                val viewModel: com.mato.syai.note.ui.editor.NoteEditorViewModel = hiltViewModel(parentEntry)
                
                NoteEditorScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    parentNavController = navController
                )
            }
            composable("page_preview/{noteId}") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("note_editor_flow/{noteId}")
                }
                val noteId = parentEntry.arguments?.getString("noteId")?.toLongOrNull() ?: 0L
                val viewModel: com.mato.syai.note.ui.editor.NoteEditorViewModel = hiltViewModel(parentEntry)
                
                PagePreviewScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                    navController = navController,
                    viewModel = viewModel
                )
            }
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
            NotesHomeScreen(
                onNoteClick = { noteId ->
                    parentNavController.navigate("note_editor_flow/$noteId")
                }
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