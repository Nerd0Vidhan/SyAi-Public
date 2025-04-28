package com.mato.syai.presentation.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mato.syai.R


open class shape {
    @Composable
    fun Square(text: String = "", image: Int? = null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                image?.let { painterResource(it) }
                    ?.let { Image(painter = it, contentDescription = "") }
                Text(text = text)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HeartBeat(){
    Square("Heart Rate", R.drawable.heart)
}

@Composable
fun Square(x0: String, x1: Int) {
    TODO("Not yet implemented")
}

class Anim : shape() {

}


//fun Oxygen(){
//    TextField()
//    val state = rememberScrollState()
////    Square
//}