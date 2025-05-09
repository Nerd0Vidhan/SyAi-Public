package com.mato.syai.presentation.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mato.syai.presentation.bottomnavigation.MainScreenPreview
import com.mato.syai.presentation.dashboard.Place
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.PurpleDark

//@Preview(showBackground = true, showSystemUi = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(navController: NavHostController) {
    TopAppBar(
        title = {
            Row {
                Text("SyAi", color = PurpleDark)
                Text(" | ", color = PurpleDark)
                //            ToolbarAnimation(text = "Dashboard")
                Text("DashBoard", color = BrownLight)
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate("edit") }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = PurpleDark)
            }
        })
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Toolbar(navController: NavHostController = rememberNavController()){
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            when (currentRoute) {
                "dashboard" -> DashboardTopBar(navController)
                "notes" -> NotesTopBar()
                "settings" -> SettingsTopBar()
                "tools" -> ToolsTopBar()
                "ai" -> AITopBar()
            }
        },
        bottomBar = {
            MainScreenPreview()
//            CustomBottomNavigation(navController)
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") { Place() }
            composable("notes") { Place() }
            composable("settings") { Place() }
            composable("tools") { Place() }
            composable("ai") { Place() }
//            composable("edit") { SwappableCardsGridPreview() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopBar() {
    TopAppBar(
        title = {
            Row {
                Text("SyAi", color = PurpleDark)
                Text(" | ", color = PurpleDark)
                ToolbarAnimation(text = "Notes")
//                Text("Tools", color = BrownLight)
            }
        },
        actions = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar() {
    TopAppBar(
        title = {
            Row {
                Text("SyAi", color = PurpleDark)
                Text(" | ", color = PurpleDark)
                ToolbarAnimation(text = "Settings")
//                Text("Tools", color = BrownLight)
            }
        },
        actions = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITopBar() {
    TopAppBar(
        title = {
            Row {
                Text("SyAi", color = PurpleDark)
                Text(" | ", color = PurpleDark)
                ToolbarAnimation(text = "AI Assistant")
//                Text("Tools", color = BrownLight)
            }
        },
        actions = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsTopBar() {
    TopAppBar(
        title = {
            Row {
                Text("SyAi", color = PurpleDark)
                Text(" | ", color = PurpleDark)
                ToolbarAnimation(text = "Tools")
//                Text("Tools", color = BrownLight)
            }
        },
        actions = {})
}


@Composable
fun ToolbarAnimation(text: String){
    AnimatedVisibility(
        visible = true, // Control this with state if needed
        enter = slideInVertically(initialOffsetY = { -it }),
        exit = slideOutVertically(targetOffsetY = { it })){
        Text(text, color = BrownLight)
    }
}