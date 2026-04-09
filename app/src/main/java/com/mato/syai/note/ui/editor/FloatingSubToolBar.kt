package com.mato.syai.note.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.ActiveTool
import com.mato.syai.note.domain.local.model.CustomObject
import com.mato.syai.note.domain.local.model.PointData
import com.mato.syai.note.domain.local.model.Tool

@Composable
fun ContextualSubToolbar(
    activeTool: ActiveTool,
    viewModel: NoteEditorViewModel
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // The specific options (e.g., Color Picker or Font Size)
        AnimatedVisibility(visible = activeTool == ActiveTool.DRAW) {
            DrawingSubMenu(viewModel)
        }

        AnimatedVisibility(visible = activeTool == ActiveTool.TEXT) {
//            TextSubMenu(viewModel)
        }

        Spacer(Modifier.height(8.dp))

        // The Main Tool Selector (Already created in Part 1)
        FloatingSubToolbar(
            activeTool = activeTool,
            onToolSelect = { viewModel.setTool(it) }
        )
    }
}

@Composable
fun DrawingSubMenu(viewModel: NoteEditorViewModel) {
    Surface(
        color = Color.White.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.padding(bottom = 8.dp),
        shadowElevation = 4.dp
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Quick Color Dots
            listOf(Color.Black, Color.Red, Color.Blue, Color.Green).forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(24.dp)
                        .background(color, CircleShape)
                        .clickable { viewModel.currentDrawColor = color.toArgb() }
                )
            }
            Divider(Modifier.width(1.dp).height(20.dp).padding(horizontal = 8.dp))
            // Thickness Slider
            Slider(
                value = viewModel.currentDrawThickness,
                onValueChange = { viewModel.currentDrawThickness = it },
                valueRange = 1f..50f,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}


// In DrawingCanvas.kt or a Utils file
fun DrawScope.renderStaticDrawing(obj: CustomObject) {
    // Retrieve the path from the generic Map
    val dataMap = obj.data["pathData"] as? Map<String, Any> ?: return

    // GSON often converts numbers to Doubles, so we cast carefully
    val points = (dataMap["points"] as? List<Map<String, Any>>)?.map {
        PointData((it["x"] as Double).toFloat(), (it["y"] as Double).toFloat())
    } ?: return
    val color = (dataMap["color"] as Double).toInt()
    val thickness = (dataMap["thickness"] as Double).toFloat()

    val path = Path().apply {
        if (points.isNotEmpty()) {
            moveTo(points[0].x, points[0].y)
            points.forEach { lineTo(it.x, it.y) }
        }
    }

    drawPath(
        path = path,
        color = Color(color),
        style = Stroke(width = thickness, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}