package com.mato.syai.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mato.syai.core.model.TrackerCardItem
import com.mato.syai.core.composables.FitnessTracker
import com.mato.syai.core.composables.StepsTracker
import com.mato.syai.presentation.dashboard.stepCounter.StepScreen

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreen() {
    var context = LocalContext.current
        var trackerList by remember {
            mutableStateOf(
                listOf(
                    TrackerCardItem(1, tracker = FitnessTracker()),
                    TrackerCardItem(2, tracker = StepsTracker(context))
                    StepScreen(context = context)
                )
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)

        ) {
            items(
                trackerList.size,
                key = { trackerList[it].id },
                span = { index ->
                    GridItemSpan(if (trackerList[index].isExpanded) 2 else 1)
                }
            ) { index ->
                val item = trackerList[index]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            trackerList = trackerList.map {
                                if (it.id == item.id)
                                    it.copy(isExpanded = !it.isExpanded)
                                else
                                    it.copy(isExpanded = false)
                            }
                        },
                ) {
                    if (item.isExpanded) {
                        item.tracker.RectanglePreview()()
                    } else {
                        item.tracker.SquarePreview()()
                    }
                }
            }
        }
    }
