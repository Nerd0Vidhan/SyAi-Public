package com.mato.syai.presentation.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mato.syai.R
import com.mato.syai.presentation.bottomnavigation.CustomBottomNavigation
import com.mato.syai.presentation.navigation.BottomNavigationGraph
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.PurpleDark

//@Preview(showBackground = true, showSystemUi = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(navController: NavHostController) {
    TopAppBar(
        title = {
            Row {
                Image(painter = painterResource(id = R.drawable.syai),"Logo", modifier = Modifier.size(55.dp,30.dp))
                Text(" | ", color = BrownLight, fontSize = 30.sp)
                //            ToolbarAnimation(text = "Dashboard")
                Text("DashBoard", color = BrownLight, fontSize = 30.sp)

            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PurpleDark),
        actions = {
            IconButton(onClick = { navController.navigate("edit") }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrownLight)
            }
        }
    )
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
//            MainScreen()
            CustomBottomNavigation(navController)
            Spacer(modifier = Modifier.height(40.dp))
        }
    ) { paddingValues ->
        BottomNavigationGraph(navController = navController, paddingValues = paddingValues)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesTopBar() {
    TopAppBar(
        title = {
            Row {
                Image(painter = painterResource(id = R.drawable.syai),"Logo", modifier = Modifier.size(55.dp,30.dp))
                Text(" | ", color = BrownLight, fontSize = 30.sp)
                //            ToolbarAnimation(text = "Dashboard")
                Text("Notes", color = BrownLight, fontSize = 30.sp)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PurpleDark),
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
                Image(painter = painterResource(id = R.drawable.syai),"Logo", modifier = Modifier.size(55.dp,30.dp))
                Text(" | ", color = BrownLight, fontSize = 30.sp)
                //            ToolbarAnimation(text = "Dashboard")
                Text("Assistant", color = BrownLight, fontSize = 30.sp)

            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PurpleDark),
        actions = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsTopBar() {
    TopAppBar(
        title = {
            Row {
                Image(painter = painterResource(id = R.drawable.syai),"Logo", modifier = Modifier.size(55.dp,30.dp))
                Text(" | ", color = BrownLight, fontSize = 30.sp)
                //            ToolbarAnimation(text = "Dashboard")
                Text("Tools", color = BrownLight, fontSize = 30.sp)

            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = PurpleDark),
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