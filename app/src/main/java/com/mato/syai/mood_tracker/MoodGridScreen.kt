package com.mato.syai.mood_tracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
//import kotlinx.coroutines.DefaultExecutor.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodGridScreen(viewModel: MoodViewModel, onHistoryClick: () -> Unit) {
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val todayStr = today.format(formatter)

    val allEntries by viewModel.allEntries.collectAsState()

    var selectedMoodRes by remember { mutableStateOf<Int?>(null) }
    var journalText by remember { mutableStateOf("") }
    var showJournalBox by remember { mutableStateOf(false) }
    var showBurst by remember { mutableStateOf(false) }
    var showMoodZoom by remember { mutableStateOf(false) }

    // Animate flow: mood zoom -> burst -> journal
    LaunchedEffect(selectedMoodRes) {
        selectedMoodRes?.let {
            showMoodZoom = true
            delay(800) // mood zoom-in duration
            showBurst = true
            delay(600) // burst duration
            val entry = allEntries.find { it.date == todayStr }
            journalText = entry?.journal ?: ""
            showJournalBox = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            TopAppBar(
                title = { Text("Select Your Mood") },
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )

            MoodGifGrid(
                selectedMoodRes = selectedMoodRes,
                onMoodSelected = { moodRes ->
                    selectedMoodRes = moodRes
                    showJournalBox = false
                }
            )
        }

        // Enlarged animated mood in background
        if (showMoodZoom && selectedMoodRes != null) {
            AnimatedMoodZoomImage(
                moodRes = selectedMoodRes!!,
                onAnimationEnd = { showMoodZoom = false }
            )
        }

        // Burst animation
        if (showBurst && selectedMoodRes != null) {
            MoodBurstEffect(show = true, moodRes = selectedMoodRes) {
                showBurst = false
            }
        }

        // Journal Input
        // Journal Input
        if (showJournalBox) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                JournalInputBox(
                    journalText = journalText,
                    onTextChange = {
                        if (it.length <= 200) journalText = it
                    },
                    onSave = {
                        if (selectedMoodRes != null) {
                            viewModel.saveMoodEntry(todayStr, selectedMoodRes!!, journalText)
                            showJournalBox = false
                            selectedMoodRes = null
                            journalText = ""
                        }
                    },
                    onCancel = {
                        showJournalBox = false
                        selectedMoodRes = null
                        journalText = ""
                    }
                )
            }
        }

    }
}
