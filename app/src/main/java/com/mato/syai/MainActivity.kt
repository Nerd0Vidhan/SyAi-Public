package com.mato.syai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.mato.syai.mood_tracker.MoodTrackerApp
import com.mato.syai.presentation.navigation.AppNavGraph
import com.mato.syai.profile.kkhh

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
//            kkhh()
            val navController = rememberNavController()
            AppNavGraph(navController = navController)
        }
    }
}