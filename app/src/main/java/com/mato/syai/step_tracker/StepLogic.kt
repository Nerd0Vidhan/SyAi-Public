package com.mato.syai.step_tracker

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun StepCounterScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val viewModel: StepCounterViewModel = viewModel(
        factory = StepCounterViewModelFactory(application)
    )

    val stepCount by viewModel.stepCount.collectAsState()

    val sensorAvailable = remember {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!sensorAvailable) {
            Text(
                "Step counter sensor not available on this device.",
                fontSize = 25.sp,
                color = LightPurple
            )
        } else {
            Timer(
                steps = stepCount.toString(),
                targetSteps = 100,
                handleColor = BrownLight,
                inactiveBarColor = BrownLight,
                activeBarColor = LightPurple,
                modifier = Modifier.size(135.dp)
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun StepCounterPreview() {
    StepCounterScreen()
}
