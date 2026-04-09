package com.mato.syai.note.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mato.syai.note.domain.local.model.ActiveTool
import com.mato.syai.note.domain.local.model.CustomObject
import com.mato.syai.note.domain.local.model.ObjectType
import com.mato.syai.note.domain.local.model.PageData
import com.mato.syai.note.domain.local.model.PageSize
import com.mato.syai.note.domain.local.model.Tool

@Composable
fun NotePage(
    pageIndex: Int,
    page: PageData,
    viewModel: NoteEditorViewModel
) {
    val activeTool by viewModel.currentTool.collectAsState()
    val focusedId by viewModel.focusedObjectId.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .aspectRatio(1 / page.pageSize.ratio)
            .pointerInput(activeTool) {
                detectTapGestures { offset ->
                    viewModel.handlePageTap(pageIndex, offset)
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color(page.backgroundColor)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Static Drawings (JSON -> Canvas)
            Canvas(modifier = Modifier.fillMaxSize()) {
                page.items.filter { it.type == ObjectType.DRAWING }.forEach {
                    renderStaticDrawing(it)
                }
            }

            // 2. Active Drawing Layer
            if (activeTool == ActiveTool.DRAW) {
                DrawingCanvas(pageIndex, viewModel, Modifier.fillMaxSize())
            }

            // 3. Text and Media Objects
            page.items.filter { it.type != ObjectType.DRAWING }.forEach { obj ->
                if (obj.type == ObjectType.TEXT) {
                    RenderTextBlock(pageIndex, obj, focusedId == obj.id, viewModel)
                }
            }
        }
    }
}

@Composable
fun DraggableObject(
    obj: CustomObject,
    pageIndex: Int,
    viewModel: NoteEditorViewModel,
    activeTool: Tool
) {
    Box(
        /*modifier = Modifier
            .offset(obj.offset.x.dp, obj.offset.y.dp)
            .pointerInput(activeTool) {
                if (activeTool == Tool.TEXT || activeTool == Tool.IMAGE) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newOffset = Offset(
                            obj.offset.x + dragAmount.x,
                            obj.offset.y + dragAmount.y
                        )
                        viewModel.updateObjectOffset(pageIndex, obj.id, newOffset)
                    }
                }
            }*/
    ) {
        when (obj.type) {
            ObjectType.TEXT -> RenderText(obj)
            ObjectType.DRAWING -> RenderDrawing(obj)
//            ObjectType.IMAGE -> RenderImage(obj)
            else -> {}
        }
    }
}

