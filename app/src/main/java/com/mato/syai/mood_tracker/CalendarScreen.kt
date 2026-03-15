//package com.mato.syai.mood_tracker
//
//import androidx.compose.foundation.*
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.grid.*
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.ArrowBack
//import androidx.compose.material.icons.filled.ArrowForward
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
//import coil.compose.AsyncImage
//import com.mato.syai.ui.theme.LightPurple
//import com.mato.syai.ui.theme.PurpleDark
//import java.time.LocalDate
//import java.time.YearMonth
//
//data class MoodEntry(
//    val date: LocalDate,
//    val moodRes: Int,
//    val journal: String = ""
//)
//
//@Composable
//fun MoodCalendarScreen() {
//    val moodEntries = remember { mutableStateMapOf<LocalDate, MoodEntry>() }
//    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
//    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
//    var journalText by remember { mutableStateOf("") }
//
//    val selectedMoodEntry = moodEntries[selectedDate]
//    val selectedMoodRes = selectedMoodEntry?.moodRes
//    val today = LocalDate.now()
//    val isTodaySelected = selectedDate == today
//
//    // When selectedDate changes, update journalText to match entry
//    LaunchedEffect(selectedDate, moodEntries) {
//        journalText = moodEntries[selectedDate]?.journal ?: ""
//    }
//
//    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
//        MonthNavigation(
//            currentYearMonth = currentYearMonth,
//            onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
//            onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) }
//        )
//
//        CalendarGrid(
//            yearMonth = currentYearMonth,
//            selectedDate = selectedDate,
//            moodEntries = moodEntries,
//            onDateSelected = { date ->
//                selectedDate = date
//            }
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))
//
//        if (isTodaySelected) {
//            MoodSelectorCardGrid(
//                selectedMood = selectedMoodRes,
//                onMoodSelected = { moodRes ->
//                    // Save or update mood only for today
//                    moodEntries[today] = MoodEntry(
//                        date = today,
//                        moodRes = moodRes,
//                        journal = journalText
//                    )
//                }
//            )
//            // TODO: Add journal input here if you want to allow adding journal for today
//        } else {
//            selectedMoodRes?.let { moodRes ->
//                Text(
//                    "Mood on ${selectedDate}:",
//                    style = MaterialTheme.typography.titleMedium,
//                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                )
//                MoodCard(gifRes = moodRes, isSelected = false, onClick = { })
//            }
//            if (journalText.isNotBlank()) {
//                Text(
//                    "Journal:",
//                    style = MaterialTheme.typography.titleMedium,
//                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
//                )
//                Text(
//                    journalText,
//                    style = MaterialTheme.typography.bodyMedium,
//                    modifier = Modifier.padding(horizontal = 16.dp)
//                )
//            }
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                "You can update mood only for today.",
//                style = MaterialTheme.typography.bodyMedium,
//                color = Color.Gray,
//                modifier = Modifier.padding(16.dp)
//            )
//        }
//    }
//}
//
//@Composable
//fun MonthNavigation(
//    currentYearMonth: YearMonth,
//    onPreviousMonth: () -> Unit,
//    onNextMonth: () -> Unit
//) {
//    Row(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 16.dp, vertical = 8.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        IconButton(onClick = onPreviousMonth) {
//            Icon(
//                imageVector = Icons.Default.ArrowBack,
//                contentDescription = "Previous Month"
//            )
//        }
//        Text(
//            text = "${currentYearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentYearMonth.year}",
//            style = MaterialTheme.typography.titleMedium,
//            fontWeight = FontWeight.Bold
//        )
//        IconButton(onClick = onNextMonth) {
//            Icon(
//                imageVector = Icons.Default.ArrowForward,
//                contentDescription = "Next Month"
//            )
//        }
//    }
//}
//
//@Composable
//fun CalendarGrid(
//    yearMonth: YearMonth,
//    selectedDate: LocalDate,
//    moodEntries: Map<LocalDate, MoodEntry>,
//    onDateSelected: (LocalDate) -> Unit
//) {
//    val startOfMonth = yearMonth.atDay(1)
//    val totalDays = yearMonth.lengthOfMonth()
//    val firstDayOfWeek = startOfMonth.dayOfWeek.value % 7 // Sunday=0, Monday=1 ... adjust for calendar layout
//
//    LazyVerticalGrid(
//        columns = GridCells.Fixed(7),
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(300.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        contentPadding = PaddingValues(4.dp)
//    ) {
//        // Add blank spaces for days before start of month
//        items(firstDayOfWeek) {
//            Box(modifier = Modifier.size(44.dp)) { /* empty cell */ }
//        }
//
//        // Days in month
//        items(totalDays) { index ->
//            val date = startOfMonth.plusDays(index.toLong())
//            val mood = moodEntries[date]?.moodRes
//
//            Box(
//                modifier = Modifier
//                    .size(44.dp)
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(if (date == selectedDate) LightPurple else Color.LightGray)
//                    .clickable { onDateSelected(date) },
//                contentAlignment = Alignment.Center
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text(text = "${date.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
//                    if (mood != null) {
//                        AsyncImage(
//                            model = mood,
//                            contentDescription = null,
//                            modifier = Modifier.size(24.dp)
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//
//@Preview
//@Composable
//fun MoodCalendarScreenPreview() {
//    MoodCalendarScreen()
//}


package com.mato.syai.mood_tracker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import coil.decode.GifDecoder
import coil.request.ImageRequest
import coil.ImageLoader
import android.os.Build

@Composable
fun AnimatedMoodZoomImage(moodRes: Int, onAnimationEnd: () -> Unit) {
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context).components {
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
        }.build()
    }

    val scaleAnim = remember { Animatable(0.5f) }
    val alphaAnim = remember { Animatable(0.1f) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1.5f, animationSpec = tween(800))
        alphaAnim.animateTo(0f, animationSpec = tween(800))
        onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(moodRes)
                    .build(),
                imageLoader = imageLoader
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(200.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        )
    }
}
