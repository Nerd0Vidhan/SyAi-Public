package com.mato.syai.step_tracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark
import androidx.compose.ui.unit.sp

@Composable
fun StepCounterScreen() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    // Changed from `remember` to `rememberSaveable` to persist the state across recompositions
    var baseStepCount by rememberSaveable { mutableStateOf(-1f) }
    var stepCount by rememberSaveable { mutableStateOf(0f) }

    val stepCounterSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    DisposableEffect(stepCounterSensor) {
        if (stepCounterSensor == null) return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || event.values.isEmpty()) return
                val totalSteps = event.values[0]
                if (baseStepCount < 0) {
                    baseStepCount = totalSteps
                }
                stepCount = totalSteps - baseStepCount
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp).background(PurpleDark),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (stepCounterSensor == null) {
            Text("Step counter sensor not available on this device.", fontSize = 25.sp)
        } else {
            Timer(
                steps = stepCount.toInt().toString(),
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
