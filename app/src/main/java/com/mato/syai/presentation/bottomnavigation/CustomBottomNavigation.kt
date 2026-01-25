package com.mato.syai.presentation.bottomnavigation

import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarms
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@Composable
fun CustomBottomNavigation(navController: NavController) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(
                BottomBarCutoutShape()
            )
            .background(Color(0xFF5B4EA1)) // IMPORTANT
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ---- LEFT SIDE (2 icons) ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate("dashboard") }) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Dashboard",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("mood_tracker") }) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Mood Tracker",
                        tint = Color.White
                    )
                }
            }

            // ---- SPACE FOR FAB CUTOUT ----
            Spacer(modifier = Modifier.weight(1f))

            // ---- RIGHT SIDE (2 icons) ----
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate("tools") }) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Tools",
                        tint = Color.White
                    )
                }

                IconButton(onClick = { navController.navigate("remainder") }) {
                    Icon(
                        imageVector = Icons.Default.AccessAlarms,
                        contentDescription = "Alarm",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
