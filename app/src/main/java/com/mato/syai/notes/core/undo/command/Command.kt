package com.mato.syai.notes.core.undo.command

interface Command<T> {
    fun execute(current: T): T
    fun undo(current: T): T
}