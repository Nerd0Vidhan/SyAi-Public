//package com.mato.syai.step_tracker
//
//import com.mato.syai.core.composables.RectangleCard
//import com.mato.syai.core.composables.SquareCard
//import com.mato.syai.core.composables.TrackerInterface
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
////import com.mato.syai.presentation.dashboard.stepCounter.StepCounterService
//import com.mato.syai.step_tracker.Timer
//
////
////
//class Trial(step : String) : TrackerInterface {
//
//    @Composable
//    fun SquarePreview(function: () -> Unit): @Composable () -> Unit {
//        SquareCard {
//            Column(modifier = Modifier.padding(8.dp)) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Timer(
//                        steps = step,
//                        targetSteps = 100,
//                        handleColor = Color.Blue,
//                        inactiveBarColor = Color.Gray,
//                        activeBarColor = Color.Green,
//                        modifier = Modifier.size(110.dp),
//                        strokeWidth = 8.dp
//                    )
//                }
//            }
//        }
//    }
//
//
//    override fun RectanglePreview(): @Composable () -> Unit = {
//        RectangleCard {
////            CounterPreview()
//            Row(modifier = Modifier.padding(8.dp)) {
//                Box(
//                    modifier = Modifier.fillMaxSize(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Timer(
//                        steps = step,
//                        targetSteps = 100,
//                        handleColor = Color.Blue,
//                        inactiveBarColor = Color.Gray,
//                        activeBarColor = Color.Green,
//                        modifier = Modifier.size(110.dp),
//                        strokeWidth = 8.dp
//                    )
//
//                    Text(text = "Target Steps: 100")
//                }
//            }
//        }
//    }
//
//}