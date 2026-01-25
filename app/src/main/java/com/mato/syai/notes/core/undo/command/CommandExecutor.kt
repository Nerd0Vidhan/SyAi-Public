package com.mato.syai.notes.core.undo.command

import com.mato.syai.notes.core.undo.stack.RedoStack
import com.mato.syai.notes.core.undo.stack.UndoStack

class CommandExecutor<T>(
    private val undoStack: UndoStack<T>,
    private val redoStack: RedoStack<T>
) {

    fun execute(
        current: T,
        command: Command<T>
    ): T {
        val newState = command.execute(current)
        undoStack.push(command)
        redoStack.clear()
        return newState
    }

    fun undo(current: T): T {
        val command = undoStack.pop() ?: return current
        val previousState = command.undo(current)
        redoStack.push(command)
        return previousState
    }

    fun redo(current: T): T {
        val command = redoStack.pop() ?: return current
        val newState = command.execute(current)
        undoStack.push(command)
        return newState
    }
}
