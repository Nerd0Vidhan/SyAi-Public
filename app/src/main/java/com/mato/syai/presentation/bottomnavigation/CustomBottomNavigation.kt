package com.mato.syai.presentation.bottomnavigation

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.mato.syai.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mato.syai.presentation.toolbar.Toolbar


@Composable
fun CustomBottomNavigation(navController: NavHostController) {
//    Toolbar(navController)
    Row(
        modifier = Modifier
            .height(80.dp)
            .paint(
                painter = painterResource(R.drawable.bottom_navigation),
                contentScale = ContentScale.FillHeight
            )
            .padding(horizontal = 40.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dashboard
        IconButton(onClick = { navController.navigate("dashboard") }) {
            Icon(
                imageVector = Icons.Default.Home, // replace with desired Dashboard icon
                contentDescription = "Dashboard",
                tint = Color.White
            )
        }
        // Notes
        IconButton(onClick = { navController.navigate("notes") }) {
            Icon(
                imageVector = Icons.Default.Note, // replace with Notes icon
                contentDescription = "Notes",
                tint = Color.White
            )
        }
        // Settings
        IconButton(onClick = {navController.navigate("menu") }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }
        // Tools
        IconButton(onClick = { navController.navigate("tools")}) {
            Icon(
                imageVector = Icons.Default.Build, // replace with Tools icon
                contentDescription = "Tools",
                tint = Color.White
            )
        }
        // AI Assistant
        IconButton(onClick = { navController.navigate("ai") }) {
            Icon(
                imageVector = Icons.Default.SmartToy, // replace with AI Assistant icon
                contentDescription = "AI Assistant",
                tint = Color.White
            )
        }
    }
}
