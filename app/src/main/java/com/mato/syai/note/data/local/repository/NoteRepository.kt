package com.mato.syai.note.data.local.repository

import android.os.Environment
import com.google.gson.Gson
import com.mato.syai.note.data.local.database.MetadataEntity
import com.mato.syai.note.data.local.database.NoteDao
import com.mato.syai.note.data.local.database.NoteEntity
import com.mato.syai.note.data.local.security.CryptoManager
import com.mato.syai.note.domain.local.model.Note
import com.mato.syai.note.domain.local.model.NoteContent
import com.mato.syai.note.domain.local.model.NoteMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val dao: NoteDao,
    private val cryptoManager: CryptoManager,
    private val gson: Gson
) {

    val allNotes: Flow<List<Note>> = dao.getNotesWithMetadata().map { list ->
        list.map { item ->
            Note(
                id = item.note.noteId,
                filePath = item.note.filePath,
                title = item.note.title,
                folderName = item.note.folderName,
                lastModified = item.note.lastModified,
                isFavorite = item.note.isFavorite,
                metadata = NoteMetadata(
                    textSize = item.metadata?.textSize ?: 12f,
                    colorHex = item.metadata?.colorHex ?: 0xFFFFFFFF.toInt(),
                )
            )
        }
    }

    suspend fun getNoteById(id: Long): NoteEntity? = dao.getNoteById(id)

    suspend fun createNewNote(title: String, folder: String): Long = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(root, "SyAi/$folder").apply { if (!exists()) mkdirs() }
        val file = File(dir, "$title.dpn")

        // Ensure file exists physically
        if (!file.exists()) file.createNewFile()

        val entity = NoteEntity(
            filePath = file.absolutePath,
            title = title,
            folderName = folder,
            lastModified = System.currentTimeMillis(),
            preview = file.readText().take(150),
            isFavorite = false
        )

        val meta = MetadataEntity(noteId = 0, textSize = 12f)
        dao.insertNoteWithMetadata(entity, meta)
    }

    suspend fun syncFileSystem() = withContext(Dispatchers.IO) {
        try {
            // Targeted directories are more reliable than scanning the whole Root
            val roots = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                // You can still add the root, but be aware of performance
                Environment.getExternalStorageDirectory()
            )

            val diskFiles = mutableListOf<File>()

            roots.forEach { root ->
                if (root.exists()) {
                    val files = root.walkTopDown()
                        .onFail { file, _ -> println("Access Denied to: ${file.absolutePath}") }
                        .filter { it.isFile && it.extension == "dpn" }
                        .toList()
                    diskFiles.addAll(files)
                }
            }

            val diskPaths = diskFiles.map { it.absolutePath }.toSet()
            val dbPaths = dao.getAllStoredPaths().toSet()

            // Remove deleted
            (dbPaths - diskPaths).forEach { dao.deleteByPath(it) }

            // Add/Update new
            diskFiles.forEach { file ->
                val existingId = dao.getIdByPath(file.absolutePath)
                if (existingId == null) {
                    val noteEntity = NoteEntity(
                        filePath = file.absolutePath,
                        title = file.nameWithoutExtension,
                        folderName = file.parentFile?.name ?: "Root",
                        lastModified = file.lastModified(),
                        preview = file.readText().take(150),
                        isFavorite = false
                    )
                    val defaultMetadata = MetadataEntity(
                        noteId = 0, // Room will ignore this 0 because of the Transaction logic
                        textSize = 16f,
                        colorHex = 0xFFF8E0C3.toInt(),
                    )

                    // Use the transaction method we created
                    dao.insertNoteWithMetadata(noteEntity, defaultMetadata)
//                    val newId = dao.insertNote(noteEntity)
//                    dao.insertMetadata(MetadataEntity(newId, 16f, 0xFFF8E0C3.toInt(), false))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        dao.deleteByPath(note.filePath)
        val file = File(note.filePath)
        if (file.exists()) file.delete()
    }

    suspend fun saveNoteContent(noteId: Long, content: NoteContent) = withContext(Dispatchers.IO) {
        val note = dao.getNoteById(noteId) ?: return@withContext
        val json = gson.toJson(content)
        val (iv, encrypted) = cryptoManager.encrypt(json.toByteArray())
        File(note.filePath).writeBytes(iv + encrypted)
    }

    suspend fun loadNoteContent(noteId: Long): NoteContent? = withContext(Dispatchers.IO) {
        val note = dao.getNoteById(noteId) ?: return@withContext null
        val file = File(note.filePath)
        if (!file.exists() || file.length() < 13) return@withContext null

        val bytes = file.readBytes()
        val iv = bytes.take(12).toByteArray()
        val encrypted = bytes.drop(12).toByteArray()
        val decrypted = cryptoManager.decrypt(iv, encrypted).decodeToString()
        gson.fromJson(decrypted, NoteContent::class.java)
    }

    suspend fun updateTitle(noteId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val note = dao.getNoteById(noteId) ?: return@withContext
        val oldFile = File(note.filePath)
        val newFile = File(oldFile.parent, "$newTitle.dpn")
        if (oldFile.renameTo(newFile)) {
            dao.updateNotePathAndTitle(noteId, newFile.absolutePath, newTitle)
        }
    }
}