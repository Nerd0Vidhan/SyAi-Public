package com.mato.syai.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mato.syai.R
import com.mato.syai.presentation.navigation.Screen
import com.mato.syai.ui.theme.BrownLight

data class ToolItem(val id: Int, val screen: Screen)

val tools = listOf(
    ToolItem(R.drawable.notes, Screen.Notes),
    ToolItem(R.drawable.step_counter, Screen.StepCounter),
    ToolItem(R.drawable.digital_wellbeing, Screen.DigitalWellbeing),
    ToolItem(R.drawable.finance, Screen.Finance),
    ToolItem(R.drawable.notes, Screen.Tools),
    ToolItem(R.drawable.remainder, Screen.Remainder),
    ToolItem(R.drawable.speech_to_voice, Screen.SpeechToVoice),
//    ToolItem(R.drawable.speech_to_voice, Screen.Premium),
//    ToolItem(R.drawable.speech_to_voice, Screen.Menu),
//    ToolItem(R.drawable.speech_to_voice, Screen.Menu),
//    ToolItem(R.drawable.speech_to_voice, Screen.Menu)
)

@Composable
fun ToolsScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(tools) { tool ->
                Card(
                    modifier = Modifier
                        .size(150.dp)
                        .clickable {
                            navController.navigate(tool.screen.route)
                        },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = tool.id),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BrownLight)
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}
