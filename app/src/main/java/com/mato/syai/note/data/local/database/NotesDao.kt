package com.mato.syai.note.data.local.database

import androidx.room.*
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

    @Query("DELETE FROM notes_path WHERE noteId = :noteId")
    suspend fun deleteById(noteId: Long)

    @Query("SELECT filePath FROM notes_path")
    suspend fun getAllStoredPaths(): List<String>

    @Query("SELECT * FROM notes_path WHERE noteId = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Query("SELECT * FROM note_metadata WHERE noteId = :noteId LIMIT 1")
    suspend fun getMetadataByNoteId(noteId: Long): MetadataEntity?

    @Query("UPDATE notes_path SET filePath = :absolutePath, title = :newTitle, lastModified = :lastModified WHERE noteId = :noteId")
    suspend fun updateNotePathAndTitle(noteId: Long, absolutePath: String, newTitle: String, lastModified: Long)

    @Query("UPDATE notes_path SET title = :title, lastModified = :lastModified WHERE noteId = :noteId")
    suspend fun updateTitle(noteId: Long, title: String, lastModified: Long)

    @Query("UPDATE notes_path SET lastModified = :lastModified WHERE noteId = :noteId")
    suspend fun updateLastModified(noteId: Long, lastModified: Long)

    @Query("UPDATE notes_path SET isFavorite = :isFavorite WHERE noteId = :noteId")
    suspend fun updateFavorite(noteId: Long, isFavorite: Boolean)

    @Query("UPDATE note_metadata SET textSize = :textSize, colorHex = :colorHex WHERE noteId = :noteId")
    suspend fun updateMetadata(noteId: Long, textSize: Float, colorHex: Int)

    @Query("UPDATE notes_path SET filePath = :newPath, folderName = :folderName WHERE noteId = :noteId")
    suspend fun updateFolder(noteId: Long, newPath: String, folderName: String)

    @Query("SELECT EXISTS(SELECT 1 FROM notes_path WHERE noteId = :noteId)")
    suspend fun exists(noteId: Long): Boolean

    @Transaction
    suspend fun insertNoteWithMetadata(note: NoteEntity, metadata: MetadataEntity): Long {
        val id = insertNote(note)
        insertMetadata(metadata.copy(noteId = id))
        return id
    }

    @Query("UPDATE notes_path SET imagePreview = :previewId WHERE noteId = :noteId")
    suspend fun savePreviewId(noteId: Long, previewId: String?)

    @Query("SELECT imagePreview from notes_path WHERE noteId = :noteId")
    suspend fun fetchPreviewId(noteId: Long): String?

    @Query("SELECT * FROM saved_colors ORDER BY timestamp DESC LIMIT 15")
    fun getSavedColors(): Flow<List<SavedColorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedColor(color: SavedColorEntity)

    @Query("DELETE FROM saved_colors WHERE id = :id")
    suspend fun deleteSavedColorById(id: Long)

    @Query("DELETE FROM saved_colors WHERE id NOT IN (SELECT id FROM saved_colors ORDER BY timestamp DESC LIMIT 15)")
    suspend fun deleteOldSavedColors()

    @Query("SELECT isSeen FROM ai_seen_updates WHERE updateId = :updateId LIMIT 1")
    suspend fun isAIUpdateSeen(updateId: String): Boolean?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAIUpdate(aiUpdate: AIUpdateEntity)
}