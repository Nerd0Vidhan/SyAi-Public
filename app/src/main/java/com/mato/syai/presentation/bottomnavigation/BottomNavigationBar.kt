package com.mato.syai.presentation.bottomnavigation

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.LayoutDirection
import com.mato.syai.R
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.CalendarToday
//import androidx.compose.material.icons.filled.Group
//import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
//import com.juraj.fluid.ui.theme.DEFAULT_PADDING
//import com.juraj.fluid.ui.theme.FluidBottomNavigationTheme
import kotlin.math.PI
import kotlin.math.sin
import com.mato.syai.presentation.toolbar.Toolbar
//D:\SyAi\app\src\main\java\com\mato\syai\presentation\toolbar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.navigation.compose.rememberNavController
import com.mato.syai.presentation.navigation.AppNavGraph

private fun getRenderEffect(): RenderEffect {
    val blurEffect = RenderEffect
        .createBlurEffect(80f, 80f, Shader.TileMode.MIRROR)

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

    return RenderEffect
        .createChainEffect(alphaMatrix, blurEffect)
}

@Composable
fun MainScreen() {
    val isMenuExtended = remember { mutableStateOf(false) }

    val fabAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        )
    )

    val clickAnimationProgress by animateFloatAsState(
        targetValue = if (isMenuExtended.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            easing = LinearEasing
        )
    )

    val renderEffect =
        getRenderEffect().asComposeRenderEffect()

    MainScreen(
        renderEffect = renderEffect,
        fabAnimationProgress = fabAnimationProgress,
        clickAnimationProgress = clickAnimationProgress
    ) {
        isMenuExtended.value = isMenuExtended.value.not()
    }
}

@Composable
fun MainScreen(
    renderEffect: androidx.compose.ui.graphics.RenderEffect?,
    fabAnimationProgress: Float = 0f,
    clickAnimationProgress: Float = 0f,
    toggleAnimation: () -> Unit = { },
) {
    val navController = rememberNavController()
    Toolbar(navController = navController)
//    CustomBottomNavigation(navController)
//    AppNavGraph(navController = navController)

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
//        CustomBottomNavigation(navController)
        Circle(
            color = Color.Blue, //MaterialTheme.colors.primary.copy(alpha = 0.5f),
            animationProgress = 0.5f
        )

        FabGroup(renderEffect = renderEffect, animationProgress = fabAnimationProgress)
        FabGroup(
            renderEffect = null,
            animationProgress = fabAnimationProgress,
            toggleAnimation = toggleAnimation
        )
        Circle(
            color = Color.White,
            animationProgress = clickAnimationProgress
        )
    }
}

@Composable
fun Circle(color: Color, animationProgress: Float) {
    val animationValue = sin(PI * animationProgress).toFloat()

    Box(
        modifier = Modifier
//            .padding(DEFAULT_PADDING.dp)
            .size(56.dp)
            .scale(2 - animationValue)
            .border(
                width = 2.dp,
                color = color.copy(alpha = color.alpha * animationValue),
                shape = CircleShape
            )
    )
}

@Composable
fun FabGroup(
    animationProgress: Float = 0f,
    renderEffect: androidx.compose.ui.graphics.RenderEffect? = null,
    toggleAnimation: () -> Unit = { }
) {
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.renderEffect = renderEffect },
        contentAlignment = Alignment.BottomCenter
    ) {

        AnimatedFab(
            icon = Icons.Default.Settings,
            modifier = Modifier
                .padding(
                    PaddingValues(
                        bottom = 72.dp,
                        end = 210.dp
                    ) * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress)
                ),
            opacity = LinearEasing.transform(0.2f, 0.7f, animationProgress)
        )

        AnimatedFab(
            icon = Icons.Default.Settings,
            modifier = Modifier.padding(
                PaddingValues(
                    bottom = 88.dp,
                ) * FastOutSlowInEasing.transform(0.1f, 0.9f, animationProgress)
            ),
            opacity = LinearEasing.transform(0.3f, 0.8f, animationProgress)
        )

        AnimatedFab(
            icon = Icons.Default.ShoppingCart,
            modifier = Modifier.padding(
                PaddingValues(
                    bottom = 72.dp,
                    start = 210.dp
                ) * FastOutSlowInEasing.transform(0.2f, 1.0f, animationProgress)
            ),
            opacity = LinearEasing.transform(0.4f, 0.9f, animationProgress)
        )

        AnimatedFab(
            modifier = Modifier
                .scale(1f - LinearEasing.transform(0.5f, 0.85f, animationProgress)),
        )

        AnimatedFab(
            icon = Icons.Default.Add,
            modifier = Modifier
                .rotate(
                    225 * FastOutSlowInEasing
                        .transform(0.35f, 0.65f, animationProgress)
                ),
            onClick = toggleAnimation,
            backgroundColor = Color.Transparent
        )
    }
}

@Composable
fun AnimatedFab(
    modifier: Modifier,
    icon: ImageVector? = null,
    opacity: Float = 1f,
    backgroundColor: Color = Color.White, //MaterialTheme.colors.secondary,
    onClick: () -> Unit = {}
) {
    FloatingActionButton(
        onClick = onClick,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
//        backgroundColor = backgroundColor,
        modifier = modifier.scale(1.25f)
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
@Preview(showBackground = true, showSystemUi = true)
fun MainScreenPreview() {
//    FluidBottomNavigationTheme {
        MainScreen()
//    }
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

//package com.mato.syai.presentation.bottomnavigation
//
//import androidx.compose.animation.core.Easing
//import androidx.compose.foundation.layout.calculateEndPadding
//import androidx.compose.foundation.layout.calculateStartPadding
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.FloatingActionButtonDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.ui.unit.LayoutDirection
//import com.mato.syai.R
//import android.graphics.ColorMatrix
//import android.graphics.ColorMatrixColorFilter
//import android.graphics.RenderEffect
//import android.graphics.Shader
//import android.os.Build
//import androidx.annotation.RequiresApi
//import androidx.compose.animation.core.FastOutSlowInEasing
//import androidx.compose.animation.core.LinearEasing
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.Icon
//
//import androidx.compose.material3.IconButton
//
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
////import androidx.compose.material.icons.filled.CalendarToday
////import androidx.compose.material.icons.filled.Group
////import androidx.compose.material.icons.filled.PhotoCamera
//import androidx.compose.material.icons.filled.Settings
//import androidx.compose.material.icons.filled.ShoppingCart
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.paint
//import androidx.compose.ui.draw.rotate
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.asComposeRenderEffect
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.graphics.vector.ImageVector
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.tooling.preview.Preview
//import androidx.compose.ui.unit.dp
////import com.juraj.fluid.ui.theme.DEFAULT_PADDING
////import com.juraj.fluid.ui.theme.FluidBottomNavigationTheme
//import kotlin.math.PI
//import kotlin.math.sin
//import androidx.compose.material.icons.filled.Home
//import androidx.compose.material.icons.filled.Build
//
//private fun getRenderEffect(): RenderEffect {
//    val blurEffect = RenderEffect
//        .createBlurEffect(80f, 80f, Shader.TileMode.MIRROR)
//
//    val alphaMatrix = RenderEffect.createColorFilterEffect(
//        ColorMatrixColorFilter(
//            ColorMatrix(
//                floatArrayOf(
//                    1f, 0f, 0f, 0f, 0f,
//                    0f, 1f, 0f, 0f, 0f,
//                    0f, 0f, 1f, 0f, 0f,
//                    0f, 0f, 0f, 50f, -5000f
//                )
//            )
//        )
//    )
//
//    return RenderEffect
//        .createChainEffect(alphaMatrix, blurEffect)
//}
//
//@Composable
//fun MainScreen() {
//    val isMenuExtended = remember { mutableStateOf(false) }
//
//    val fabAnimationProgress by animateFloatAsState(
//        targetValue = if (isMenuExtended.value) 1f else 0f,
//        animationSpec = tween(
//            durationMillis = 1000,
//            easing = LinearEasing
//        )
//    )
//
//    val clickAnimationProgress by animateFloatAsState(
//        targetValue = if (isMenuExtended.value) 1f else 0f,
//        animationSpec = tween(
//            durationMillis = 400,
//            easing = LinearEasing
//        )
//    )
//
//    val renderEffect =
//        getRenderEffect().asComposeRenderEffect()
//
//    MainScreen(
//        renderEffect = renderEffect,
//        fabAnimationProgress = fabAnimationProgress,
//        clickAnimationProgress = clickAnimationProgress
//    ) {
//        isMenuExtended.value = isMenuExtended.value.not()
//    }
//}
//
//@Composable
//fun MainScreen(
//    renderEffect: androidx.compose.ui.graphics.RenderEffect?,
//    fabAnimationProgress: Float = 0f,
//    clickAnimationProgress: Float = 0f,
//    toggleAnimation: () -> Unit = { }
//) {
//    Box(
//        Modifier
//            .fillMaxSize()
//            .padding(bottom = 24.dp),
//        contentAlignment = Alignment.BottomCenter
//    ) {
//        CustomBottomNavigation()
//        Circle(
//            color = Color.Blue, //MaterialTheme.colors.primary.copy(alpha = 0.5f),
//            animationProgress = 0.5f
//        )
//
//        FabGroup(renderEffect = renderEffect, animationProgress = fabAnimationProgress)
//        FabGroup(
//            renderEffect = null,
//            animationProgress = fabAnimationProgress,
//            toggleAnimation = toggleAnimation
//        )
//        Circle(
//            color = Color.White,
//            animationProgress = clickAnimationProgress
//        )
//    }
//}
//
//@Composable
//fun Circle(color: Color, animationProgress: Float) {
//    val animationValue = sin(PI * animationProgress).toFloat()
//
//    Box(
//        modifier = Modifier
////            .padding(DEFAULT_PADDING.dp)
//            .size(56.dp)
//            .scale(2 - animationValue)
//            .border(
//                width = 2.dp,
//                color = color.copy(alpha = color.alpha * animationValue),
//                shape = CircleShape
//            )
//    )
//}
//
////@Composable
////fun CustomBottomNavigation() {
////    Row(
////        horizontalArrangement = Arrangement.SpaceBetween,
////        verticalAlignment = Alignment.CenterVertically,
////        modifier = Modifier
////            .height(80.dp)
////            .paint(
////                painter = painterResource(R.drawable.bottom_navigation),
////                contentScale = ContentScale.FillHeight
////            )
////            .padding(horizontal = 40.dp)
////    ) {
////        listOf(Icons.Default.Settings, Icons.Default.Settings).map { image ->
////            IconButton(onClick = { }) {
////                Icon(imageVector = image, contentDescription = null, tint = Color.White)
////            }
////        }
////    }
////}
//
//@Composable
//fun FabGroup(
//    animationProgress: Float = 0f,
//    renderEffect: androidx.compose.ui.graphics.RenderEffect? = null,
//    toggleAnimation: () -> Unit = { }
//) {
//    Box(
//        Modifier
//            .fillMaxSize()
//            .graphicsLayer { this.renderEffect = renderEffect },
////            .padding(bottom = DEFAULT_PADDING.dp),
//        contentAlignment = Alignment.BottomCenter
//    ) {
//
//        AnimatedFab(
//            icon = Icons.Default.Settings,
//            modifier = Modifier
//                .padding(
//                    PaddingValues(
//                        bottom = 72.dp,
//                        end = 210.dp
//                    ) * FastOutSlowInEasing.transform(0f, 0.8f, animationProgress)
//                ),
//            opacity = LinearEasing.transform(0.2f, 0.7f, animationProgress)
//        )
//
//        AnimatedFab(
//            icon = Icons.Default.Settings,
//            modifier = Modifier.padding(
//                PaddingValues(
//                    bottom = 88.dp,
//                ) * FastOutSlowInEasing.transform(0.1f, 0.9f, animationProgress)
//            ),
//            opacity = LinearEasing.transform(0.3f, 0.8f, animationProgress)
//        )
//
//        AnimatedFab(
//            icon = Icons.Default.ShoppingCart,
//            modifier = Modifier.padding(
//                PaddingValues(
//                    bottom = 72.dp,
//                    start = 210.dp
//                ) * FastOutSlowInEasing.transform(0.2f, 1.0f, animationProgress)
//            ),
//            opacity = LinearEasing.transform(0.4f, 0.9f, animationProgress)
//        )
//
//        AnimatedFab(
//            modifier = Modifier
//                .scale(1f - LinearEasing.transform(0.5f, 0.85f, animationProgress)),
//        )
//
//        AnimatedFab(
//            icon = Icons.Default.Add,
//            modifier = Modifier
//                .rotate(
//                    225 * FastOutSlowInEasing
//                        .transform(0.35f, 0.65f, animationProgress)
//                ),
//            onClick = toggleAnimation,
//            backgroundColor = Color.Transparent
//        )
//    }
//}
//
//@Composable
//fun AnimatedFab(
//    modifier: Modifier,
//    icon: ImageVector? = null,
//    opacity: Float = 1f,
//    backgroundColor: Color = Color.White, //MaterialTheme.colors.secondary,
//    onClick: () -> Unit = {}
//) {
//    FloatingActionButton(
//        onClick = onClick,
//        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
////        backgroundColor = backgroundColor,
//        modifier = modifier.scale(1.25f)
//    ) {
//        icon?.let {
//            Icon(
//                imageVector = it,
//                contentDescription = null,
//                tint = Color.White.copy(alpha = opacity)
//            )
//        }
//    }
//}
//
//
//@Composable
//@Preview(showBackground = true, showSystemUi = true)
//fun MainScreenPreview() {
////    FluidBottomNavigationTheme {
//        MainScreen()
////    }
//}
//
//fun Easing.transform(from: Float, to: Float, value: Float): Float {
//    return transform(((value - from) * (1f / (to - from))).coerceIn(0f, 1f))
//}
//
//operator fun PaddingValues.times(value: Float): PaddingValues = PaddingValues(
//    top = calculateTopPadding() * value,
//    bottom = calculateBottomPadding() * value,
//    start = calculateStartPadding(LayoutDirection.Ltr) * value,
//    end = calculateEndPadding(LayoutDirection.Ltr) * value
//)