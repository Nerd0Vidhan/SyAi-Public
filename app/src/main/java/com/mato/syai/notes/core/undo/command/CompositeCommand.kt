package com.mato.syai.notes.core.undo.command

class CompositeCommand<T>(
    private val commands: List<Command<T>>
) : Command<T> {

    override fun execute(current: T): T =
        commands.fold(current) { acc, command ->
            command.execute(acc)
        }

    override fun undo(current: T): T =
        commands.asReversed().fold(current) { acc, command ->
            command.undo(acc)
        }
}
