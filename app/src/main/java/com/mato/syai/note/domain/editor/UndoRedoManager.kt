package com.mato.syai.note.domain.editor

import com.google.gson.Gson
import com.mato.syai.note.domain.local.model.NoteContent
import java.util.ArrayDeque

class UndoRedoManager(
    private val gson: Gson
) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    fun push(content: NoteContent) {
        val snapshot = gson.toJson(content)
        if (undoStack.lastOrNull() == snapshot) return
        undoStack.addLast(snapshot)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(current: NoteContent): NoteContent? {
        if (undoStack.size <= 1) return null
        redoStack.addLast(gson.toJson(current))
        undoStack.removeLast()
        return gson.fromJson(undoStack.last(), NoteContent::class.java)
    }

    fun redo(current: NoteContent): NoteContent? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(gson.toJson(current))
        return gson.fromJson(redoStack.removeLast(), NoteContent::class.java)
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
