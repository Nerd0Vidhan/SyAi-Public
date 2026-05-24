package com.mato.syai.note.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.mato.syai.note.domain.local.model.PageSize
import com.mato.syai.note.domain.local.model.PageUnitConverter
import com.mato.syai.note.domain.local.model.TextSpan
import com.mato.syai.note.domain.local.model.TextStyleData
import kotlin.math.max
import kotlin.math.min

object RichTextParser {

    fun buildRichText(
        text: String,
        defaultStyle: TextStyleData,
        spans: List<TextSpan>,
        uiScale: Float = 1f,
        density: Density,
        pageSize: PageSize = PageSize.A4
    ): AnnotatedString {
        return buildAnnotatedString {
            // Apply default style to the whole text if needed,
            // but usually we rely on the parent Text composable's style.
            // Here we just append the text.
            append(text)

            // Apply specific spans
            for (span in spans) {
                // Ensure span bounds are within the text length
                val start = max(0, min(span.start, text.length))
                val end = max(0, min(span.end, text.length))
                
                if (start < end) {
                    addStyle(
                        style = SpanStyle(
                            color = Color(span.style.color),
                            fontSize = with(density) { (span.style.fontSize * uiScale).toSp() },
                            fontWeight = if (span.style.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (span.style.isItalic) FontStyle.Italic else FontStyle.Normal,
                        ),
                        start = start,
                        end = end
                    )
                }
            }
        }
    }
}
