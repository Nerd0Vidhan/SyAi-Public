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


@Composable
fun CustomBottomNavigation() {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(80.dp)
            .paint(
                painter = painterResource(R.drawable.bottom_navigation),
                contentScale = ContentScale.FillHeight
            )
            .padding(horizontal = 40.dp)
    ) {
        // Dashboard
        IconButton(onClick = { /* Handle Dashboard click */ }) {
            Icon(
                imageVector = Icons.Default.Settings, // replace with desired Dashboard icon
                contentDescription = "Dashboard",
                tint = Color.White
            )
        }
        // Notes
        IconButton(onClick = { /* Handle Notes click */ }) {
            Icon(
                imageVector = Icons.Default.ShoppingCart, // replace with Notes icon
                contentDescription = "Notes",
                tint = Color.White
            )
        }
        // Settings
        IconButton(onClick = { /* Handle Settings click */ }) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }
        // Tools
        IconButton(onClick = { /* Handle Tools click */ }) {
            Icon(
                imageVector = Icons.Default.Add, // replace with Tools icon
                contentDescription = "Tools",
                tint = Color.White
            )
        }
        // AI Assistant
        IconButton(onClick = { /* Handle AI Assistant click */ }) {
            Icon(
                imageVector = Icons.Default.ShoppingCart, // replace with AI Assistant icon
                contentDescription = "AI Assistant",
                tint = Color.White
            )
        }
    }
}
