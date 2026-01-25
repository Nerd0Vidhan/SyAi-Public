package com.mato.syai.notes.core.undo.stack

import com.mato.syai.notes.core.undo.command.Command

class RedoStack<T> {
    private val stack = ArrayDeque<Command<T>>()

    fun push(command: Command<T>) {
        stack.addLast(command)
    }

    fun pop(): Command<T>? =
        stack.removeLastOrNull()

    fun clear() = stack.clear()
}
