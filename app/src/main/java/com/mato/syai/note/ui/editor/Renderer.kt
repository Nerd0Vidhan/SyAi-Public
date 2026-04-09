package com.mato.syai.note.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mato.syai.note.domain.local.model.CustomObject
import com.mato.syai.note.domain.local.model.TextSpan

@Composable
fun RenderText(obj: CustomObject) {
    // We parse the 'spans' list from the data map
    val spans = obj.data["spans"] as? List<TextSpan> ?: emptyList()

    val annotatedString = buildAnnotatedString {
        spans.forEach { span ->
            withStyle(style = SpanStyle(
                color = Color(span.color),
                fontSize = span.fontSize.sp,
                fontWeight = if (span.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (span.isItalic) FontStyle.Italic else FontStyle.Normal
            )) {
                append(span.text)
            }
        }
    }
    Text(text = annotatedString)
}

@Composable
fun RenderDrawing(obj: CustomObject) {
    /*val drawing = obj.data["path"] as? DrawingPath ?: return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = Path().apply {
            if (drawing.points.isNotEmpty()) {
                moveTo(drawing.points[0].x, drawing.points[0].y)
                drawing.points.forEach { lineTo(it.x, it.y) }
            }
        }
        drawPath(
            path = path,
            color = Color(drawing.color),
            style = Stroke(
                width = drawing.thickness,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }*/
}