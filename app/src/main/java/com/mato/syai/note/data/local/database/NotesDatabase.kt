package com.mato.syai.note.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [NoteEntity::class, MetadataEntity::class, SavedColorEntity::class, AIUpdateEntity::class],
    version = 4,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}