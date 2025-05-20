package com.mato.syai.mood_tracker
////
////import androidx.compose.foundation.BorderStroke
////import androidx.compose.foundation.background
////import androidx.compose.foundation.clickable
////import androidx.compose.foundation.layout.*
////import androidx.compose.foundation.lazy.grid.GridCells
////import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
////import androidx.compose.foundation.shape.RoundedCornerShape
////import androidx.compose.material3.*
////import androidx.compose.runtime.*
////import androidx.compose.ui.Alignment
////import androidx.compose.ui.Modifier
////import androidx.compose.ui.graphics.Color
////import androidx.compose.ui.platform.LocalContext
////import androidx.compose.ui.tooling.preview.Preview
////import androidx.compose.ui.unit.dp
////import coil.compose.AsyncImage
////import android.os.Build
////import androidx.compose.animation.core.animateFloatAsState
////import androidx.compose.animation.core.tween
////import androidx.compose.material.icons.Icons
////import androidx.compose.material.icons.filled.History
////import androidx.compose.ui.graphics.graphicsLayer
////import coil.decode.GifDecoder
////import coil.decode.ImageDecoderDecoder
////import coil.request.ImageRequest
////import com.mato.syai.R
import com.mato.syai.ui.theme.LightPurple
////import com.mato.syai.ui.theme.PurpleDark
////import java.time.LocalDate
////
////@Composable
////fun MoodSelectorCardGrid(
////    onMoodSelected: (Int) -> Unit,
////    selectedMood: Int? = null
////) {
////    var burstGif by remember { mutableStateOf<Int?>(null) }
////    val gifList = listOf(
////        R.drawable.happy,
////        R.drawable.sad,
////        R.drawable.abuse,
////        R.drawable.angry,
////        R.drawable.cry,
////        R.drawable.kissy,
////        R.drawable.love,
////        R.drawable.wink,
////        R.drawable.surprised,
////        R.drawable.worry,
////        R.drawable.vomit,
////        R.drawable.sick,
////        R.drawable.sleep,
////        R.drawable.party
////
////    )
////
////    var selectedIndex by remember(selectedMood) {
////        mutableStateOf(gifList.indexOf(selectedMood))
////    }
////
////    Column(
////        modifier = Modifier.fillMaxWidth(),
////        horizontalAlignment = Alignment.CenterHorizontally
////    ) {
////        Row(
////            modifier = Modifier
////                .fillMaxWidth()
////                .padding(16.dp),
////            verticalAlignment = Alignment.CenterVertically,
////            horizontalArrangement = Arrangement.SpaceBetween
////        ){
////            Text(
////                text = "How do you feel today?",
////                style = MaterialTheme.typography.titleLarge,
////                modifier = Modifier.padding(16.dp)
////            )
////            IconButton(onClick = { /*TODO*/ }) {
////                Icon(
////                    imageVector = Icons.Default.History,
////                    contentDescription = "Info",
////                    tint = PurpleDark,
////                )
////            }
////
////        }
////
////        LazyVerticalGrid(
////            columns = GridCells.Fixed(3),
////            contentPadding = PaddingValues(16.dp),
////            verticalArrangement = Arrangement.spacedBy(16.dp),
////            horizontalArrangement = Arrangement.spacedBy(16.dp),
////            modifier = Modifier.height(400.dp)
////        ) {
////            items(gifList.size) { index ->
////                MoodCard(
////                    gifRes = gifList[index],
////                    isSelected = selectedIndex == index,
////                    onClick = {
////                        selectedIndex = index
////                        onMoodSelected(gifList[index])
////                    }
////                )
////            }
////        }
////    }
////}
////
////@Composable
////fun MoodCard(gifRes: Int, isSelected: Boolean, onClick: () -> Unit) {
////    val context = LocalContext.current
////
////    Card(
////        modifier = Modifier
////            .size(100.dp)
////            .clickable { onClick() }
////            .background(Color.Transparent),
////        shape = RoundedCornerShape(16.dp),
////        elevation = CardDefaults.cardElevation(8.dp),
////        border = if (isSelected) BorderStroke(3.dp, PurpleDark) else null
////    ) {
////        AsyncImage(
////            model = ImageRequest.Builder(context)
////                .data(gifRes)
////                .decoderFactory(
////                    if (Build.VERSION.SDK_INT >= 28)
////                        ImageDecoderDecoder.Factory()
////                    else
////                        GifDecoder.Factory()
////                )
////                .build(),
////            contentDescription = "Mood GIF",
////            modifier = Modifier
////                .fillMaxSize()
////                .background(LightPurple)
////        )
////    }
////}
////
////@Preview(showBackground = true)
////@Composable
////fun PreviewMoodCalendarScreen() {
////    var burstGif by remember { mutableStateOf<Int?>(null) }
////    val moodEntries = remember { mutableStateMapOf<LocalDate, MoodEntry>() }
////    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
////
////    var journalText by remember { mutableStateOf("") }
////
////    val selectedMoodEntry = moodEntries[selectedDate]
////    val selectedMoodRes = selectedMoodEntry?.moodRes
////    MoodSelectorCardGrid(
////            onMoodSelected = { moodRes ->
////                burstGif = moodRes // show burst
////                moodEntries[selectedDate] = MoodEntry(
////                    date = selectedDate,
////                    moodRes = moodRes,
////                    journal = journalText
////                )
////            },
////            selectedMood = selectedMoodRes
////        )
////    burstGif?.let { gif ->
////            MoodBurstEffect(gifRes = gif) {
////                burstGif = null // clear when animation completes
////            }
////        }
////}
////
////
////@Composable
////fun MoodBurstEffect(
////    gifRes: Int,
////    onAnimationEnd: () -> Unit
////) {
////    val visible = remember { mutableStateOf(true) }
////    val scale by animateFloatAsState(
////        targetValue = if (visible.value) 1.5f else 0f,
////        animationSpec = tween(durationMillis = 1000),
////        finishedListener = { onAnimationEnd() }
////    )
////
////    if (visible.value) {
////        Box(
////            contentAlignment = Alignment.Center,
////            modifier = Modifier
////                .fillMaxSize()
////                .background(Color.Transparent)
////        ) {
////            AsyncImage(
////                model = gifRes,
////                contentDescription = "Burst Mood",
////                modifier = Modifier
////                    .size(180.dp)
////                    .graphicsLayer(
////                        scaleX = scale,
////                        scaleY = scale,
////                        alpha = 1 - scale / 2
////                    )
////            )
////        }
////    }
////}
//
//
////package com.mato.syai.mood_tracker
////
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
//import com.mato.syai.R
//import java.time.LocalDate
import androidx.navigation.compose.composable
//import androidx.navigation.compose.navArgument
//import java.time.YearMonth
////
////// --- Data Model ---
////data class MoodEntry(
////    val date: LocalDate,
////    val moodRes: Int,
////    val journal: String = ""
////)
////
////// --- Mood Selector Grid ---
////@Composable
////fun MoodSelectorCardGrid(
////    onMoodSelected: (Int) -> Unit,
////    selectedMood: Int? = null
////) {
////    val context = LocalContext.current
////    val gifList = listOf(
////        R.drawable.happy, R.drawable.sad, R.drawable.abuse, R.drawable.angry,
////        R.drawable.cry, R.drawable.kissy, R.drawable.love, R.drawable.wink,
////        R.drawable.surprised, R.drawable.worry, R.drawable.vomit, R.drawable.sick,
////        R.drawable.sleep, R.drawable.party
////    )
////    var selectedIndex by remember(selectedMood) {
////        mutableStateOf(selectedMood?.let { gifList.indexOf(it) } ?: -1)
////    }
////
////    Column(
////        modifier = Modifier.fillMaxWidth(),
////        horizontalAlignment = Alignment.CenterHorizontally
////    ) {
////        Text(
////            text = "How do you feel today?",
////            style = MaterialTheme.typography.titleLarge,
////            modifier = Modifier.padding(16.dp)
////        )
////        LazyVerticalGrid(
////            columns = GridCells.Fixed(3),
////            contentPadding = PaddingValues(16.dp),
////            verticalArrangement = Arrangement.spacedBy(16.dp),
////            horizontalArrangement = Arrangement.spacedBy(16.dp),
////            modifier = Modifier.height(400.dp)
////        ) {
////            items(gifList.size) { index ->
////                val isSelected = index == selectedIndex
////                Card(
////                    modifier = Modifier
////                        .size(100.dp)
////                        .clickable {
////                            selectedIndex = index
////                            onMoodSelected(gifList[index])
////                        },
////                    shape = RoundedCornerShape(16.dp),
////                    elevation = CardDefaults.cardElevation(8.dp)
////                ) {
////                    AsyncImage(
////                        model = ImageRequest.Builder(context)
////                            .data(gifList[index])
////                            .decoderFactory(
////                                if (Build.VERSION.SDK_INT >= 28)
////                                    ImageDecoderDecoder.Factory()
////                                else
////                                    GifDecoder.Factory()
////                            )
////                            .build(),
////                        contentDescription = "Mood GIF",
////                        modifier = Modifier.background(LightPurple)
////                    )
////                }
////            }
////        }
////    }
////}
////
////// --- Mood History List ---
////@Composable
////fun MoodHistoryScreen(
////    moodEntries: Map<LocalDate, MoodEntry>,
////    onEntryClicked: (LocalDate) -> Unit,
////    modifier: Modifier = Modifier
////) {
////    LazyColumn(modifier = modifier.padding(16.dp)) {
////        items(moodEntries.toList().sortedByDescending { it.first }) { (date, entry) ->
////            Card(
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .padding(vertical = 4.dp)
////                    .clickable { onEntryClicked(date) },
////                elevation = CardDefaults.cardElevation(4.dp)
////            ) {
////                Row(
////                    modifier = Modifier.padding(12.dp),
////                    verticalAlignment = Alignment.CenterVertically
////                ) {
////                    AsyncImage(
////                        model = entry.moodRes,
////                        contentDescription = "Mood",
////                        modifier = Modifier.size(48.dp)
////                    )
////                    Spacer(Modifier.width(12.dp))
////                    Column {
////                        Text(text = date.toString(), style = MaterialTheme.typography.bodyMedium)
////                        Text(
////                            text = if (entry.journal.isNotBlank()) entry.journal.take(40) + "…" else "No journal",
////                            style = MaterialTheme.typography.bodySmall,
////                            color = Color.Gray
////                        )
////                    }
////                }
////            }
////        }
////    }
////}
////
////// --- Mood Journal Editor ---
////@OptIn(ExperimentalMaterial3Api::class)
////@Composable
////fun MoodJournalScreen(
////    date: LocalDate,
////    moodEntry: MoodEntry?,
////    onSave: (MoodEntry) -> Unit,
////    onBack: () -> Unit
////) {
////    var journalText by remember { mutableStateOf(TextFieldValue(moodEntry?.journal ?: "")) }
////
////    Scaffold(
////        topBar = {
////            TopAppBar(
////                title = { Text("Journal for $date") },
////                navigationIcon = {
////                    IconButton(onClick = onBack) {
////                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
////                    }
////                },
////                actions = {
////                    TextButton(onClick = {
////                        onSave(moodEntry?.copy(journal = journalText.text)
////                            ?: MoodEntry(date, moodEntry?.moodRes ?: R.drawable.happy, journalText.text))
////                    }) {
////                        Text("Save")
////                    }
////                }
////            )
////        }
////    ) { padding ->
////        Column(
////            modifier = Modifier
////                .padding(padding)
////                .padding(16.dp)
////        ) {
////            Text(
////                text = "Write about your day...",
////                style = MaterialTheme.typography.bodyLarge,
////                modifier = Modifier.padding(bottom = 8.dp)
////            )
////            TextField(
////                value = journalText,
////                onValueChange = { journalText = it },
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .height(200.dp)
////            )
////        }
////    }
////}
////
////// --- Calendar Screen ---
////@Composable
////fun MoodCalendarScreen(
////    moodEntries: Map<LocalDate, MoodEntry>,
////    onDateSelected: (LocalDate) -> Unit,
////    modifier: Modifier = Modifier
////) {
////    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
////    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
////
////    Column(modifier = modifier.padding(8.dp)) {
////        MonthNavigation(
////            currentYearMonth = currentYearMonth,
////            onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
////            onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) }
////        )
////
////        CalendarGrid(
////            yearMonth = currentYearMonth,
////            selectedDate = selectedDate,
////            moodEntries = moodEntries,
////            onDateSelected = { date ->
////                selectedDate = date
////                onDateSelected(date)
////            }
////        )
////    }
////}
////
////// --- Month Navigation ---
////@Composable
////fun MonthNavigation(
////    currentYearMonth: YearMonth,
////    onPreviousMonth: () -> Unit,
////    onNextMonth: () -> Unit
////) {
////    Row(
////        modifier = Modifier
////            .fillMaxWidth()
////            .padding(horizontal = 16.dp, vertical = 8.dp),
////        horizontalArrangement = Arrangement.SpaceBetween,
////        verticalAlignment = Alignment.CenterVertically
////    ) {
////        IconButton(onClick = onPreviousMonth) {
////            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
////        }
////        Text(
////            text = "${currentYearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentYearMonth.year}",
////            style = MaterialTheme.typography.titleMedium
////        )
////        IconButton(onClick = onNextMonth) {
////            Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
////        }
////    }
////}
////
////// --- Calendar Grid ---
////@Composable
////fun CalendarGrid(
////    yearMonth: YearMonth,
////    selectedDate: LocalDate,
////    moodEntries: Map<LocalDate, MoodEntry>,
////    onDateSelected: (LocalDate) -> Unit
////) {
////    val startOfMonth = yearMonth.atDay(1)
////    val totalDays = yearMonth.lengthOfMonth()
////    val firstDayOfWeek = startOfMonth.dayOfWeek.value % 7
////
////    LazyVerticalGrid(
////        columns = GridCells.Fixed(7),
////        modifier = Modifier
////            .fillMaxWidth()
////            .height(300.dp),
////        verticalArrangement = Arrangement.spacedBy(8.dp),
////        horizontalArrangement = Arrangement.spacedBy(8.dp),
////        contentPadding = PaddingValues(4.dp)
////    ) {
////        // Blank days before start of month
////        items(firstDayOfWeek) {
////            Box(modifier = Modifier.size(44.dp)) {}
////        }
////        // Days
////        items(totalDays) { index ->
////            val date = startOfMonth.plusDays(index.toLong())
////            val mood = moodEntries[date]?.moodRes
////            Box(
////                modifier = Modifier
////                    .size(44.dp)
////                    .clip(RoundedCornerShape(8.dp))
////                    .background(if (date == selectedDate) LightPurple else Color.LightGray)
////                    .clickable { onDateSelected(date) },
////                contentAlignment = Alignment.Center
////            ) {
////                Column(horizontalAlignment = Alignment.CenterHorizontally) {
////                    Text(text = "${date.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
////                    if (mood != null) {
////                        AsyncImage(
////                            model = mood,
////                            contentDescription = null,
////                            modifier = Modifier.size(24.dp)
////                        )
////                    }
////                }
////            }
////        }
////    }
////}
////
////// --- Navigation & Main Controller ---
////@Composable
////fun MoodTrackerFlow() {
////    val navController = rememberNavController()
////    val moodEntries = remember { mutableStateMapOf<LocalDate, MoodEntry>() }
////
////    NavHost(navController = navController, startDestination = "mood_grid") {
////        composable("mood_grid") {
////            MoodSelectorCardGrid(
////                onMoodSelected = { moodRes ->
////                    val today = LocalDate.now()
////                    moodEntries[today] = MoodEntry(today, moodRes)
////                    navController.navigate("history")
////                }
////            )
////        }
////        composable("history") {
////            MoodHistoryScreen(
////                moodEntries = moodEntries,
////                onEntryClicked = { date ->
////                    navController.navigate("journal/${date}")
////                }
////            )
////        }
////        composable(
////            "journal/{date}",
////            arguments = listOf(navArgument("date") { type = NavType.StringType })
////        ) { backStackEntry ->
////            val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
////            val date = LocalDate.parse(dateStr)
////            MoodJournalScreen(
////                date = date,
////                moodEntry = moodEntries[date],
////                onSave = { entry ->
////                    moodEntries[date] = entry
////                    navController.popBackStack()
////                },
////                onBack = { navController.popBackStack() }
////            )
////        }
////        composable("calendar") {
////            MoodCalendarScreen(
////                moodEntries = moodEntries,
////                onDateSelected = { date ->
////                    // When date selected on calendar, open journal to view/edit
////                    navController.navigate("journal/${date}")
////                }
////            )
////        }
////    }
////}
////
////// --- Preview ---
////@Preview(showBackground = true)
////@Composable
////fun MoodTrackerFlowPreview() {
////    MoodTrackerFlow()
////}
//
//
import android.app.Application
import androidx.compose.material.icons.filled.History
//import androidx.compose.material3.icons
//import androidx.compose.material3.icons.Icons
//import androidx.compose.material3.icons.filled.ArrowBack
//import androidx.compose.material3.icons.filled.History
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.lifecycle.AndroidViewModel
import com.mato.syai.R
//
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
//
//// --- Data class for Mood Entry ---
//data class MoodEntry(
//    val moodRes: Int,
//    val journal: String
//)
//
//// --- ViewModel ---
//open class MoodViewModel(application: Application) : AndroidViewModel(application) {
//
//    // In-memory map of dateString -> MoodEntry
//    private val _moodEntries = MutableStateFlow<Map<String, MoodEntry>>(emptyMap())
//    open val moodEntries: StateFlow<Map<String, MoodEntry>> = _moodEntries.asStateFlow()
//
//    // Get MoodEntry for a date
//    fun getMoodEntry(date: String): MoodEntry? = _moodEntries.value[date]
//
//    // Save or update mood entry
//    fun saveMoodEntry(date: String, moodRes: Int, journal: String) {
//        val updatedMap = _moodEntries.value.toMutableMap()
//        updatedMap[date] = MoodEntry(moodRes, journal)
//        _moodEntries.value = updatedMap
//    }
//}
//
//// --- Composables ---
//
//@Composable
//fun MoodTrackerApp(
//    viewModel: MoodViewModel = viewModel(
//        factory = ViewModelProvider.AndroidViewModelFactory(
//            LocalContext.current.applicationContext as Application
//        )
//    )
//) {
//    val navController = rememberNavController()
//
//    NavHost(navController = navController, startDestination = "mood_grid") {
//        composable("mood_grid") {
//            MoodGridScreen(
//                viewModel = viewModel,
//                onHistoryClick = { navController.navigate("history") }
//            )
//        }
//        composable("history") {
//            HistoryScreen(
//                viewModel = viewModel,
//                onBack = { navController.popBackStack() }
//            )
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MoodGridScreen(viewModel: MoodViewModel, onHistoryClick: () -> Unit) {
//    val today = remember { LocalDate.now() }
//    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
//    val todayStr = today.format(formatter)
//
//    val moodEntries by viewModel.moodEntries.collectAsState()
//
//    var selectedMoodRes by remember { mutableStateOf<Int?>(null) }
//    var journalText by remember { mutableStateOf("") }
//    var showJournalBox by remember { mutableStateOf(false) }
//
//    // Load existing entry when mood selected
//    LaunchedEffect(selectedMoodRes) {
//        if (selectedMoodRes != null) {
//            journalText = moodEntries[todayStr]?.journal ?: ""
//            showJournalBox = true
//        }
//    }
//
//    Column(modifier = Modifier.fillMaxSize()) {
//        TopAppBar(
//            title = { Text("Select Your Mood") },
//            actions = {
//                IconButton(onClick = onHistoryClick) {
//                    Icon(Icons.Default.History, contentDescription = "History")
//                }
//            }
//        )
//        MoodGifGrid(
//            selectedMoodRes = selectedMoodRes,
//            onMoodSelected = { moodRes ->
//                selectedMoodRes = moodRes
//            }
//        )
//        if (showJournalBox) {
//            JournalInputBox(
//                journalText = journalText,
//                onTextChange = {
//                    if (it.length <= 200) journalText = it
//                },
//                onSave = {
//                    if (selectedMoodRes != null) {
//                        viewModel.saveMoodEntry(todayStr, selectedMoodRes!!, journalText)
//                        showJournalBox = false
//                        selectedMoodRes = null
//                        journalText = ""
//                    }
//                },
//                onCancel = {
//                    showJournalBox = false
//                    selectedMoodRes = null
//                    journalText = ""
//                }
//            )
//        }
//    }
//}
//
//@Composable
//fun MoodGifGrid(selectedMoodRes: Int?, onMoodSelected: (Int) -> Unit) {
//    val gifList = listOf(
//        R.drawable.happy,
//        R.drawable.sad,
//        R.drawable.abuse,
//        R.drawable.angry,
//        R.drawable.cry,
//        R.drawable.kissy,
//        R.drawable.love,
//        R.drawable.wink,
//        R.drawable.surprised,
//        R.drawable.worry,
//        R.drawable.vomit,
//        R.drawable.sick,
//        R.drawable.sleep,
//        R.drawable.party
//    )
//
//    val selectedIndex = selectedMoodRes?.let { gifList.indexOf(it) } ?: -1
//
//    LazyVerticalGrid(
//        columns = GridCells.Fixed(3),
//        modifier = Modifier
////            .weight(1f)
//            .padding(8.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        contentPadding = PaddingValues(8.dp)
//    ) {
//        items(gifList.size) { index ->
//            val isSelected = index == selectedIndex
//            Card(
//                modifier = Modifier
//                    .size(100.dp)
//                    .clickable { onMoodSelected(gifList[index]) },
//                shape = RoundedCornerShape(16.dp),
//                elevation = CardDefaults.cardElevation(8.dp)
//            ) {
//                AsyncImage(
//                    model = gifList[index],
//                    contentDescription = "Mood GIF",
//                    modifier = Modifier.background(if (isSelected) Color.LightGray else Color.Transparent)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun JournalInputBox(
//    journalText: String,
//    onTextChange: (String) -> Unit,
//    onSave: () -> Unit,
//    onCancel: () -> Unit
//) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(Color(0xFFEFEFEF))
//            .padding(8.dp)
//    ) {
//        Text(text = "Write a journal (max 200 words)", style = MaterialTheme.typography.bodyMedium)
//        OutlinedTextField(
//            value = journalText,
//            onValueChange = onTextChange,
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(120.dp),
//            maxLines = 6,
//            singleLine = false
//        )
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.End
//        ) {
//            TextButton(onClick = onCancel) {
//                Text("Cancel")
//            }
//            Spacer(Modifier.width(8.dp))
//            Button(onClick = onSave) {
//                Text("Save")
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun HistoryScreen(viewModel: MoodViewModel, onBack: () -> Unit) {
//    val moodEntries by viewModel.moodEntries.collectAsState()
//    val today = remember { LocalDate.now() }
//    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
//
//    var selectedDate by remember { mutableStateOf(today) }
//    val selectedDateStr = selectedDate.format(formatter)
//
//    val entry = moodEntries[selectedDateStr]
//
//    var journalText by remember { mutableStateOf(entry?.journal ?: "") }
//    val isEditable = selectedDate == today
//
//    Column(modifier = Modifier.fillMaxSize()) {
//        TopAppBar(
//            title = { Text("Mood History") },
//            navigationIcon = {
//                IconButton(onClick = onBack) {
//                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                }
//            }
//        )
//
//        CalendarView(
//            selectedDate = selectedDate,
//            onDateSelected = { date ->
//                selectedDate = date
//                journalText = moodEntries[date.format(formatter)]?.journal ?: ""
//            },
//            moodEntries = moodEntries.mapKeys { LocalDate.parse(it.key) }
//        )
//
//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(8.dp)
//                .background(Color(0xFFEFEFEF))
//        ) {
//            if (entry != null) {
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    AsyncImage(
//                        model = entry.moodRes,
//                        contentDescription = "Mood",
//                        modifier = Modifier.size(48.dp)
//                    )
//                    Spacer(Modifier.width(8.dp))
//                    Text("Mood on ${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}")
//                }
//            } else {
//                Text("No mood recorded for this date")
//            }
//        }
//
//        OutlinedTextField(
//            value = journalText,
//            onValueChange = {
//                if (it.length <= 200 && isEditable) journalText = it
//            },
//            enabled = isEditable,
//            label = { Text(if (isEditable) "Your journal (max 200 words)" else "Journal (read-only)") },
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(120.dp)
//                .padding(8.dp),
//            maxLines = 6,
//            singleLine = false
//        )
//
//        if (isEditable) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.End
//            ) {
//                Button(onClick = {
//                    if (entry != null) {
//                        viewModel.saveMoodEntry(selectedDateStr, entry.moodRes, journalText)
//                    }
//                }) {
//                    Text("Save")
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun CalendarView(
//    selectedDate: LocalDate,
//    onDateSelected: (LocalDate) -> Unit,
//    moodEntries: Map<LocalDate, MoodEntry>
//) {
//    val currentYearMonth = remember { YearMonth.now() }
//    val startOfMonth = currentYearMonth.atDay(1)
//    val totalDays = currentYearMonth.lengthOfMonth()
//    val firstDayOfWeek = startOfMonth.dayOfWeek.value % 7
//
//    LazyVerticalGrid(
//        columns = GridCells.Fixed(7),
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(320.dp)
//            .padding(8.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        contentPadding = PaddingValues(4.dp)
//    ) {
//        items(firstDayOfWeek) {
//            Box(Modifier.size(44.dp))
//        }
//        items(totalDays) { index ->
//            val date = startOfMonth.plusDays(index.toLong())
//            val moodEntry = moodEntries[date]
//            val isSelected = date == selectedDate
//
//            Box(
//                modifier = Modifier
//                    .size(44.dp)
//                    .background(if (isSelected) Color(0xFFCE93D8) else Color.LightGray)
//                    .clickable { onDateSelected(date) },
//                contentAlignment = Alignment.Center
//            ) {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    Text(text = "${date.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
//                    if (moodEntry != null) {
//                        AsyncImage(
//                            model = moodEntry.moodRes,
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
////@Suppress("ViewModelConstructor")
////@Preview(showBackground = true)
////@Composable
////fun PreviewMoodGridScreen() {
////    MaterialTheme {
////        MoodGridScreen(
////            viewModel = PreviewMoodViewModel(),
////            onHistoryClick = {}
////        )
////    }
////}
//////
////@Suppress("ViewModelConstructor")
////class PreviewMoodViewModel : MoodViewModel(Application()) {
////    override val moodEntries = MutableStateFlow(
////        mapOf(
////            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) to
////                    MoodEntry(moodRes = R.drawable.happy, journal = "Feeling great!")
////        )
////    )
////}



// Imports skipped for brevity — remember to include androidx.room, navigation, compose, coil etc.

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey val date: String, // Format: "yyyy-MM-dd"
    val moodRes: Int,
    val journal: String
)

@Dao
interface MoodEntryDao {
    @Query("SELECT * FROM mood_entries WHERE date = :date")
    suspend fun getMoodEntry(date: String): MoodEntryEntity?

    @Query("SELECT * FROM mood_entries")
    suspend fun getAllEntries(): List<MoodEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: MoodEntryEntity)
}

@Database(entities = [MoodEntryEntity::class], version = 1)
abstract class MoodDatabase : RoomDatabase() {
    abstract fun moodEntryDao(): MoodEntryDao
}

// --- ViewModel ---
class MoodViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application, MoodDatabase::class.java, "mood_db"
    ).build()

    private val dao = db.moodEntryDao()

    private val _allEntries = MutableStateFlow<List<MoodEntryEntity>>(emptyList())
    val allEntries: StateFlow<List<MoodEntryEntity>> = _allEntries

    init {
        viewModelScope.launch {
            _allEntries.value = dao.getAllEntries()
        }
    }

    suspend fun getMoodEntry(date: String): MoodEntryEntity? = dao.getMoodEntry(date)

    fun saveMoodEntry(date: String, moodRes: Int, journal: String) {
        viewModelScope.launch {
            dao.insertMoodEntry(MoodEntryEntity(date, moodRes, journal))
            _allEntries.value = dao.getAllEntries()
        }
    }
}

// --- Screens + UI ---

@Composable
fun MoodTrackerApp(viewModel: MoodViewModel = viewModel(factory = ViewModelProvider.AndroidViewModelFactory(LocalContext.current.applicationContext as Application))) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "mood_grid") {
        composable("mood_grid") {
            MoodGridScreen(
                viewModel = viewModel,
                onHistoryClick = { navController.navigate("history") }
            )
        }
        composable("history") {
            HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodGridScreen(viewModel: MoodViewModel, onHistoryClick: () -> Unit) {
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }
    val todayStr = today.format(formatter)

    // Load all entries as state
    val allEntries by viewModel.allEntries.collectAsState()

    // State: selected mood resource, journal text, and whether journal box is visible
    var selectedMoodRes by remember { mutableStateOf<Int?>(null) }
    var journalText by remember { mutableStateOf("") }
    var showJournalBox by remember { mutableStateOf(false) }

    // If existing mood entry for today, prefill
    LaunchedEffect(selectedMoodRes) {
        if (selectedMoodRes != null) {
            val entry = allEntries.find { it.date == todayStr }
            journalText = entry?.journal ?: ""
            showJournalBox = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
            }
        )
        if (showJournalBox) {
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

@Composable
fun MoodGifGrid(selectedMoodRes: Int?, onMoodSelected: (Int) -> Unit) {
    val context = LocalContext.current
    val gifList = listOf(
        R.drawable.happy,
        R.drawable.sad,
        R.drawable.abuse,
        R.drawable.angry,
        R.drawable.cry,
        R.drawable.kissy,
        R.drawable.love,
        R.drawable.wink,
        R.drawable.surprised,
        R.drawable.worry,
        R.drawable.vomit,
        R.drawable.sick,
        R.drawable.sleep,
        R.drawable.party
    )

    val selectedIndex = selectedMoodRes?.let { gifList.indexOf(it) } ?: -1

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
//            .weight(1f)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(gifList.size) { index ->
            val isSelected = index == selectedIndex
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .clickable { onMoodSelected(gifList[index]) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                AsyncImage(
                    model = gifList[index],
                    contentDescription = "Mood GIF",
                    modifier = Modifier
                        .background(if (isSelected) Color.LightGray else Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun JournalInputBox(
    journalText: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFEFEF))
            .padding(8.dp)
    ) {
        Text(text = "Write a journal (max 200 words)", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = journalText,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 6,
            singleLine = false
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSave) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MoodViewModel, onBack: () -> Unit) {
    val allEntries by viewModel.allEntries.collectAsState()
    val today = remember { LocalDate.now() }
    val formatter = remember { DateTimeFormatter.ISO_LOCAL_DATE }

    var selectedDate by remember { mutableStateOf(today) }
    val selectedDateStr = selectedDate.format(formatter)

    // Find mood entry for selected date
    val entry = allEntries.find { it.date == selectedDateStr }

    // Journal text and editability state
    var journalText by remember { mutableStateOf(entry?.journal ?: "") }
    val isEditable = selectedDate == today

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mood History") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        CalendarView(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                // Reset journal text to selected date's journal
                journalText = allEntries.find { it.date == date.format(formatter) }?.journal ?: ""
            },
            moodEntries = allEntries.associateBy { LocalDate.parse(it.date) }
        )

        // Mood Display + Journal Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color(0xFFEFEFEF))
        ) {
            if (entry != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = entry.moodRes,
                        contentDescription = "Mood",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Mood on ${selectedDate.dayOfMonth} ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }}")
                }
            } else {
                Text("No mood recorded for this date")
            }
        }

        OutlinedTextField(
            value = journalText,
            onValueChange = {
                if (it.length <= 200 && isEditable) journalText = it
            },
            enabled = isEditable,
            label = { Text(if (isEditable) "Your journal (max 200 words)" else "Journal (read-only)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(8.dp),
            maxLines = 6,
            singleLine = false
        )

        if (isEditable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = {
                    if (entry != null) {
                        viewModel.saveMoodEntry(selectedDateStr, entry.moodRes, journalText)
                    }
                }) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    moodEntries: Map<LocalDate, MoodEntryEntity>
) {
    val currentYearMonth = remember { YearMonth.now() }
    val startOfMonth = currentYearMonth.atDay(1)
    val totalDays = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = startOfMonth.dayOfWeek.value % 7

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        // Blank days
        items(firstDayOfWeek) {
            Box(Modifier.size(44.dp))
        }
        items(totalDays) { index ->
            val date = startOfMonth.plusDays(index.toLong())
            val moodEntry = moodEntries[date]
            val isSelected = date == selectedDate

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) LightPurple else Color.LightGray)
                    .clickable { onDateSelected(date) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${date.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
                    if (moodEntry != null) {
                        AsyncImage(
                            model = moodEntry.moodRes,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
