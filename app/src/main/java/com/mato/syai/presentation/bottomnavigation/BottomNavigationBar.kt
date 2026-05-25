package com.mato.syai.presentation.bottomnavigation

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mato.syai.ui.theme.WhitePurple
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun BottomFabGroup(navController: NavController,parentNavController: NavController) {
    val isMenuExtended = remember { mutableStateOf(false) }

    val fabAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
    )

    val clickAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    val renderEffect = getRenderEffect().asComposeRenderEffect()
        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {

            if (fabAnimationProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { isMenuExtended.value = false }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .graphicsLayer {
                        shadowElevation = 12f
                        shape = BottomBarCutoutShape()
                        clip = true
                    }
            ) {
                CustomBottomNavigation(navController = navController)
            }
            Box(modifier = Modifier.padding(bottom = 12.dp)) {
                FabGroup(
                    animationProgress = fabAnimationProgress,
                    renderEffect = renderEffect,
                    navController = navController,
                    parentNavController = parentNavController
                    )
                FabGroup(
                    renderEffect = null,
                    animationProgress = fabAnimationProgress,
                    toggleAnimation = { isMenuExtended.value = !isMenuExtended.value },
                    navController = navController,
                    parentNavController = parentNavController
                )
            }

            Box(modifier = Modifier.padding(bottom = 16.dp)) {

                Circle(color = MaterialTheme.colorScheme.primary, animationProgress = clickAnimationProgress)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

@Composable
fun FabGroup(
    animationProgress: Float = 0f,
    renderEffect: androidx.compose.ui.graphics.RenderEffect? = null,
    toggleAnimation: () -> Unit = {},
    navController: NavController,
    parentNavController: NavController
) {
    val context = LocalContext.current

    Box(
        Modifier.fillMaxSize().graphicsLayer { this.renderEffect = renderEffect },
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedFab(
            icon = Icons.Default.ShoppingCart,
            modifier = Modifier.padding(PaddingValues(bottom = 72.dp, end = 210.dp) * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress)),
            opacity = LinearEasing.transform(0.2f, 0.7f, animationProgress),
            onClick = {
//                Toast.makeText(context, "Premium Features Coming Soon", Toast.LENGTH_SHORT).show()
                navController.navigate("premium")
                toggleAnimation()
            }
        )

        AnimatedFab(
            icon = Icons.Filled.Person,
            modifier = Modifier.padding(PaddingValues(bottom = 88.dp) * FastOutSlowInEasing.transform(0.1f, 0.9f, animationProgress)),
            opacity = LinearEasing.transform(0.3f, 0.8f, animationProgress),
            onClick = {
//                Toast.makeText(context, "Profile View Will be available in future update", Toast.LENGTH_SHORT).show()
                navController.navigate("profile")
                toggleAnimation()
            }
        )

        AnimatedFab(
            icon = Icons.Default.Settings,
            modifier = Modifier.padding(PaddingValues(bottom = 72.dp, start = 210.dp) * FastOutSlowInEasing.transform(0.2f, 1.0f, animationProgress)),
            opacity = LinearEasing.transform(0.4f, 0.9f, animationProgress),
            onClick = {
//                Toast.makeText(context, "Settings will be available in future update", Toast.LENGTH_SHORT).show()
                parentNavController.navigate("settings")
                toggleAnimation()
            }
        )

        AnimatedFab(
            icon = Icons.Default.Add,
            modifier = Modifier
                .padding(bottom = 10.dp)
                .rotate(225 * FastOutSlowInEasing.transform(0.35f, 0.65f, animationProgress)),
            backgroundColor = Color.Transparent,
            onClick = toggleAnimation
        )
    }
}

@Composable
fun AnimatedFab(
    modifier: Modifier,
    icon: ImageVector? = null,
    opacity: Float = 1f,
    backgroundColor: Color = WhitePurple,
    onClick: () -> Unit = {}
) {
    FloatingActionButton(
        onClick = onClick,
        elevation = FloatingActionButtonDefaults.elevation(0.dp),
        containerColor = backgroundColor,
        modifier = modifier.scale(1.25f),
        shape = CircleShape
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Color.White.copy(alpha = opacity)
            )
        }
    }
}

@Composable
fun Circle(color: Color, animationProgress: Float) {
    val animationValue = sin(PI * animationProgress).toFloat()
    Box(
        modifier = Modifier.size(56.dp)
            .scale(2 - animationValue)
            .border(width = 2.dp, color = color.copy(alpha = color.alpha * animationValue), shape = CircleShape)
    )
}

fun Easing.transform(from: Float, to: Float, value: Float): Float {
    return transform(((value - from) * (1f / (to - from))).coerceIn(0f, 1f))
}

operator fun PaddingValues.times(value: Float): PaddingValues = PaddingValues(
    top = calculateTopPadding() * value,
    bottom = calculateBottomPadding() * value,
    start = calculateStartPadding(LayoutDirection.Ltr) * value,
    end = calculateEndPadding(LayoutDirection.Ltr) * value
)

@RequiresApi(Build.VERSION_CODES.S)
fun getRenderEffect(): RenderEffect {
    val blurEffect = RenderEffect.createBlurEffect(80f, 80f, Shader.TileMode.MIRROR)
    val alphaMatrix = RenderEffect.createColorFilterEffect(
        ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f, 0f,
                    0f, 0f, 1f, 0f, 0f,
                    0f, 0f, 0f, 50f, -5000f
                )
            )
        )
    )
    return RenderEffect.createChainEffect(alphaMatrix, blurEffect)
}