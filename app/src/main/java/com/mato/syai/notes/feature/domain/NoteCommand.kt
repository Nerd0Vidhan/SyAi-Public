package com.mato.syai.notes.feature.domain

import com.mato.syai.notes.core.undo.command.Command
import com.mato.syai.notes.feature.domain.model.Note

/**
 * Thin wrapper to carry any Note-related undoable command
 */
@JvmInline
value class NoteCommand(
    val command: Command<Note>
)
