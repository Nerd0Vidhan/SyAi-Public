package com.mato.syai.note.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.mato.syai.note.domain.local.model.PageSize
import com.mato.syai.note.domain.local.model.PageUnitConverter
import com.mato.syai.note.domain.local.model.TextPayload
import com.mato.syai.note.domain.local.model.TextSpan
import com.mato.syai.note.domain.local.model.TextStyleData
import com.mato.syai.note.utils.RichTextParser
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType

private enum class SelectionDragTarget {
    START,
    END
}

private object NoOpTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: ComposeRect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) = Unit
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualLinearTextEditor(
    payload: TextPayload,
    widthPoints: Float,
    uiScale: Float = 1f,
    maxHeightPoints: Float? = null,
    isSelected: Boolean,
    activeStyle: TextStyleData,
    selection: TextRange? = null,
    onTextChange: (String, List<TextSpan>) -> Unit,
    onSelectionChange: (TextRange) -> Unit,
    onCopy: (String, Int, Int) -> Unit = { _, _, _ -> },
    onPaste: (Int) -> Unit = { _ -> },
    onHeightMeasured: (Float) -> Unit = {},
    onBackspaceAtStart: () -> Unit = {},
    onCheckboxToggle: (Int) -> Unit = {},
    onOverflow: (String, String, List<TextSpan>, List<TextSpan>) -> Unit = { _, _, _, _ -> }
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val hostView = LocalView.current
    val density = LocalDensity.current

    var hiddenInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = "\u200B" + payload.text,
                selection = TextRange(payload.text.length + 1)
            )
        )
    }

    LaunchedEffect(payload.text) {
        val targetText = "\u200B" + payload.text
        if (hiddenInput.text != targetText) {
            hiddenInput = hiddenInput.copy(
                text = targetText,
                selection = TextRange(hiddenInput.selection.end.coerceAtMost(targetText.length))
            )
        }
    }

    LaunchedEffect(selection, isSelected) {
        if (!isSelected || selection == null) return@LaunchedEffect
        val normalized = TextRange(
            start = (selection.start + 1).coerceIn(1, payload.text.length + 1),
            end = (selection.end + 1).coerceIn(1, payload.text.length + 1)
        )
        if (hiddenInput.selection != normalized) {
            hiddenInput = hiddenInput.copy(selection = normalized)
        }
    }
    var isFocused by remember { mutableStateOf(false) }
    var latestLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerInWindow by remember { mutableStateOf(Offset.Zero) }
    var selectionActionMode by remember { mutableStateOf<ActionMode?>(null) }
    var selectionActionModeCallback by remember { mutableStateOf<TextSelectionActionModeCallback?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val cursorAlpha by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    LaunchedEffect(isSelected) {
        if (isSelected) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            selectionActionMode?.finish()
            selectionActionMode = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            selectionActionMode?.finish()
        }
    }

    val textStyle = TextStyle(
        color = androidx.compose.ui.graphics.Color(payload.style.color),
        fontSize = with(density) { (payload.style.fontSize * uiScale).toSp() },
        fontWeight = if (payload.style.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (payload.style.isItalic) FontStyle.Italic else FontStyle.Normal,
        textAlign = when (payload.style.alignment) {
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.Right
            "JUSTIFY" -> TextAlign.Justify
            else -> TextAlign.Left
        },
        textDecoration = if (payload.style.isUnderline) TextDecoration.Underline else TextDecoration.None
    )

    val displayText = remember(hiddenInput.text) {
        LinearListMarkerCodec.displayText(hiddenInput.text.removePrefix("\u200B"))
    }

    val annotatedText = remember(displayText, payload.spans, payload.style, uiScale, density.fontScale) {
        RichTextParser.buildRichText(
            text = displayText.ifEmpty { " " },
            defaultStyle = payload.style,
            spans = payload.spans,
            uiScale = uiScale,
            density = density
        )
    }

    val measuredLayout = remember(annotatedText, textStyle, canvasSize) {
        if (canvasSize.width <= 0) {
            null
        } else {
            textMeasurer.measure(
                text = annotatedText,
                style = textStyle,
                maxLines = Int.MAX_VALUE,
                constraints = Constraints(
                    minWidth = canvasSize.width,
                    maxWidth = canvasSize.width
                )
            )
        }
    }

    LaunchedEffect(measuredLayout) {
        latestLayout = measuredLayout
    }

    val contentHeightDp = with(density) {
        ((measuredLayout?.size?.height ?: 0).toDp() + 16.dp).coerceAtLeast(42.dp)
    }

    LaunchedEffect(measuredLayout?.size?.height, uiScale) {
        val measuredHeightPx = measuredLayout?.size?.height ?: return@LaunchedEffect
        if (uiScale <= 0f) return@LaunchedEffect
        onHeightMeasured((measuredHeightPx / uiScale + 16f).coerceAtLeast(42f))
    }

    fun toExternalSelection(selection: TextRange): TextRange {
        return TextRange(
            start = (selection.start - 1).coerceIn(0, payload.text.length),
            end = (selection.end - 1).coerceIn(0, payload.text.length)
        )
    }

    fun updateSelection(selection: TextRange) {
        val clampedSelection = TextRange(
            start = selection.start.coerceIn(1, payload.text.length + 1),
            end = selection.end.coerceIn(1, payload.text.length + 1)
        )
        hiddenInput = hiddenInput.copy(selection = clampedSelection)
        onSelectionChange(toExternalSelection(clampedSelection))
        selectionActionMode?.finish()
        selectionActionMode = null
        selectionActionModeCallback = null
    }

    fun selectionHandleTarget(offset: Offset, layout: TextLayoutResult): SelectionDragTarget? {
        val selection = hiddenInput.selection
        if (selection.collapsed) return null

        val mappedMin = (selection.min - 1).coerceAtLeast(0)
        val mappedMax = (selection.max - 1).coerceAtLeast(0)
        val handleYOffset = with(density) { 8.dp.toPx() }
        val hitRadius = with(density) { 30.dp.toPx() }
        val hitRadiusSq = hitRadius * hitRadius
        fun isNear(center: Offset): Boolean {
            val dx = offset.x - center.x
            val dy = offset.y - center.y
            return dx * dx + dy * dy <= hitRadiusSq
        }

        val startCursorRect = layout.getCursorRect(mappedMin)
        val endCursorRect = layout.getCursorRect(mappedMax)
        val startCenter = Offset(startCursorRect.left, startCursorRect.bottom + handleYOffset)
        val endCenter = Offset(endCursorRect.left, endCursorRect.bottom + handleYOffset)
        return when {
            isNear(startCenter) -> SelectionDragTarget.START
            isNear(endCenter) -> SelectionDragTarget.END
            else -> null
        }
    }

    Box(
        modifier = Modifier
            .width(with(density) { (widthPoints * uiScale).toDp() })
            .heightIn(min = contentHeightDp)
            .onSizeChanged { canvasSize = it }
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInWindow()
                containerInWindow = Offset(position.x, position.y)
            }
            .onKeyEvent { keyEvent ->
                if (
                    keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.key == Key.Backspace &&
                    hiddenInput.text.isEmpty()
                ) {
                    onBackspaceAtStart()
                    true
                } else false
            }
            .pointerInput(payload.text, isSelected) {
                detectTapGestures(
                    onTap = { offset ->
                        val layout = latestLayout ?: return@detectTapGestures
                        val position = layout.getOffsetForPosition(offset)
                        val lineInfo = LinearListMarkerCodec.lineAt(hiddenInput.text, position)
                        if (
                            LinearListMarkerCodec.isCheckboxMarker(lineInfo.marker) &&
                            position < lineInfo.start + (lineInfo.marker?.rawLength ?: 0)
                        ) {
                            onCheckboxToggle(lineInfo.start)
                            return@detectTapGestures
                        }
                        updateSelection(TextRange(position + 1))
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    onLongPress = { offset ->
                        val layout = latestLayout ?: return@detectTapGestures
                        val index = layout.getOffsetForPosition(offset)
                        val mappedIndex = (index - 1).coerceAtLeast(0)
                        onTextChange(payload.text, payload.spans)
                        updateSelection(TextRange(index + 1, payload.text.length + 1))
                    }
                )
            }
            .pointerInput(payload.text) {
                var dragTarget: SelectionDragTarget? = null
                detectDragGestures(
                    onDragStart = { start ->
                        val layout = latestLayout
                        if (layout != null) {
                            dragTarget = selectionHandleTarget(start, layout)
                            if (dragTarget == null) {
                                val position = layout.getOffsetForPosition(start)
                                updateSelection(TextRange(position + 1))
                                dragTarget = SelectionDragTarget.END
                            }
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    },
                    onDrag = { change, _ ->
                        val layout = latestLayout
                        if (layout != null) {
                            val current = (layout.getOffsetForPosition(change.position) + 1)
                                .coerceIn(1, payload.text.length + 1)
                            val fixedSelection = hiddenInput.selection
                            val nextSelection = when (dragTarget) {
                                SelectionDragTarget.START -> TextRange(current, fixedSelection.max)
                                SelectionDragTarget.END, null -> TextRange(fixedSelection.min, current)
                            }
                            updateSelection(nextSelection)
                            change.consume()
                        }
                    },
                    onDragEnd = { dragTarget = null },
                    onDragCancel = { dragTarget = null }
                )
            }
            .pointerInput(payload.text) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        val layout = latestLayout ?: return@detectDragGesturesAfterLongPress
                        val position = layout.getOffsetForPosition(start)
                        updateSelection(TextRange(position + 1))
                    },
                    onDrag = { change, _ ->
                        val layout = latestLayout ?: return@detectDragGesturesAfterLongPress
                        val start = hiddenInput.selection.start
                        val current = layout.getOffsetForPosition(change.position) + 1
                        updateSelection(TextRange(min(start, current), max(start, current)))
                        change.consume()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val layout = measuredLayout ?: return@Canvas

            val selection = hiddenInput.selection
            if (!selection.collapsed) {
                val minSelection = selection.min
                val maxSelection = selection.max
                val mappedMin = (minSelection - 1).coerceAtLeast(0)
                val mappedMax = (maxSelection - 1).coerceAtLeast(0)
                
                drawPath(
                    path = layout.getPathForRange(mappedMin, mappedMax),
                    color = androidx.compose.ui.graphics.Color(0x334F46E5)
                )
                
                // Draw teardrops
                val startCursorRect = layout.getCursorRect(mappedMin)
                val endCursorRect = layout.getCursorRect(mappedMax)
                val primaryColor = androidx.compose.ui.graphics.Color(0xFF4F46E5)
                val handleRadius = 10.dp.toPx()
                val handleYOffset = 8.dp.toPx()
                
                // Start handle
                drawLine(
                    color = primaryColor,
                    start = Offset(startCursorRect.left, startCursorRect.top),
                    end = Offset(startCursorRect.left, startCursorRect.bottom),
                    strokeWidth = 3.dp.toPx()
                )
                drawCircle(
                    color = primaryColor,
                    radius = handleRadius,
                    center = Offset(startCursorRect.left, startCursorRect.bottom + handleYOffset)
                )
                
                // End handle
                drawLine(
                    color = primaryColor,
                    start = Offset(endCursorRect.left, endCursorRect.top),
                    end = Offset(endCursorRect.left, endCursorRect.bottom),
                    strokeWidth = 3.dp.toPx()
                )
                drawCircle(
                    color = primaryColor,
                    radius = handleRadius,
                    center = Offset(endCursorRect.left, endCursorRect.bottom + handleYOffset)
                )
            }

            drawText(layout)

            if (isSelected && isFocused && selection.collapsed) {
                val mappedIndex = (selection.start - 1).coerceAtLeast(0)
                val cursorRect = layout.getCursorRect(mappedIndex)
                drawLine(
                    color = androidx.compose.ui.graphics.Color(payload.style.color).copy(alpha = cursorAlpha),
                    start = Offset(cursorRect.left, cursorRect.top),
                    end = Offset(cursorRect.left, cursorRect.bottom),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        CompositionLocalProvider(LocalTextToolbar provides NoOpTextToolbar) {
            BasicTextField(
                value = hiddenInput,
                onValueChange = { newValue ->
                val previousSelection = hiddenInput.selection
                val oldText = hiddenInput.text
                var newText = newValue.text
                var newSelection = newValue.selection

                if (newSelection.start == 0 && newSelection.end == 0 && newText.startsWith("\u200B")) {
                    newSelection = TextRange(1)
                }

                if (!newText.contains("\u200B")) {
                    if (oldText == "\u200B" && newText.isEmpty()) {
                        onBackspaceAtStart()
                        return@BasicTextField
                    }
                    newText = "\u200B" + newText
                    newSelection = TextRange((newSelection.start + 1).coerceAtMost(newText.length), (newSelection.end + 1).coerceAtMost(newText.length))
                } else if (!newText.startsWith("\u200B")) {
                    val split = newText.split("\u200B")
                    newText = "\u200B" + split.joinToString("")
                }

                hiddenInput = newValue.copy(text = newText, selection = newSelection)

                val realOldText = oldText.replace("\u200B", "")
                val realNewText = newText.replace("\u200B", "")

                var newSpans = payload.spans.toList()
                
                val commonPrefixLen = realOldText.commonPrefixWith(realNewText).length
                val commonSuffixLen = realOldText.reversed().commonPrefixWith(realNewText.reversed()).length
                
                val overlap = (commonPrefixLen + commonSuffixLen) - min(realOldText.length, realNewText.length)
                val safeSuffixLen = if (overlap > 0) commonSuffixLen - overlap else commonSuffixLen
                
                val replaceStart = commonPrefixLen
                val oldReplaceEnd = realOldText.length - safeSuffixLen
                val newReplaceEnd = realNewText.length - safeSuffixLen
                
                val deletedLen = oldReplaceEnd - replaceStart
                val insertedLen = newReplaceEnd - replaceStart
                val delta = insertedLen - deletedLen
                
                if (delta != 0 || insertedLen > 0 || deletedLen > 0) {
                    newSpans = newSpans.flatMap { span ->
                        val pieces = mutableListOf<TextSpan>()
                        when {
                            span.end <= replaceStart -> pieces.add(span)
                            span.start >= oldReplaceEnd -> pieces.add(
                                span.copy(
                                    start = span.start + delta,
                                    end = span.end + delta
                                )
                            )
                            else -> {
                                if (span.start < replaceStart) {
                                    pieces.add(span.copy(end = replaceStart))
                                }
                                if (span.end > oldReplaceEnd) {
                                    pieces.add(
                                        span.copy(
                                            start = replaceStart + insertedLen,
                                            end = span.end + delta
                                        )
                                    )
                                }
                            }
                        }
                        pieces
                    }
                }
                
                if (insertedLen > 0) {
                    val lastSpan = newSpans.lastOrNull()
                    if (lastSpan != null && lastSpan.style == activeStyle && lastSpan.end == replaceStart) {
                        // Extend existing span
                        newSpans = newSpans.dropLast(1) + lastSpan.copy(end = replaceStart + insertedLen)
                    } else {
                        newSpans = newSpans + TextSpan(replaceStart, replaceStart + insertedLen, activeStyle)
                    }
                }

                newSpans = mergeAdjacentSpans(newSpans)

                val externalCursor = toExternalSelection(newSelection).end
                LinearListMarkerCodec.normalizeInsertedNewLine(realNewText, externalCursor)?.let { (normalizedText, normalizedCursor) ->
                    hiddenInput = TextFieldValue(
                        text = "\u200B$normalizedText",
                        selection = TextRange((normalizedCursor + 1).coerceIn(1, normalizedText.length + 1))
                    )
                    onTextChange(normalizedText, newSpans)
                    onSelectionChange(TextRange(normalizedCursor.coerceIn(0, normalizedText.length)))
                    return@BasicTextField
                }

                val maxHeightPx = maxHeightPoints?.let { with(density) { (it * uiScale).toDp().toPx() } }
                if (maxHeightPx != null && canvasSize.width > 0) {
                    val overflowDisplayText = LinearListMarkerCodec.displayText(realNewText)
                    val overflowLayout = textMeasurer.measure(
                        text = RichTextParser.buildRichText(
                            text = overflowDisplayText.ifEmpty { " " },
                            defaultStyle = payload.style,
                            spans = newSpans,
                            uiScale = uiScale,
                            density = density
                        ),
                        style = textStyle,
                        maxLines = Int.MAX_VALUE,
                        constraints = Constraints(
                            minWidth = canvasSize.width,
                            maxWidth = canvasSize.width
                        )
                    )

                    if (overflowLayout.size.height > maxHeightPx) {
                        val fittingLine = (0 until overflowLayout.lineCount)
                            .lastOrNull { overflowLayout.getLineBottom(it) <= maxHeightPx }
                            ?: 0
                        val cutIndex = overflowLayout.getLineEnd(fittingLine, visibleEnd = true)
                            .coerceIn(0, realNewText.length)
                        if (cutIndex in 1 until realNewText.length) {
                            val visibleText = realNewText.substring(0, cutIndex).trimEnd()
                            val overflowText = realNewText.substring(cutIndex).trimStart('\n')
                            val visibleSpans = mutableListOf<TextSpan>()
                            val overflowSpans = mutableListOf<TextSpan>()
                            newSpans.forEach { span ->
                                if (span.start < cutIndex) {
                                    visibleSpans.add(
                                        span.copy(end = min(span.end, cutIndex))
                                    )
                                }
                                if (span.end > cutIndex) {
                                    overflowSpans.add(
                                        span.copy(
                                            start = max(0, span.start - cutIndex),
                                            end = span.end - cutIndex
                                        )
                                    )
                                }
                            }
                            hiddenInput = TextFieldValue(
                                text = "\u200B$visibleText",
                                selection = TextRange(visibleText.length + 1)
                            )
                            onOverflow(visibleText, overflowText, visibleSpans, overflowSpans)
                            return@BasicTextField
                        }
                    }
                }
                
                hiddenInput = newValue.copy(text = newText, selection = newSelection)
                if (realOldText != realNewText) {
                    onTextChange(realNewText, newSpans)
                }
                onSelectionChange(toExternalSelection(newSelection))
                if (newSelection != previousSelection) {
                    updateSelection(newSelection)
                } else if (newSelection.collapsed) {
                    selectionActionMode?.finish()
                    selectionActionMode = null
                    selectionActionModeCallback = null
                }
                },
                textStyle = textStyle.copy(color = androidx.compose.ui.graphics.Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                modifier = Modifier
                    .size(1.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { isFocused = it.isFocused }
            )
        }
    }
}

private fun mergeAdjacentSpans(spans: List<TextSpan>): List<TextSpan> {
    if (spans.isEmpty()) return emptyList()
    val merged = mutableListOf<TextSpan>()
    spans
        .sortedWith(compareBy<TextSpan> { it.start }.thenBy { it.end })
        .forEach { span ->
            val last = merged.lastOrNull()
            if (last != null && last.end >= span.start && last.style == span.style) {
                merged[merged.lastIndex] = last.copy(end = max(last.end, span.end))
            } else if (last != null && last.end == span.start && last.style == span.style) {
                merged[merged.lastIndex] = last.copy(end = span.end)
            } else {
                merged.add(span)
            }
        }
    return merged
}

@RequiresApi(Build.VERSION_CODES.M)
private class TextSelectionActionModeCallback(
    private val context: Context,
    private val getSelection: () -> TextRange,
    private val getText: () -> String,
    private val onSelectionUpdate: (TextRange) -> Unit,
    private val onCopy: (String, Int, Int) -> Unit,
    private val onPaste: () -> Unit
) : ActionMode.Callback2() {

    var contentRect: Rect = Rect()

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.add(0, android.R.id.copy, 0, android.R.string.copy)
        menu?.add(0, android.R.id.paste, 1, android.R.string.paste)
        menu?.add(0, android.R.id.selectAll, 2, android.R.string.selectAll)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.copy -> {
                val selection = getSelection()
                val text = getText()
                if (!selection.collapsed) {
                    val selectedText = text.substring(selection.min, selection.max)
                    onCopy(selectedText, selection.min, selection.max)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("selected_text", selectedText))
                }
                mode?.finish()
                return true
            }
            android.R.id.paste -> {
                onPaste()
                mode?.finish()
                return true
            }

            android.R.id.selectAll -> {
                onSelectionUpdate(TextRange(0, getText().length))
                mode?.invalidateContentRect()
                return true
            }
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) = Unit

    override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: Rect?) {
        outRect?.set(contentRect)
    }
}

@RequiresApi(Build.VERSION_CODES.M)
private fun calculateSelectionRect(
    layout: TextLayoutResult?,
    selection: TextRange,
    containerInWindow: Offset
): Rect? {
    if (layout == null || selection.collapsed) return null
    val minIndex = selection.min
    val maxIndex = selection.max
    val endIndex = (maxIndex - 1).coerceAtLeast(minIndex)
    val startBox = layout.getBoundingBox(minIndex)
    val endBox = layout.getBoundingBox(endIndex)
    return Rect(
        (containerInWindow.x + min(startBox.left, endBox.left)).toInt(),
        (containerInWindow.y + min(startBox.top, endBox.top)).toInt(),
        (containerInWindow.x + max(startBox.right, endBox.right)).toInt(),
        (containerInWindow.y + max(startBox.bottom, endBox.bottom)).toInt()
    )
}
