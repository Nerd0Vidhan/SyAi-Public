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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
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
import kotlin.math.max
import kotlin.math.min

@Composable
fun ManualLinearTextEditor(
    payload: TextPayload,
    widthPoints: Float,
    uiScale: Float = 1f,
    isSelected: Boolean,
    onTextChange: (String) -> Unit,
    onSelectionChange: (Int) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val hostView = LocalView.current
    val density = LocalDensity.current

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

    DisposableEffect(Unit) {
        onDispose {
            selectionActionMode?.finish()
        }
    }

    val textStyle = TextStyle(
        color = androidx.compose.ui.graphics.Color(payload.style.color),
        fontSize = PageUnitConverter.pointsToSp(payload.style.fontSize * uiScale, PageSize.A4).sp,
        fontWeight = if (payload.style.isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (payload.style.isItalic) FontStyle.Italic else FontStyle.Normal,
        textAlign = when (payload.style.alignment) {
            "CENTER" -> TextAlign.Center
            "RIGHT" -> TextAlign.Right
            "JUSTIFY" -> TextAlign.Justify
            else -> TextAlign.Left
        }
    )

    val measuredLayout = remember(hiddenInput.text, hiddenInput.selection, textStyle, canvasSize) {
        if (canvasSize.width <= 0) {
            null
        } else {
            textMeasurer.measure(
                text = hiddenInput.text.ifEmpty { " " },
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
        onSelectionChange(selection.end)
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
                        onSelectionUpdate = { newSelection -> updateSelection(newSelection) }
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
            .width(PageUnitConverter.pointsToDp(widthPoints * uiScale, PageSize.A4).dp)
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
                drawPath(
                    path = layout.getPathForRange(selection.start, selection.end),
                    color = androidx.compose.ui.graphics.Color(0x334F46E5)
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
            onValueChange = {
                val previousSelection = hiddenInput.selection
                hiddenInput = it
                onTextChange(it.text)
                onSelectionChange(it.selection.end)
                if (it.selection != previousSelection) {
                    updateSelection(it.selection)
                } else if (it.selection.collapsed) {
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
    private val onSelectionUpdate: (TextRange) -> Unit
) : ActionMode.Callback2() {

    var contentRect: Rect = Rect()

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.add(0, android.R.id.copy, 0, android.R.string.copy)
        menu?.add(0, android.R.id.selectAll, 1, android.R.string.selectAll)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.copy -> {
                val selection = getSelection()
                val text = getText()
                if (!selection.collapsed) {
                    val selectedText = text.substring(selection.start, selection.end)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("selected_text", selectedText))
                }
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
    val endIndex = (selection.end - 1).coerceAtLeast(selection.start)
    val startBox = layout.getBoundingBox(selection.start)
    val endBox = layout.getBoundingBox(endIndex)
    return Rect(
        (containerInWindow.x + min(startBox.left, endBox.left)).toInt(),
        (containerInWindow.y + min(startBox.top, endBox.top)).toInt(),
        (containerInWindow.x + max(startBox.right, endBox.right)).toInt(),
        (containerInWindow.y + max(startBox.bottom, endBox.bottom)).toInt()
    )
}
