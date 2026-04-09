package com.mato.syai.note.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow


@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes_path ORDER BY lastModified DESC")
    fun getNotesWithMetadata(): Flow<List<NoteWithMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: MetadataEntity)

    @Query("SELECT noteId FROM notes_path WHERE filePath = :path")
    suspend fun getIdByPath(path: String): Long?

    @Query("DELETE FROM notes_path WHERE filePath = :path")
    suspend fun deleteByPath(path: String)

    @Query("SELECT filePath FROM notes_path")
    suspend fun getAllStoredPaths(): List<String>

    @Query("SELECT * FROM notes_path WHERE noteId = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("UPDATE notes_path SET filePath = :absolutePath, title = :newTitle WHERE noteId = :noteId")
    suspend fun updateNotePathAndTitle(noteId: Long, absolutePath: String, newTitle: String)

    @Transaction
    suspend fun insertNoteWithMetadata(note: NoteEntity, metadata: MetadataEntity): Long {
        val id = insertNote(note)
        // Ensure we use the freshly generated ID for the child record
        insertMetadata(metadata.copy(noteId = id))
        return id
    }
}