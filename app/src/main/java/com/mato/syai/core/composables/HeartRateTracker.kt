package com.mato.syai.core.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mato.syai.R
import com.mato.syai.core.model.FitnessItem
import com.mato.syai.ui.theme.PurpleDark

class FitnessTracker: TrackerInterface{
    override fun SquarePreview(): @Composable () -> Unit = {
        SquareCard {
            Column(modifier = Modifier.padding(8.dp).background(PurpleDark)) {
//                Image(
//                    painter = painterResource(id = R.drawable.heart),
//                    contentDescription = null,
//                    modifier = Modifier.size(70.dp)
//                )
//                Text(text = "Heart Rate")
            }
        }
    }

    override fun RectanglePreview(): @Composable () -> Unit = {
        val fitnessItems = listOf(
            FitnessItem(R.drawable.heart, "Heart Rate"),
            FitnessItem(R.drawable.heart, "Steps"),
            FitnessItem(R.drawable.heart, "Calories")
        )
        RectangleCard {

//            Row(
//                modifier = Modifier.padding(1.dp),
//                horizontalArrangement = Arrangement.spacedBy(15.dp)
//            ){
//                fitnessItems.forEach { item ->
//                    Column(modifier = Modifier.padding(8.dp)) {
//                        Image(
//                            painter = painterResource(id = item.image),
//                            contentDescription = null,
//                            modifier = Modifier.size(70.dp)
//                        )
//                        Text(text = item.text)
//                    }
//                }
//            }
        }
    }
}