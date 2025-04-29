//package com.mato.syai.presentation.navigation
//
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.Scaffold
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.paint
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.currentBackStackEntryAsState
//import androidx.navigation.compose.rememberNavController
//import com.mato.syai.R
//import com.mato.syai.presentation.AIAssistant.GeminiTextGeneratorUI
//import com.mato.syai.presentation.dashboard.Place
//
//sealed class BottomNavItem(
//    val route: String,
//    val icon: ImageVector,
//    val label: String
//) {
//    object Dashboard : BottomNavItem("dashboard", Icons.Default.Home, "Dashboard")
//    object Notes : BottomNavItem("notes", Icons.Default.AddCircle, "Notes")
//    object Settings : BottomNavItem("settings", Icons.Default.Settings, "Settings")
//    object Tools : BottomNavItem("tools", Icons.Default.Build, "Tools")
//    object AIAssistant : BottomNavItem("ai", Icons.Default.Add, "AI")
//}
//
//@Composable
//fun CustomBottomNavigation(
//    navController: NavController,
//    currentRoute: String?
//) {
//    val navItems = listOf(
//        BottomNavItem.Dashboard,
//        BottomNavItem.Notes,
//        BottomNavItem.Settings,
//        BottomNavItem.Tools,
//        BottomNavItem.AIAssistant
//    )
//
//    Row(
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically,
//        modifier = Modifier
//            .height(80.dp)
//            .paint(
//                painter = painterResource(R.drawable.bottom_navigation),
//                contentScale = ContentScale.FillHeight
//            )
//            .padding(horizontal = 40.dp)
//    ) {
//        navItems.forEach { item ->
//            IconButton(onClick = {
//                if (currentRoute != item.route) {
//                    navController.navigate(item.route) {
//                        popUpTo(navController.graph.startDestinationId) { saveState = true }
//                        launchSingleTop = true
//                        restoreState = true
//                    }
//                }
//            }) {
//                Icon(
//                    imageVector = item.icon,
//                    contentDescription = item.label,
//                    tint = if (currentRoute == item.route) Color.Yellow else Color.Black
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun MainScreen() {
//    val navController = rememberNavController()
//    val navBackStackEntry by navController.currentBackStackEntryAsState()
//    val currentRoute = navBackStackEntry?.destination?.route
//
//    Scaffold(
//        bottomBar = {
//            CustomBottomNavigation(navController, currentRoute)
//        }
//    ) { innerPadding ->
//        NavHost(
//            navController = navController,
//            startDestination = BottomNavItem.Dashboard.route,
//            modifier = Modifier.padding(innerPadding)
//        ) {
//            composable(BottomNavItem.Dashboard.route) { Place() }
//            composable(BottomNavItem.Notes.route) { Place() }
//            composable(BottomNavItem.Settings.route) { GeminiTextGeneratorUI() }
//            composable(BottomNavItem.Tools.route) { Place() }
//            composable(BottomNavItem.AIAssistant.route) { GeminiTextGeneratorUI() }
//        }
//    }
//}
//
//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun CustomBottomNavigationPreview() {
//    MainScreen()
//}
