package com.mato.syai.note.ui.editor

import com.mato.syai.note.domain.local.model.BulletListStyle
import com.mato.syai.note.domain.local.model.ListMarker
import com.mato.syai.note.domain.local.model.OrderedListStyle

object LinearListMarkerCodec {
    private const val BULLET = "123"
    private const val DASH = "124"
    private const val STAR = "125"
    private const val NUMBER = "126"
    private const val ROMAN = "127"
    private const val LOWER_ALPHA = "128"
    private const val UPPER_ALPHA = "129"
    private const val CIRCLE = "130"
    private const val SQUARE = "131"
    private const val LOWER_ROMAN = "132"
    private const val CHECKBOX_UNCHECKED = "220"
    private const val CHECKBOX_CHECKED = "221"

    private val markerRegex = Regex("^#(\\d+)#(\\d+)#")

    data class Marker(
        val code: String,
        val depth: Int,
        val rawLength: Int
    )

    data class LineInfo(
        val start: Int,
        val end: Int,
        val line: String,
        val marker: Marker?
    )

    fun markerFor(
        marker: ListMarker,
        orderedStyle: OrderedListStyle? = null,
        bulletStyle: BulletListStyle? = null,
        depth: Int = 0
    ): String {
        val code = when (marker) {
            ListMarker.CHECKBOX, ListMarker.CHECK -> CHECKBOX_UNCHECKED
            ListMarker.NUMBER -> when (orderedStyle) {
                OrderedListStyle.LOWER_ALPHA -> LOWER_ALPHA
                OrderedListStyle.UPPER_ALPHA -> UPPER_ALPHA
                else -> NUMBER
            }
            ListMarker.ROMAN -> when (orderedStyle) {
                OrderedListStyle.LOWER_ROMAN -> LOWER_ROMAN
                else -> ROMAN
            }
            ListMarker.DASH -> DASH
            ListMarker.STAR -> STAR
            ListMarker.BULLET -> when (bulletStyle) {
                BulletListStyle.DASH -> DASH
                BulletListStyle.CIRCLE -> CIRCLE
                BulletListStyle.SQUARE -> SQUARE
                else -> BULLET
            }
        }
        return "#$code#$depth#"
    }

    fun lineMarker(line: String): Marker? {
        val match = markerRegex.find(line) ?: return null
        return Marker(
            code = match.groupValues[1],
            depth = match.groupValues[2].toIntOrNull()?.coerceIn(0, 8) ?: 0,
            rawLength = match.value.length
        )
    }

    fun isListLine(line: String): Boolean = lineMarker(line) != null

    fun isCheckboxMarker(marker: Marker?): Boolean =
        marker?.code == CHECKBOX_UNCHECKED || marker?.code == CHECKBOX_CHECKED

    fun displayText(rawText: String): String {
        if (rawText.isEmpty()) return rawText
        var orderedIndex = 1
        return rawText
            .split('\n')
            .joinToString("\n") { line ->
                val marker = lineMarker(line)
                if (marker == null) {
                    orderedIndex = 1
                    line
                } else {
                    val body = line.drop(marker.rawLength)
                    val indent = "  ".repeat(marker.depth)
                    val visualMarker = when (marker.code) {
                        CHECKBOX_UNCHECKED -> "[ ]"
                        CHECKBOX_CHECKED -> "[x]"
                        NUMBER -> "${orderedIndex++}."
                        ROMAN -> "${toRoman(orderedIndex++)}."
                        LOWER_ROMAN -> "${toRoman(orderedIndex++).lowercase()}."
                        LOWER_ALPHA -> "${toAlphabet(orderedIndex++, uppercase = false)}."
                        UPPER_ALPHA -> "${toAlphabet(orderedIndex++, uppercase = true)}."
                        CIRCLE -> "\u25e6"
                        SQUARE -> "\u25aa"
                        DASH -> "-"
                        STAR -> "*"
                        else -> "\u2022"
                    }
                    val visualPrefix = "$indent$visualMarker "
                        .take(marker.rawLength)
                        .padEnd(marker.rawLength, ' ')
                    "$visualPrefix$body"
                }
            }
    }

    fun normalizeInsertedNewLine(text: String, cursor: Int): Pair<String, Int>? {
        if (cursor <= 0 || cursor > text.length || text.getOrNull(cursor - 1) != '\n') return null

        val beforeNewLine = text.substring(0, cursor - 1)
        val currentLineStart = beforeNewLine.lastIndexOf('\n').let { if (it == -1) 0 else it + 1 }
        val previousLine = beforeNewLine.substring(currentLineStart)
        val marker = lineMarker(previousLine) ?: return null
        val previousBody = previousLine.drop(marker.rawLength)

        if (previousBody.isBlank()) {
            val linePrefix = beforeNewLine.substring(0, currentLineStart)
            val afterCursor = text.substring(cursor)
            val replacement = linePrefix + afterCursor
            return replacement to linePrefix.length
        }

        val nextMarker = "#${marker.code}#${marker.depth}#"
        val updated = text.substring(0, cursor) + nextMarker + text.substring(cursor)
        return updated to (cursor + nextMarker.length)
    }

    fun toggleCheckboxAtLine(text: String, lineStart: Int): String {
        val lineEnd = text.indexOf('\n', lineStart).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val marker = lineMarker(line) ?: return text
        val newCode = when (marker.code) {
            CHECKBOX_UNCHECKED -> CHECKBOX_CHECKED
            CHECKBOX_CHECKED -> CHECKBOX_UNCHECKED
            else -> return text
        }
        val replacement = "#$newCode#${marker.depth}#" + line.drop(marker.rawLength)
        return text.replaceRange(lineStart, lineEnd, replacement)
    }

    fun lineAt(text: String, cursor: Int): LineInfo {
        val safeCursor = cursor.coerceIn(0, text.length)
        val startSearch = (safeCursor - 1).coerceAtLeast(0)
        val start = text.lastIndexOf('\n', startSearch).let { if (it == -1) 0 else it + 1 }
        val end = text.indexOf('\n', safeCursor).let { if (it == -1) text.length else it }
        val line = text.substring(start, end)
        return LineInfo(start, end, line, lineMarker(line))
    }

    fun replaceLineMarker(text: String, cursor: Int, markerToken: String): Pair<String, Int> {
        val info = lineAt(text, cursor)
        val currentMarker = info.marker
        if (currentMarker == null) {
            val updated = text.replaceRange(info.start, info.start, markerToken)
            return updated to (cursor + markerToken.length)
        }

        val updated = text.replaceRange(info.start, info.start + currentMarker.rawLength, markerToken)
        val delta = markerToken.length - currentMarker.rawLength
        return updated to (cursor + delta).coerceIn(0, updated.length)
    }

    fun shiftLineDepth(text: String, cursor: Int, delta: Int): Pair<String, Int>? {
        val info = lineAt(text, cursor)
        val marker = info.marker ?: return null
        val newDepth = (marker.depth + delta).coerceIn(0, 8)
        if (newDepth == marker.depth) return text to cursor.coerceIn(0, text.length)
        val replacement = "#${marker.code}#$newDepth#"
        val updated = text.replaceRange(info.start, info.start + marker.rawLength, replacement)
        val cursorDelta = replacement.length - marker.rawLength
        return updated to (cursor + cursorDelta).coerceIn(0, updated.length)
    }

    private fun toAlphabet(number: Int, uppercase: Boolean): String {
        var value = number
        val builder = StringBuilder()
        while (value > 0) {
            value--
            builder.insert(0, ('a'.code + (value % 26)).toChar())
            value /= 26
        }
        val result = builder.toString()
        return if (uppercase) result.uppercase() else result
    }

    private fun toRoman(number: Int): String {
        var value = number.coerceIn(1, 3999)
        val numerals = listOf(
            1000 to "M",
            900 to "CM",
            500 to "D",
            400 to "CD",
            100 to "C",
            90 to "XC",
            50 to "L",
            40 to "XL",
            10 to "X",
            9 to "IX",
            5 to "V",
            4 to "IV",
            1 to "I"
        )
        return buildString {
            numerals.forEach { (arabic, roman) ->
                while (value >= arabic) {
                    append(roman)
                    value -= arabic
                }
            }
        }
    }
}
