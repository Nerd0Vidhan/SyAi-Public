package com.mato.syai.presentation.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mato.syai.presentation.bottomnavigation.BottomFabGroup
import com.mato.syai.R
import com.mato.syai.presentation.navigation.BottomNavigationGraph
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.PurpleDark

@Composable
fun HomeScreen(
    parentNavController: NavController
) {
    val bottomNavController = rememberNavController()

    Scaffold(
        topBar = {
            HomeTopBar(bottomNavController,parentNavController)
        },
        containerColor = Color.Transparent
    ) { paddingValues ->

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding()) // This handles the TopBar offset
        ) {
            // 1. The Main Content
            BottomNavigationGraph(
                navController = bottomNavController,
                paddingValues = PaddingValues(bottom = 0.dp) // Leave space for the bar
            )

            // 2. The Bottom UI (Bar + FABs + Overlay)
            // This stays on top of the NavGraph
            BottomFabGroup(navController = bottomNavController,parentNavController = parentNavController)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(navController: NavHostController,parentNavController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var expanded by remember { mutableStateOf(false) }

    val title = when (currentRoute) {
        "dashboard" -> "Dashboard"
        "mood_tracker" -> "Mood Tracker"
        "finance" -> "Finance"
        "notes" -> "Notes"
        "remainder"->  "Reminders & Alarms"
        else -> "Home"
    }

    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = R.drawable.syai),"Logo", modifier = Modifier.size(55.dp,30.dp))
                VerticalDivider(thickness = 2.dp,color= MaterialTheme.colorScheme.secondary, modifier = Modifier.size(2.dp,28.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(text = title, color = BrownLight, fontSize = 22.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(PurpleDark),
        actions = {

            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {

                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        expanded = false
                        parentNavController.navigate("settings")
                    }
                )
            }
        }
    )
}