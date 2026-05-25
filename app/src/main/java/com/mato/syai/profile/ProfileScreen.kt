package com.mato.syai.profile

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import java.util.Calendar
import android.content.res.Configuration
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

// ─── Color Palette (matching reference) ────────────────────────────────────────
val DeepPurpleBg    = Color(0xFF0D0A1E)
val GlassCard       = Color(0x1AFFFFFF)
val GlassBorder     = Color(0x26FFFFFF)
val AccentViolet    = Color(0xFF7C4DFF)
val AccentCyan      = Color(0xFF00E5FF)
val AccentPink      = Color(0xFFE040FB)
val TextPrimary     = Color(0xFFEEEEEE)
val TextSecondary   = Color(0xFFAAAAAA)
val ToggleActive    = Color(0xFF7C4DFF)

// ─── Blob Background ────────────────────────────────────────────────────────────
@Composable
fun BlobBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "blob")
    val shift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "blobShift"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Blob 1 – Cyan top-center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentCyan.copy(alpha = 0.35f), Color.Transparent),
                center = Offset(size.width * 0.45f, size.height * (0.05f + shift * 0.05f)),
                radius = size.width * 0.42f
            )
        )
        // Blob 2 – Pink left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentPink.copy(alpha = 0.30f), Color.Transparent),
                center = Offset(size.width * (0.05f - shift * 0.02f), size.height * 0.55f),
                radius = size.width * 0.45f
            )
        )
        // Blob 3 – Violet bottom-center
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccentViolet.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width * 0.55f, size.height * (0.88f - shift * 0.04f)),
                radius = size.width * 0.38f
            )
        )
    }
}

// ─── Glass Card ─────────────────────────────────────────────────────────────────
@Composable
fun GlassSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 8.dp),
        content = content
    )
}

// ─── Profile Field ──────────────────────────────────────────────────────────────
@Composable
fun ProfileField(
    label: String,
    value: String,
    icon: ImageVector,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    trailingContent: (@Composable () -> Unit)? = null
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AccentViolet.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(14.dp))

        // Label + TextField
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("Enter $label", color = TextSecondary.copy(alpha = 0.5f), fontSize = 15.sp)
                    }
                    inner()
                }
            )
        }

        trailingContent?.invoke()
    }
}

// ─── Divider ────────────────────────────────────────────────────────────────────
@Composable
fun GlassDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = GlassBorder,
        thickness = 0.8.dp
    )
}

// ─── Gender Picker Row ──────────────────────────────────────────────────────────
@Composable
fun GenderSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("Male", "Female", "Other")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(AccentViolet.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Gender", color = TextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isSelected = selected == option
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AccentViolet else GlassCard)
                            .border(1.dp, if (isSelected) AccentViolet else GlassBorder, RoundedCornerShape(20.dp))
                            .clickable { onSelect(option) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            option,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

// ─── Avatar Section ──────────────────────────────────────────────────────────────
@Composable
fun ProfileAvatar(name: String) {
    val initials = name.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .take(2).joinToString("")
        .ifEmpty { "?" }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AccentViolet, AccentCyan),
                            start = Offset(0f, 0f),
                            end = Offset(90f, 90f)
                        )
                    )
                    .border(2.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }

            // Edit badge
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(AccentPink)
                    .border(2.dp, DeepPurpleBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit photo", tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            name.ifEmpty { "Your Name" },
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text("Edit Profile", color = AccentViolet, fontSize = 12.sp)
    }
}

// ─── DOB Picker ─────────────────────────────────────────────────────────────────
fun showDatePicker(context: Context, current: String, onPick: (String) -> Unit) {
    val cal = Calendar.getInstance()
    val parts = current.split("/")
    if (parts.size == 3) {
        runCatching {
            cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
        }
    }
    DatePickerDialog(
        context,
        { _, y, m, d -> onPick("%02d/%02d/%04d".format(d, m + 1, y)) },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

// ─── MAIN SCREEN ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State
    var name    by remember { mutableStateOf("") }
    var phone   by remember { mutableStateOf("") }
    var email   by remember { mutableStateOf("") }
    var state   by remember { mutableStateOf("") }
    var city    by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var gender  by remember { mutableStateOf("") }
    var dob     by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(DeepPurpleBg)) {
        // Animated blob background
        BlobBackground()

        // Frosted overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepPurpleBg.copy(alpha = 0.55f))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GlassCard)
                        .border(1.dp, GlassBorder, CircleShape)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(18.dp))
                }

                Spacer(Modifier.width(14.dp))

                Text(
                    "Profile",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // ── Scrollable Body ──────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                // Avatar
                ProfileAvatar(name)

                Spacer(Modifier.height(24.dp))

                // ─── Personal Info Card ──────────────────────────────────
                Text(
                    "Personal Info",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
                )

                GlassSection {
                    ProfileField(
                        label = "Full Name",
                        value = name,
                        icon = Icons.Outlined.Person,
                        onValueChange = { name = it }
                    )
                    GlassDivider()
                    ProfileField(
                        label = "Phone Number",
                        value = phone,
                        icon = Icons.Outlined.Phone,
                        onValueChange = { phone = it }
                    )
                    GlassDivider()
                    ProfileField(
                        label = "Email Address",
                        value = email,
                        icon = Icons.Outlined.Email,
                        onValueChange = { email = it }
                    )
                    GlassDivider()

                    // DOB – tappable
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker(context, dob) { dob = it } }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentViolet.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.DateRange, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Date of Birth", color = TextSecondary, fontSize = 11.sp, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                dob.ifEmpty { "DD / MM / YYYY" },
                                color = if (dob.isEmpty()) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                    }

                    GlassDivider()

                    // Gender
                    GenderSelector(selected = gender, onSelect = { gender = it })
                }

                Spacer(Modifier.height(20.dp))

                // ─── Location Card ───────────────────────────────────────
                Text(
                    "Location",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
                )

                GlassSection {
                    ProfileField(
                        label = "City",
                        value = city,
                        icon = Icons.Outlined.LocationOn,
                        onValueChange = { city = it }
                    )
                    GlassDivider()
                    ProfileField(
                        label = "State / Province",
                        value = state,
                        icon = Icons.Outlined.Place,
                        onValueChange = { state = it }
                    )
                    GlassDivider()
                    ProfileField(
                        label = "Country",
                        value = country,
                        icon = Icons.Outlined.Public,
                        onValueChange = { country = it }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // ─── Save Button ─────────────────────────────────────────
                Button(
                    onClick = { /* handle save */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AccentViolet, AccentPink),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.MAX_VALUE, 0f)
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Changes", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
            }
        }
    }
}


@Preview(
    name = "Profile Screen - Light",
    showBackground = true,
    backgroundColor = 0xFF0D0A1E,
    widthDp = 412,
    heightDp = 915
)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreen()
    }
}

@Preview(
    name = "Profile Screen - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0D0A1E,
    widthDp = 412,
    heightDp = 915
)
@Composable
fun ProfileScreenDarkPreview() {
    MaterialTheme {
        ProfileScreen()
    }
}