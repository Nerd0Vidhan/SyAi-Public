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
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.mato.syai.R
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark
import java.time.LocalDate


@Composable
fun MoodSelectorCardGrid(
    onMoodSelected: (Int) -> Unit,
    selectedMood: Int? = null
) {
    var burstGif by remember { mutableStateOf<Int?>(null) }
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

    var selectedIndex by remember(selectedMood) {
        mutableStateOf(gifList.indexOf(selectedMood))
    }

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
                    onClick = {
                        selectedIndex = index
                        onMoodSelected(gifList[index])
                    }
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
            .clickable { onClick() }
            .background(Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        border = if (isSelected) BorderStroke(3.dp, PurpleDark) else null
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(gifRes)
                .decoderFactory(
                    if (Build.VERSION.SDK_INT >= 28)
                        ImageDecoderDecoder.Factory()
                    else
                        GifDecoder.Factory()
                )
                .build(),  // This is the call that was causing the issue
            contentDescription = "Mood GIF",
            modifier = Modifier
                .fillMaxSize()
                .background(LightPurple)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMoodCalendarScreen() {
    var burstGif by remember { mutableStateOf<Int?>(null) }
    val moodEntries = remember { mutableStateMapOf<LocalDate, MoodEntry>() }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var journalText by remember { mutableStateOf("") }

    val selectedMoodEntry = moodEntries[selectedDate]
    val selectedMoodRes = selectedMoodEntry?.moodRes
    MoodSelectorCardGrid(
            onMoodSelected = { moodRes ->
                burstGif = moodRes // show burst
                moodEntries[selectedDate] = MoodEntry(
                    date = selectedDate,
                    moodRes = moodRes,
                    journal = journalText
                )
            },
            selectedMood = selectedMoodRes
        )
    burstGif?.let { gif ->
            MoodBurstEffect(gifRes = gif) {
                burstGif = null // clear when animation completes
            }
        }
}


@Composable
fun MoodBurstEffect(
    gifRes: Int,
    onAnimationEnd: () -> Unit
) {
    val visible = remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (visible.value) 1.5f else 0f,
        animationSpec = tween(durationMillis = 1000),
        finishedListener = { onAnimationEnd() }
    )

    if (visible.value) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            AsyncImage(
                model = gifRes,
                contentDescription = "Burst Mood",
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        alpha = 1 - scale / 2
                    )
            )
        }
    }
}
