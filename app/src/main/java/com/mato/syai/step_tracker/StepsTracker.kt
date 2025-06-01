package com.mato.syai.step_tracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mato.syai.core.composables.TrackerInterface

class StepCountTracker : TrackerInterface {

    override fun SquarePreview(): @Composable () -> Unit = {
        StepCounterScreen()
    }

    override fun RectanglePreview(): @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ){
            StepCounterScreen()
            Text(text = "Change Goal", modifier = Modifier.clickable(enabled = true, onClick = {}))
//            Text(text = "Steps")
        }
    }
}
