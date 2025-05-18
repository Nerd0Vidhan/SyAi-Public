package com.mato.syai.step_tracker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mato.syai.core.composables.TrackerInterface

class StepCountTracker : TrackerInterface {

    override fun SquarePreview(): @Composable () -> Unit = {
        StepCounterScreen()
    }

    override fun RectanglePreview(): @Composable () -> Unit = {
        Row(){
            StepCounterScreen()
            Text(text = "Change Goal", modifier = Modifier.clickable(enabled = true, onClick = {}))
//            Text(text = "Steps")
        }
    }
}
