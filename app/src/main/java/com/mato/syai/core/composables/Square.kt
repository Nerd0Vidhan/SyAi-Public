package com.mato.syai.core.animation.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mato.syai.R

@Composable
fun Square(text: String = "hello") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Image(painter = painterResource(id = R.drawable.syai_square_logo), contentDescription = "")
            Text(text = text)
        }
    }
}