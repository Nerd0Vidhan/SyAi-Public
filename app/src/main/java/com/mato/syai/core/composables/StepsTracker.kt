//package com.mato.syai.core.composables
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.State
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import com.mato.syai.R
//import com.mato.syai.core.model.FitnessItem
//import com.mato.syai.presentation.dashboard.stepCounter.CounterPreview
//import com.mato.syai.presentation.dashboard.stepCounter.StepCounterService
//import com.mato.syai.presentation.dashboard.stepCounter.Timer
//import com.mato.syai.ui.theme.BrownLight
//import com.mato.syai.ui.theme.LightPurple
//import java.security.Provider
//import java.security.Provider.Service
//
//class StepsTracker(context : Context) : TrackerInterface {
//
////    DashboardScreen(context = context)
//
////    StepCounterService.DashboardScreen(context = context)
//
//    private val _steps = mutableStateOf(0)
//    val steps: State<Int> get() = _steps
//
//    private val stepCountReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            val stepCount = intent?.getIntExtra("step_count", 0) ?: 0
//            _steps.value = stepCount
//        }
//    }
//
//    init {
//        // Register the receiver to get updates
//        context.registerReceiver(
//            stepCountReceiver,
//            IntentFilter(StepCounterService.STEP_COUNT_UPDATE),
//            Context.RECEIVER_NOT_EXPORTED
//        )
//
//    }
//
//    override fun SquarePreview(): @Composable () -> Unit = {
//        SquareCard {
//            Column(modifier = Modifier.padding(8.dp)) {
////                CounterPreview()
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Timer(
//                        totalTime = 100L * 1000L,
//                        handleColor = BrownLight,
//                        inactiveBarColor = BrownLight,
//                        activeBarColor = LightPurple,
//                        modifier = Modifier.size(110.dp),
//                        steps = _steps.value.toString(),
//                    )
//                }
//            }
//        }
//    }
//
//    override fun RectanglePreview(): @Composable () -> Unit = {
//        RectangleCard {
//            CounterPreview()
//        }
//    }
//
//
//
//
//    fun startTracking(context: Context) {
//        // Start the service to begin tracking
//        val intent = Intent(context, StepCounterService::class.java)
//        ContextCompat.startForegroundService(context, intent)
//
//    }
//
//    fun stopTracking(context: Context) {
//        // Stop the service when done
//        context.stopService(Intent(context, StepCounterService::class.java))
//    }
//
//    fun unregisterReceiver(context: Context) {
//        context.unregisterReceiver(stepCountReceiver)
//    }
//}

package com.mato.syai.core.composables

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.mato.syai.presentation.dashboard.stepCounter.CounterPreview
import com.mato.syai.presentation.dashboard.stepCounter.StepCounterService
import com.mato.syai.presentation.dashboard.stepCounter.Timer
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple

class StepsTracker(context: Context) : TrackerInterface {

//    var context = LocalContext.current
//
//    StepScreen(context)

    private val _steps = mutableStateOf(0)
    val steps: State<Int> get() = _steps

    private val stepCountReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val stepCount = intent?.getIntExtra("step_count", 0) ?: 0
            _steps.value = stepCount
        }
    }

    init {
        context.registerReceiver(
            stepCountReceiver,
            IntentFilter(StepCounterService.STEP_COUNT_UPDATE),
            Context.RECEIVER_NOT_EXPORTED
        )
    }

    override fun SquarePreview(): @Composable () -> Unit = {
        SquareCard {
            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Timer(
                        totalTime = 100L * 1000L,
                        handleColor = BrownLight,
                        inactiveBarColor = BrownLight,
                        activeBarColor = LightPurple,
                        modifier = Modifier.size(110.dp),
                        steps = _steps.value.toString(),
                    )
                }
            }
        }
    }

    override fun RectanglePreview(): @Composable () -> Unit = {
        RectangleCard {
            CounterPreview()
        }
    }

    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(stepCountReceiver)
    }

    fun ensureServiceRunning(context: Context) {
        val intent = Intent(context, StepCounterService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }
}
