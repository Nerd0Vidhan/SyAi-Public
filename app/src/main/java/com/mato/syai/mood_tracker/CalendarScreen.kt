package com.mato.syai.mood_tracker

import android.os.Build
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.mato.syai.R
import com.mato.syai.ui.theme.LightPurple
import com.mato.syai.ui.theme.PurpleDark
import java.time.LocalDate
import java.time.YearMonth

data class MoodEntry(
    val date: LocalDate,
    val moodRes: Int,
    val journal: String = ""
)

@Composable
fun MoodCalendarScreen() {
    var burstGif by remember { mutableStateOf<Int?>(null) }
    val moodEntries = remember { mutableStateMapOf<LocalDate, MoodEntry>() }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    var journalText by remember { mutableStateOf("") }

    val selectedMoodEntry = moodEntries[selectedDate]
    val selectedMoodRes = selectedMoodEntry?.moodRes

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        CalendarGrid(
            selectedDate = selectedDate,
            moodEntries = moodEntries,
            onDateSelected = {
                selectedDate = it
                journalText = moodEntries[it]?.journal ?: ""
            }
        )

//        MoodSelectorCardGrid(
//            onMoodSelected = { moodRes ->
//                burstGif = moodRes // show burst
//                moodEntries[selectedDate] = MoodEntry(
//                    date = selectedDate,
//                    moodRes = moodRes,
//                    journal = journalText
//                )
//            },
//            selectedMood = selectedMoodRes
//        )


//        TextField(
//            value = journalText,
//            onValueChange = {
//                journalText = it
//                selectedMoodRes?.let { mood ->
//                    moodEntries[selectedDate] = MoodEntry(
//                        date = selectedDate,
//                        moodRes = mood,
//                        journal = it
//                    )
//                }
//            },
//            label = { Text("Why do you feel this way?") },
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            maxLines = 4
//        )

//        burstGif?.let { gif ->
//            MoodBurstEffect(gifRes = gif) {
//                burstGif = null // clear when animation completes
//            }
//        }
    }
}

@Composable
fun CalendarGrid(
    selectedDate: LocalDate,
    moodEntries: Map<LocalDate, MoodEntry>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val yearMonth = YearMonth.of(today.year, today.month)
    val startOfMonth = yearMonth.atDay(1)
    val totalDays = yearMonth.lengthOfMonth()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "${today.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${today.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(totalDays) { index ->
                val date = startOfMonth.plusDays(index.toLong())
                val mood = moodEntries[date]?.moodRes

                Box(
                    modifier = Modifier
                        .size(44.dp)
//                        .clip(RoundedCornerShape(8.dp))
                        .background(if (date == selectedDate) LightPurple else Color.LightGray)
                        .clickable { onDateSelected(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${date.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
                        if (mood != null) {
                            AsyncImage(
                                model = mood,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}