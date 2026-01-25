package com.mato.syai.notes.core.undo.stack

import com.mato.syai.notes.core.undo.command.Command

class UndoStack<T>(
    private val maxSize: Int = 100
) {
    private val stack = ArrayDeque<Command<T>>()

    fun push(command: Command<T>) {
        if (stack.size >= maxSize) {
            stack.removeFirst()
        }
        stack.addLast(command)
    }

    fun pop(): Command<T>? =
        stack.removeLastOrNull()

    fun clear() = stack.clear()

    fun isEmpty(): Boolean = stack.isEmpty()
}
