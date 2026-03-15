package com.mato.syai.mood_tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import coil.ImageLoader
import com.mato.syai.R
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.WhitePurple

@Composable
fun MoodGifGrid(selectedMoodRes: Int?, onMoodSelected: (Int) -> Unit) {
    val gifList = listOf(
        R.drawable.happy, R.drawable.sad, R.drawable.abuse,
        R.drawable.angry, R.drawable.cry, R.drawable.kissy,
        R.drawable.love, R.drawable.wink, R.drawable.surprised,
        R.drawable.worry, R.drawable.vomit, R.drawable.sick,
        R.drawable.sleep, R.drawable.party
    )

    val selectedIndex = selectedMoodRes?.let { gifList.indexOf(it) } ?: -1
    val context = LocalContext.current

    // Custom ImageLoader to support GIFs
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .height(400.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(gifList.size) { index ->
            val isSelected = index == selectedIndex
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(gifList[index])
                    .build(),
                imageLoader = imageLoader
            )

            Card(
                modifier = Modifier
                    .size(100.dp)
                    .clickable { onMoodSelected(gifList[index]) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = "Mood GIF",
                    modifier = Modifier
                        .background(if (isSelected) WhitePurple else LightPurple).fillMaxSize()
                )
            }
        }
    }
}
