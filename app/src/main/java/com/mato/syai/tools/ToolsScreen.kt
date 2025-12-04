package com.mato.syai.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mato.syai.R
import com.mato.syai.ui.theme.BrownLight

data class ToolItem(val id: Int, val route: String)

val tools = listOf(
    ToolItem(R.drawable.tasks, "tasks_manager"),
    ToolItem(R.drawable.finance, "finance"),
    ToolItem(R.drawable.remainder, "remainder"),
    ToolItem(R.drawable.mood, "mood_tracker"),
    ToolItem(R.drawable.speech_to_voice, "speech_to_voice"),
    ToolItem(R.drawable.water_tracker, "water_tracker"),
    ToolItem(R.drawable.voice_ass, "voice_assistant"),
    ToolItem(R.drawable.promts, "prompt_box"),
    ToolItem(R.drawable.notes, "notes"),
    ToolItem(R.drawable.step_counter, "step_counter"),
    ToolItem(R.drawable.digital_wellbeing, "digital_wellbeing"),


)


@Composable
fun ToolsScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tools) { tool ->

                Card(
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                bounded = true, // or false for unbounded ripple
                                radius = 200.dp, // custom ripple radius
                                color = Color.Black // custom ripple color
                            ),
                        ) {
//                            if ( tool.route=="step_counter"){
//
//                            }
                            // Avoid duplicate pushes by checking current route
                            val currentRoute = navController.currentDestination?.route

                            if (currentRoute != tool.route) {
                                navController.navigate(tool.route)
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(BrownLight)
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {

                        Image(
                            painter = painterResource(id = tool.id),
                            contentDescription = tool.route,
                            contentScale = ContentScale.FillBounds
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = tool.route,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
