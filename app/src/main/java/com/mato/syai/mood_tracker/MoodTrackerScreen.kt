package com.mato.syai.mood_tracker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.mato.syai.ui.theme.LightPurple
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import androidx.navigation.compose.composable
import android.app.Application
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.lifecycle.AndroidViewModel
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
import com.mato.syai.R
import com.mato.syai.ui.theme.PurpleDark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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

// --- Journal Input Box with Word Count ---

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
        OutlinedTextField(
            value = journalText,
            onValueChange = {
                if (it.length <= 200) onTextChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 6,
            singleLine = false,
            placeholder = { Text("Write your journal...") }
        )
        Text(
            text = "${journalText.length}/200",
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 12.dp, top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
//            label = { Text(if (isEditable) "Your journal (max 200 words)" else "Journal (read-only)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(8.dp),
            maxLines = 6,
            singleLine = false
        )
        Text(
            text = "${journalText.length}/200",
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 12.dp, top = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
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

//@Composable
//fun CalendarView(
//    selectedDate: LocalDate,
//    onDateSelected: (LocalDate) -> Unit,
//    moodEntries: Map<LocalDate, MoodEntryEntity>
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
//        // Blank days
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
//                    .clip(RoundedCornerShape(8.dp))
//                    .background(if (isSelected) LightPurple else Color.LightGray)
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

// --- CalendarView with Month-Year Navigation ---

@Composable
fun CalendarView(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    moodEntries: Map<LocalDate, MoodEntryEntity>
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val startOfMonth = currentYearMonth.atDay(1)
    val totalDays = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = startOfMonth.dayOfWeek.value % 7

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                currentYearMonth = currentYearMonth.minusMonths(1)
            }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
            }

            Text(
                text = "${currentYearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentYearMonth.year}",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = {
                currentYearMonth = currentYearMonth.plusMonths(1)
            }) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
            }
        }

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
            val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            items(daysOfWeek.size) { index ->
                Text(
                    text = daysOfWeek[index],
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                )
            }

            // Blank days
            items(firstDayOfWeek) {
                Box(Modifier.size(44.dp))
            }

            // Calendar days
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
}


@Composable
fun MoodBurstEffect(show: Boolean, moodRes: Int?, onComplete: () -> Unit) {
    if (!show) return

    val particleCount = 25
    val moodColor = when (moodRes) {
        R.drawable.happy -> PurpleDark
        R.drawable.sad -> LightPurple
        R.drawable.angry -> Color.Red
        R.drawable.love -> Color.Magenta
        R.drawable.sleep -> Color.Cyan
        R.drawable.party -> Color.Green
        R.drawable.cry -> Color.Blue
        R.drawable.kissy -> LightPurple
        R.drawable.love -> Color.Red
        R.drawable.wink -> Color.Blue
        else -> PurpleDark
    }

    val particles = remember {
        List(particleCount) {
            Particle(
                angle = Random.nextDouble(0.0, 2 * Math.PI),
                speed = Random.nextDouble(200.0, 600.0),
                color = moodColor.copy(alpha = Random.nextFloat())
            )
        }
    }

    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        onComplete()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        particles.forEach { particle ->
            val distance = (particle.speed * animationProgress.value).toFloat()
            val x = center.x + (cos(particle.angle) * distance).toFloat()
            val y = center.y + (sin(particle.angle) * distance).toFloat()
            drawCircle(color = particle.color, radius = 8f, center = Offset(x, y))
        }
    }
}

data class Particle(
    val angle: Double,
    val speed: Double,
    val color: Color
)