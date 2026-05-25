package com.mato.syai.subscriptions.presentation

//package com.example.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.mato.syai.profile.AccentPink
import com.mato.syai.profile.AccentViolet
import com.mato.syai.profile.GlassBorder
import com.mato.syai.profile.GlassCard
import com.mato.syai.profile.TextPrimary
import com.mato.syai.profile.TextSecondary
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import com.mato.syai.profile.BlobBackground
import com.mato.syai.profile.DeepPurpleBg

// ─── Re-use colors from ProfileScreen ──────────────────────────────────────────
// DeepPurpleBg, GlassCard, GlassBorder, AccentViolet, AccentCyan, AccentPink,
// TextPrimary, TextSecondary  — all defined in ProfileScreen.kt

// ─── Plan data model ────────────────────────────────────────────────────────────
enum class Plan { FREE, PREMIUM, SUPER }

data class PlanInfo(
    val plan: Plan,
    val name: String,
    val price: String,
    val period: String,
    val tagline: String,
    val features: List<String>,
    val gradient: List<Color>,
    val accentColor: Color,
    val icon: ImageVector,
    val badge: String? = null
)

val plans = listOf(
    PlanInfo(
        plan = Plan.FREE,
        name = "Free",
        price = "₹0",
        period = "forever",
        tagline = "Get started for free",
        features = listOf(
            "5 notes per month",
            "Basic formatting",
            "1 device sync",
            "Community support"
        ),
        gradient = listOf(Color(0xFF2A2A3E), Color(0xFF1E1E30)),
        accentColor = Color(0xFF9E9E9E),
        icon = Icons.Outlined.StarOutline,
        badge = null
    ),
    PlanInfo(
        plan = Plan.PREMIUM,
        name = "Premium",
        price = "₹299",
        period = "/ month",
        tagline = "Most popular choice",
        features = listOf(
            "Unlimited notes",
            "Rich text & media",
            "5 device sync",
            "Cloud backup",
            "Priority support"
        ),
        gradient = listOf(Color(0xFF4A1D96), Color(0xFF7C3AED)),
        accentColor = AccentViolet,
        icon = Icons.Filled.Star,
        badge = "POPULAR"
    ),
    PlanInfo(
        plan = Plan.SUPER,
        name = "Super",
        price = "₹799",
        period = "/ month",
        tagline = "Unlock everything",
        features = listOf(
            "Everything in Premium",
            "Unlimited devices",
            "AI-powered features",
            "Team collaboration",
            "Custom themes",
            "Dedicated support"
        ),
        gradient = listOf(Color(0xFF831843), Color(0xFFBE185D)),
        accentColor = AccentPink,
        icon = Icons.Filled.AutoAwesome,
        badge = "BEST VALUE"
    )
)

// ─── Current Plan Banner ─────────────────────────────────────────────────────────
@Composable
fun CurrentPlanBanner(current: Plan) {
    val info = plans.first { it.plan == current }

    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(info.gradient))
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(info.accentColor.copy(alpha = 0.8f), Color.Transparent, info.accentColor.copy(alpha = 0.4f))
                ),
                RoundedCornerShape(24.dp)
            )
    ) {
        // Shimmer sweep
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent),
                        start = Offset(shimmer * 800f, 0f),
                        end = Offset(shimmer * 800f + 300f, 400f)
                    )
                )
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(info.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Current Plan", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, letterSpacing = 1.sp)
                    Text(info.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(info.price, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text(info.period, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.8.dp)

            Spacer(Modifier.height(14.dp))

            // Renewal info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Renews on 10 May 2026", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }
}

// ─── Plan Card ───────────────────────────────────────────────────────────────────
@Composable
fun PlanCard(
    info: PlanInfo,
    isCurrent: Boolean,
    onUpgrade: () -> Unit,
    onManage: () -> Unit,
    onCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(isCurrent) }

    val borderAlpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.35f,
        animationSpec = tween(400), label = "border"
    )
    val elevation by animateDpAsState(
        targetValue = if (isCurrent) 8.dp else 0.dp,
        animationSpec = tween(400), label = "elev"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassCard)
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        info.accentColor.copy(alpha = borderAlpha),
                        info.accentColor.copy(alpha = borderAlpha * 0.4f)
                    )
                ),
                RoundedCornerShape(20.dp)
            )
            .clickable { expanded = !expanded }
    ) {
        // ── Header row ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        info.gradient.map { it.copy(alpha = if (isCurrent) 1f else 0.6f) }
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(info.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(info.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    if (info.badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(info.badge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp)
                        }
                    }
                }
                Text(info.tagline, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(info.price, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(info.period, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
            }
        }

        // ── Expandable features + actions ──────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(350)) + fadeIn(tween(350)),
            exit = shrinkVertically(tween(300)) + fadeOut(tween(300))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Features
                info.features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(info.accentColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = info.accentColor, modifier = Modifier.size(12.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(feature, color = TextPrimary, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = GlassBorder, thickness = 0.8.dp)
                Spacer(Modifier.height(14.dp))

                // Action buttons
                if (isCurrent) {
                    // Manage + Cancel row
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Manage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(info.accentColor.copy(alpha = 0.18f))
                                .border(1.dp, info.accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onManage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Settings, contentDescription = null, tint = info.accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Manage", color = info.accentColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Cancel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF1744).copy(alpha = 0.10f))
                                .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { onCancel() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Cancel, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cancel", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    // Upgrade button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(info.gradient))
                            .clickable { onUpgrade() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Upgrade, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Upgrade to ${info.name}",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Collapsed tap hint
        if (!expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tap to see features", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─── Cancel Dialog ───────────────────────────────────────────────────────────────
@Composable
fun CancelDialog(planName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1530),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Cancel $planName?", color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        },
        text = {
            Text(
                "You'll lose access to all $planName features at the end of your billing cycle.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF1744).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .clickable { onConfirm() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Yes, Cancel", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Keep Plan", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ─── Upgrade Dialog ──────────────────────────────────────────────────────────────
@Composable
fun UpgradeDialog(info: PlanInfo, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1530),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(info.icon, contentDescription = null, tint = info.accentColor, modifier = Modifier.size(32.dp))
        },
        title = {
            Text("Upgrade to ${info.name}", color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        },
        text = {
            Text(
                "You'll be charged ${info.price}${info.period}. Cancel anytime.",
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(info.gradient))
                    .clickable { onConfirm() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Confirm Upgrade", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassCard)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Not Now", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ─── MAIN SCREEN ────────────────────────────────────────────────────────────────
@Composable
fun SubscriptionScreen(onBack: () -> Unit = {}) {
    val scrollState = rememberScrollState()

    // Current plan state
    var currentPlan by remember { mutableStateOf(Plan.PREMIUM) }

    // Dialog state
    var showCancelDialog by remember { mutableStateOf(false) }
    var showUpgradeTarget by remember { mutableStateOf<Plan?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(DeepPurpleBg)) {
        BlobBackground()
        Box(modifier = Modifier.fillMaxSize().background(DeepPurpleBg.copy(alpha = 0.58f)))

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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
                Column {
                    Text("Subscription", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("Manage your plan", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.CreditCard, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(24.dp))
            }

            // ── Content ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Current plan banner
                CurrentPlanBanner(currentPlan)

                // Section label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("All Plans", color = TextSecondary, fontSize = 12.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(10.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = GlassBorder, thickness = 0.8.dp)
                }

                // Plan cards
                plans.forEach { info ->
                    PlanCard(
                        info = info,
                        isCurrent = info.plan == currentPlan,
                        onUpgrade = { showUpgradeTarget = info.plan },
                        onManage = { /* open manage flow */ },
                        onCancel = { showCancelDialog = true }
                    )
                }

                // Footnote
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Payments secured by Razorpay", color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Cancel anytime · No hidden fees", color = TextSecondary.copy(alpha = 0.6f), fontSize = 10.sp)
                }
            }
        }

        // ── Dialogs ──────────────────────────────────────────────────────
        if (showCancelDialog) {
            val planInfo = plans.first { it.plan == currentPlan }
            CancelDialog(
                planName = planInfo.name,
                onConfirm = {
                    currentPlan = Plan.FREE
                    showCancelDialog = false
                },
                onDismiss = { showCancelDialog = false }
            )
        }

        showUpgradeTarget?.let { target ->
            val planInfo = plans.first { it.plan == target }
            UpgradeDialog(
                info = planInfo,
                onConfirm = {
                    currentPlan = target
                    showUpgradeTarget = null
                },
                onDismiss = { showUpgradeTarget = null }
            )
        }
    }
}

@Preview(
    name = "Subscription Screen - Light",
    showBackground = true,
    backgroundColor = 0xFF0B0A14,
    widthDp = 412,
    heightDp = 900
)
@Composable
fun SubscriptionScreenPreview() {
    MaterialTheme {
        SubscriptionScreen()
    }
}

@Preview(
    name = "Subscription Screen - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    backgroundColor = 0xFF0B0A14,
    widthDp = 412,
    heightDp = 900
)
@Composable
fun SubscriptionScreenDarkPreview() {
    MaterialTheme {
        SubscriptionScreen()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0A14)
@Composable
fun UpgradeDialogPreview() {
    MaterialTheme {
        UpgradeDialog(
            info = plans[2],
            onConfirm = {},
            onDismiss = {}
        )
    }
}
