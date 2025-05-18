//package com.mato.syai.step_tracker
//
//import android.content.Context
//import android.hardware.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import kotlin.math.sqrt
//
//@Composable
//fun StepComparisonScreen() {
//    val context = LocalContext.current
//    val sensorManager = remember {
//        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
//    }
//
//    // Step counts
//    var accelSteps by remember { mutableStateOf(0) }
//    var detectorSteps by remember { mutableStateOf(0) }
//    var counterBase by remember { mutableStateOf(-1f) }
//    var counterSteps by remember { mutableStateOf(0f) }
//
//    // Gravity filter
//    val gravity = remember { FloatArray(3) { 9.81f } }
//    val alpha = 0.8f
//    var lastStepTime by remember { mutableStateOf(0L) }
//    val stepThreshold = 10.0f
//    val stepInterval = 500L
//
//    DisposableEffect(Unit) {
//        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//        val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
//        val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
//
//        val listener = object : SensorEventListener {
//            override fun onSensorChanged(event: SensorEvent?) {
//                if (event == null) return
//
//                when (event.sensor.type) {
//
//                    // ✅ Custom Accelerometer Detection
//                    Sensor.TYPE_ACCELEROMETER -> {
//                        val x = event.values[0]
//                        val y = event.values[1]
//                        val z = event.values[2]
//
//                        gravity[0] = alpha * gravity[0] + (1 - alpha) * x
//                        gravity[1] = alpha * gravity[1] + (1 - alpha) * y
//                        gravity[2] = alpha * gravity[2] + (1 - alpha) * z
//
//                        val fx = x - gravity[0]
//                        val fy = y - gravity[1]
//                        val fz = z - gravity[2]
//
//                        val magnitude = sqrt(fx * fx + fy * fy + fz * fz)
//                        val now = System.currentTimeMillis()
//
//                        if (magnitude > stepThreshold && (now - lastStepTime > stepInterval)) {
//                            lastStepTime = now
//                            accelSteps++
//                        }
//                    }
//
//                    // ✅ STEP_DETECTOR — 1 step per event
//                    Sensor.TYPE_STEP_DETECTOR -> {
//                        detectorSteps++
//                    }
//
//                    // ✅ STEP_COUNTER — total since boot
//                    Sensor.TYPE_STEP_COUNTER -> {
//                        val total = event.values[0]
//                        if (counterBase < 0) {
//                            counterBase = total
//                        }
//                        counterSteps = total - counterBase
//                    }
//                }
//            }
//
//            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
//        }
//
//        // Register all sensors if available
//        accel?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
//        detector?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
//        counter?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }
//
//        onDispose {
//            sensorManager.unregisterListener(listener)
//        }
//    }
//
//    // UI
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(24.dp),
//        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Text("👟 Step Comparison")
//
//        Text("Accelerometer (custom): $accelSteps")
//        Text("TYPE_STEP_DETECTOR: $detectorSteps")
//        Text("TYPE_STEP_COUNTER: ${counterSteps.toInt()}")
//    }
//}
//
//@Preview(showSystemUi = true, showBackground = true)
//@Composable
//fun StepCounterPreview() {
//    StepComparisonScreen()
//}
