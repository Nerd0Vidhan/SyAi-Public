package com.mato.syai.tools

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mato.syai.R
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark

@Composable
fun ToolsScreen() {
    val tools = listOf(
        R.drawable.notes,
        R.drawable.step_counter,
        R.drawable.digital_wellbeing,
    )

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
            items(tools.size) { index ->
                Card(
                    modifier = Modifier
                        .size(150.dp)
                        .clickable {  },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Image(painter = painterResource(id = tools[index]), contentDescription = null,
                        modifier = Modifier.background(BrownLight).fillMaxSize().padding(10.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewToolsScreen() {
    ToolsScreen()
}
