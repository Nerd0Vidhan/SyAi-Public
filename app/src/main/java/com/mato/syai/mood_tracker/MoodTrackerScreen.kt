package com.mato.syai.mood_tracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.mato.syai.R
import com.mato.syai.ui.theme.BrownLight
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark

@Composable
fun MoodSelectorCardGrid() {
    val gifList = listOf(
        R.drawable.happy,
        R.drawable.sad,
        R.drawable.abuse,
        R.drawable.angry,
        R.drawable.cry,
        R.drawable.kissy,
        R.drawable.love,
        R.drawable.wink,
        R.drawable.surprised
    )

    var selectedIndex by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "How do you feel today?",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(400.dp)
        ) {
            items(gifList.size) { index ->
                MoodCard(
                    gifRes = gifList[index],
                    isSelected = selectedIndex == index,
                    onClick = { selectedIndex = index }
                )
            }
        }
    }
}

@Composable
fun MoodCard(gifRes: Int, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .size(100.dp)
            .clickable { onClick() }.background(Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        border = if (isSelected) BorderStroke(3.dp, PurpleDark) else null
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(gifRes)
                .decoderFactory(
                    if (android.os.Build.VERSION.SDK_INT >= 28)
                        ImageDecoderDecoder.Factory()
                    else
                        GifDecoder.Factory()
                )
                .build(),
            contentDescription = "Mood GIF",
            modifier = Modifier.fillMaxSize().background(LightPurple)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMoodSelectorCardGrid() {
    MoodSelectorCardGrid()
}
