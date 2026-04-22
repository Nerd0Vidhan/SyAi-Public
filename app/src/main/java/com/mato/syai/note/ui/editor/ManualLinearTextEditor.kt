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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualLinearTextEditor(
    payload: TextPayload,
    widthPoints: Float,
    uiScale: Float = 1f,
    isSelected: Boolean,
    activeStyle: TextStyleData,
    onTextChange: (String, List<TextSpan>) -> Unit,
    onSelectionChange: (TextRange) -> Unit,
    onCopy: (String, Int, Int) -> Unit = { _, _, _ -> },
    onPaste: (Int) -> Unit = { _ -> }
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val hostView = LocalView.current
    val density = LocalDensity.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    var hiddenInput by remember(payload.text) {
        mutableStateOf(
            TextFieldValue(
                text = payload.text,
                selection = TextRange(payload.text.length)
            )
        )
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

    LaunchedEffect(payload.text) {
        if (hiddenInput.text != payload.text) {
            hiddenInput = hiddenInput.copy(
                text = payload.text,
                selection = TextRange(hiddenInput.selection.end.coerceAtMost(payload.text.length))
            )
        }
    }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            selectionActionMode?.finish()
            selectionActionMode = null
        }
    }

    var wasImeVisible by remember { mutableStateOf(isImeVisible) }
    LaunchedEffect(isImeVisible) {
        if (wasImeVisible && !isImeVisible && isFocused) {
            focusManager.clearFocus()
        }
        wasImeVisible = isImeVisible
    }

    DisposableEffect(Unit) {
        onDispose {
            selectionActionMode?.finish()
        }
    }

    val textStyle = TextStyle(
        color = androidx.compose.ui.graphics.Color(payload.style.color),
        fontSize = (PageUnitConverter.pointsToSp(payload.style.fontSize * uiScale, PageSize.A4) / density.fontScale).sp,
        fontWeight = if (payload.style.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (payload.style.isItalic) FontStyle.Italic else FontStyle.Normal,
        textAlign = when (payload.style.alignment) {
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.Right
            "JUSTIFY" -> TextAlign.Justify
            else -> TextAlign.Left
        }
    )

    val annotatedText = remember(hiddenInput.text, payload.spans, payload.style, uiScale, density.fontScale) {
        RichTextParser.buildRichText(
            text = hiddenInput.text.ifEmpty { " " },
            defaultStyle = payload.style,
            spans = payload.spans,
            uiScale = uiScale,
            fontScale = density.fontScale
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
                constraints = Constraints(maxWidth = canvasSize.width)
            )
        }
    }

    LaunchedEffect(measuredLayout) {
        latestLayout = measuredLayout
    }

    val contentHeightDp = with(density) {
        ((measuredLayout?.size?.height ?: 0).toDp() + 16.dp).coerceAtLeast(42.dp)
    }

    fun updateSelection(selection: TextRange) {
        hiddenInput = hiddenInput.copy(selection = selection)
        onSelectionChange(selection)
        if (!selection.collapsed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val contentRect = calculateSelectionRect(
                layout = latestLayout,
                selection = selection,
                containerInWindow = containerInWindow
            )
            if (contentRect != null) {
                if (selectionActionMode == null) {
                    val callback = TextSelectionActionModeCallback(
                        context = context,
                        getSelection = { hiddenInput.selection },
                        getText = { hiddenInput.text },
                        onSelectionUpdate = { newSelection -> updateSelection(newSelection) },
                        onCopy = onCopy,
                        onPaste = { onPaste(hiddenInput.selection.start) }
                    )
                    selectionActionModeCallback = callback
                    selectionActionMode = hostView.startActionMode(callback, ActionMode.TYPE_FLOATING)
                }
                selectionActionModeCallback?.contentRect = contentRect
                selectionActionMode?.invalidateContentRect()
            }
        } else {
            selectionActionMode?.finish()
            selectionActionMode = null
            selectionActionModeCallback = null
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
            .pointerInput(payload.text, isSelected) {
                detectTapGestures(
                    onTap = { offset ->
                        val layout = latestLayout ?: return@detectTapGestures
                        val position = layout.getOffsetForPosition(offset)
                        updateSelection(TextRange(position))
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    onLongPress = { offset ->
                        val layout = latestLayout ?: return@detectTapGestures
                        val position = layout.getOffsetForPosition(offset)
                        updateSelection(TextRange(position, payload.text.length))
                    }
                )
            }
            .pointerInput(payload.text) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        val layout = latestLayout ?: return@detectDragGesturesAfterLongPress
                        val position = layout.getOffsetForPosition(start)
                        updateSelection(TextRange(position))
                    },
                    onDrag = { change, _ ->
                        val layout = latestLayout ?: return@detectDragGesturesAfterLongPress
                        val start = hiddenInput.selection.start
                        val current = layout.getOffsetForPosition(change.position)
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
                
                drawPath(
                    path = layout.getPathForRange(minSelection, maxSelection),
                    color = androidx.compose.ui.graphics.Color(0x334F46E5)
                )
                
                // Draw teardrops
                val startCursorRect = layout.getCursorRect(minSelection)
                val endCursorRect = layout.getCursorRect(maxSelection)
                val primaryColor = androidx.compose.ui.graphics.Color(0xFF4F46E5)
                
                // Start handle
                drawLine(
                    color = primaryColor,
                    start = Offset(startCursorRect.left, startCursorRect.top),
                    end = Offset(startCursorRect.left, startCursorRect.bottom),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = Offset(startCursorRect.left, startCursorRect.bottom + 4.dp.toPx())
                )
                
                // End handle
                drawLine(
                    color = primaryColor,
                    start = Offset(endCursorRect.left, endCursorRect.top),
                    end = Offset(endCursorRect.left, endCursorRect.bottom),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(
                    color = primaryColor,
                    radius = 6.dp.toPx(),
                    center = Offset(endCursorRect.left, endCursorRect.bottom + 4.dp.toPx())
                )
            }

            drawText(layout)

            if (isSelected && isFocused && selection.collapsed) {
                val cursorRect = layout.getCursorRect(selection.start)
                drawLine(
                    color = androidx.compose.ui.graphics.Color(payload.style.color).copy(alpha = cursorAlpha),
                    start = Offset(cursorRect.left, cursorRect.top),
                    end = Offset(cursorRect.left, cursorRect.bottom),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        BasicTextField(
            value = hiddenInput,
            onValueChange = { newValue ->
                val previousSelection = hiddenInput.selection
                val oldText = hiddenInput.text
                val newText = newValue.text
                
                var newSpans = payload.spans.toList()
                
                val commonPrefixLen = oldText.commonPrefixWith(newText).length
                val commonSuffixLen = oldText.reversed().commonPrefixWith(newText.reversed()).length
                
                val overlap = (commonPrefixLen + commonSuffixLen) - min(oldText.length, newText.length)
                val safeSuffixLen = if (overlap > 0) commonSuffixLen - overlap else commonSuffixLen
                
                val replaceStart = commonPrefixLen
                val oldReplaceEnd = oldText.length - safeSuffixLen
                val newReplaceEnd = newText.length - safeSuffixLen
                
                val deletedLen = oldReplaceEnd - replaceStart
                val insertedLen = newReplaceEnd - replaceStart
                val delta = insertedLen - deletedLen
                
                if (delta != 0 || insertedLen > 0) {
                    newSpans = newSpans.mapNotNull { span ->
                        var start = span.start
                        var end = span.end
                        
                        if (end > replaceStart) {
                            if (end <= oldReplaceEnd) end = replaceStart
                            else end += delta
                        }
                        
                        if (start >= replaceStart) {
                            if (start < oldReplaceEnd) start = replaceStart + insertedLen
                            else start += delta
                        }
                        
                        if (start < end) span.copy(start = start, end = end) else null
                    }
                }
                
                if (insertedLen > 0) {
                    newSpans = newSpans + TextSpan(replaceStart, replaceStart + insertedLen, activeStyle)
                }
                
                hiddenInput = newValue
                onTextChange(newText, newSpans)
                onSelectionChange(newValue.selection)
                if (newValue.selection != previousSelection) {
                    updateSelection(newValue.selection)
                } else if (newValue.selection.collapsed) {
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
