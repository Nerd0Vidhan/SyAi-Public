package com.mato.syai.trialarea

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mato.syai.mood_tracker.MoodTrackerApp

class MoodTrackerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
//            SyaiTheme { // Apply your app's theme
                // Call your MoodTrackerApp Composable here
                MoodTrackerApp() // Assuming MoodTrackerApp is a Composable
//            }
        }
    }
}