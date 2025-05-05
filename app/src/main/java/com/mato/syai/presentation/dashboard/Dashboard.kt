package com.mato.syai.presentation.dashboard

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mato.syai.core.model.TrackerCardItem
import com.mato.syai.core.composables.FitnessTracker

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun Place() {
    var list by remember {
        mutableStateOf(
            List(10) { index -> TrackerCardItem(id = index, isExpanded = false, tracker = FitnessTracker()) }
        )
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            list.size,
            key = { list[it].id },
            span = { item -> GridItemSpan(if (list[item].isExpanded) 2 else 1)}
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        list = list.map {
                            if (it.id == list[index].id)
                                it.copy(isExpanded = !it.isExpanded)
                            else
                                it.copy(isExpanded = false)
                        }
                    }
            ) {
                if (list[index].isExpanded) {
                    list[index].tracker.RectanglePreview()()
                } else {
                    list[index].tracker.SquarePreview()()
                }
            }
        }
    }
}