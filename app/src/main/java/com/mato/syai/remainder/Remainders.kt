package com.mato.syai.remainder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

enum class ReminderCategory(val displayName: String, val icon: ImageVector, val color: Color) {
    BIRTHDAY("Birthday", Icons.Default.Cake, Color(0xFFFF6B9D)),
    ANNIVERSARY("Anniversary", Icons.Default.Favorite, Color(0xFFE91E63)),
    MEETING("Meeting", Icons.Default.Business, Color(0xFF2196F3)),
    CUSTOM("Custom", Icons.Default.Event, Color(0xFF4CAF50))
}

enum class RepeatMode(val displayName: String) {
    ONCE("Once"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: ReminderCategory,
    val dateTime: LocalDateTime,
    val enableSnooze: Boolean,
    val snoozeDuration: Int, // in minutes
    val reminderBefore: Int, // minutes before event
    val alarmTone: String,
    val repeatMode: RepeatMode,
    val isActive: Boolean = true
)

// Main Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderAlarmScreen() {
    var reminders by remember { mutableStateOf(listOf<ReminderItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToDelete by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ---------- Main Content ----------
        if (reminders.isEmpty()) {
            EmptyState(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            ReminderList(
                reminders = reminders,
                onToggleActive = { id ->
                    reminders = reminders.map {
                        if (it.id == id) it.copy(isActive = !it.isActive) else it
                    }
                },
                onDeleteRequest = { id ->
                    reminderToDelete = id
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ---------- FAB ----------
        FloatingActionButton(
            onClick = { showAddDialog = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 100.dp   // your custom offset
                )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Reminder")
        }
    }

    // ---------- Dialogs ----------
    if (showAddDialog) {
        AddReminderDialog(
            onDismiss = { showAddDialog = false },
            onSave = { reminder ->
                reminders = reminders + reminder
                showAddDialog = false
            }
        )
    }

    if (reminderToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                reminders = reminders.filter { it.id != reminderToDelete }
                reminderToDelete = null
            },
            onDismiss = { reminderToDelete = null }
        )
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No reminders yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                "Tap + to add your first reminder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun ReminderList(
    reminders: List<ReminderItem>,
    onToggleActive: (String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reminders, key = { it.id }) { reminder ->
            ReminderCard(
                reminder = reminder,
                onToggleActive = { onToggleActive(reminder.id) },
                onDeleteRequest = { onDeleteRequest(reminder.id) }
            )
        }
    }
}

@Composable
fun ReminderCard(
    reminder: ReminderItem,
    onToggleActive: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        reminder.category.color.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    reminder.category.icon,
                    contentDescription = null,
                    tint = reminder.category.color
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reminder.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    reminder.category.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        reminder.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - hh:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (reminder.reminderBefore > 0) {
                    Text(
                        "Alert ${reminder.reminderBefore} min before",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (reminder.repeatMode != RepeatMode.ONCE) {
                    Text(
                        "Repeats: ${reminder.repeatMode.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Controls
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = { onToggleActive() }
                )
                IconButton(onClick = onDeleteRequest) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onSave: (ReminderItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ReminderCategory.CUSTOM) }
    var selectedDate by remember { mutableStateOf(LocalDateTime.now().plusHours(1).toLocalDate()) }
    var selectedTime by remember { mutableStateOf(LocalDateTime.now().plusHours(1).toLocalTime()) }
    var enableSnooze by remember { mutableStateOf(false) }
    var snoozeDuration by remember { mutableStateOf(5) }
    var reminderBefore by remember { mutableStateOf(0) }
    var selectedTone by remember { mutableStateOf("Default Tone") }
    var selectedRepeatMode by remember { mutableStateOf(RepeatMode.ONCE) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ReminderCategory.values().toList()) { category ->
                            CategoryChip(
                                category = category,
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                        }
                    }
                }

                item {
                    Text("Date & Time", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(selectedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a")))
                        }
                    }
                }

                item {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedRepeatMode.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Repeat") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            RepeatMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName) },
                                    onClick = {
                                        selectedRepeatMode = mode
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Snooze")
                        Switch(
                            checked = enableSnooze,
                            onCheckedChange = { enableSnooze = it }
                        )
                    }
                }

                if (enableSnooze) {
                    item {
                        Text("Snooze Duration: $snoozeDuration min")
                        Slider(
                            value = snoozeDuration.toFloat(),
                            onValueChange = { snoozeDuration = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 29
                        )
                    }
                }

                item {
                    Text("Reminder Before Event: ${if (reminderBefore == 0) "None" else "$reminderBefore min"}")
                    Slider(
                        value = reminderBefore.toFloat(),
                        onValueChange = { reminderBefore = it.toInt() },
                        valueRange = 0f..60f,
                        steps = 12
                    )
                }

                item {
                    var expanded by remember { mutableStateOf(false) }
                    val tones = listOf("Default Tone", "Gentle Alarm", "Loud Alarm", "Melody", "Beep")

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTone,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Alarm Tone") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            tones.forEach { tone ->
                                DropdownMenuItem(
                                    text = { Text(tone) },
                                    onClick = {
                                        selectedTone = tone
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val dateTime = LocalDateTime.of(selectedDate, selectedTime)
                        onSave(
                            ReminderItem(
                                title = title,
                                category = selectedCategory,
                                dateTime = dateTime,
                                enableSnooze = enableSnooze,
                                snoozeDuration = snoozeDuration,
                                reminderBefore = reminderBefore,
                                alarmTone = selectedTone,
                                repeatMode = selectedRepeatMode
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
fun CategoryChip(
    category: ReminderCategory,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) category.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (selected) null else ButtonDefaults.outlinedButtonBorder,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                category.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) category.color else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                category.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) category.color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Delete Reminder?") },
        text = { Text("Are you sure you want to delete this reminder? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}