package com.mato.syai.note.data.local.repository

import android.os.Environment
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.mato.syai.note.data.local.database.*
import com.mato.syai.note.data.local.security.CryptoManager
import com.mato.syai.note.domain.local.model.*
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
                    textSize = item.metadata?.textSize ?: 16f,
                    colorHex = item.metadata?.colorHex ?: 0xFF000000.toInt(),
                )
            )
        }
    }

    suspend fun getNoteById(id: Long): NoteEntity? = dao.getNoteById(id)

    suspend fun createNewNote(title: String, folder: String): Long = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(root, "SyAi/$folder").apply { if (!exists()) mkdirs() }
        val file = File(dir, "$title.dpn")

        if (!file.exists()) file.createNewFile()

        val entity = NoteEntity(
            filePath = file.absolutePath,
            title = title,
            folderName = folder,
            lastModified = System.currentTimeMillis(),
            preview = "",
            isFavorite = false
        )

        val noteId = dao.insertNote(entity)
        dao.insertMetadata(MetadataEntity(noteId = noteId, textSize = 16f))

        saveNoteContent(noteId, createDefaultContent())

        noteId
    }

    suspend fun syncFileSystem() = withContext(Dispatchers.IO) {
        try {
            val roots = listOf(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
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

            (dbPaths - diskPaths).forEach { dao.deleteByPath(it) }

            diskFiles.forEach { file ->
                val existingId = dao.getIdByPath(file.absolutePath)
                if (existingId == null) {
                    val noteEntity = NoteEntity(
                        filePath = file.absolutePath,
                        title = file.nameWithoutExtension,
                        folderName = file.parentFile?.name ?: "Root",
                        lastModified = file.lastModified(),
                        preview = "",
                        isFavorite = false
                    )

                    val defaultMetadata = MetadataEntity(
                        noteId = 0,
                        textSize = 16f,
                        colorHex = 0xFF000000.toInt(),
                    )

                    dao.insertNoteWithMetadata(noteEntity, defaultMetadata)
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
        dao.updateLastModified(noteId, System.currentTimeMillis())
    }

    suspend fun loadNoteContent(noteId: Long): NoteContent = withContext(Dispatchers.IO) {
        val note = dao.getNoteById(noteId) ?: return@withContext createDefaultContent()
        val file = File(note.filePath)

        if (!file.exists() || file.length() < 13) {
            return@withContext createDefaultContent()
        }

        return@withContext try {
            val bytes = file.readBytes()
            val iv = bytes.take(12).toByteArray()
            val encrypted = bytes.drop(12).toByteArray()
            val decrypted = cryptoManager.decrypt(iv, encrypted).decodeToString()
            val content = gson.fromJson(decrypted, NoteContent::class.java)?.normalizeForCurrentSchema()
                ?: createDefaultContent()
            Log.d("Note Content", "Note Data : $content")
            content
        } catch (e: Exception) {
            e.printStackTrace()
            createDefaultContent()
        }
    }

    suspend fun updateTitle(noteId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val note = dao.getNoteById(noteId) ?: return@withContext
        val oldFile = File(note.filePath)
        val newFile = File(oldFile.parent, "$newTitle.dpn")
        if (oldFile.renameTo(newFile)) {
            dao.updateNotePathAndTitle(
                noteId = noteId,
                absolutePath = newFile.absolutePath,
                newTitle = newTitle,
                lastModified = System.currentTimeMillis()
            )
        }
    }

    private fun createDefaultContent(): NoteContent {
        return NoteContent(
            schemaVersion = NOTE_SCHEMA_VERSION,
            pages = mutableListOf(
                PageData(pageNo = 0).apply { normalizeFromLegacyItems() }
            )
        )
    }

    suspend fun updateFavorite(id: Long, bool: Boolean) {
        dao.updateFavorite(id, bool)
    }

    suspend fun updateNoteMetadata(
        noteId: Long,
        newTitle: String,
        folder: String,
        textSize: Float,
        color: Int
    ) = withContext(Dispatchers.IO) {
        val note = dao.getNoteById(noteId) ?: return@withContext

        val oldFile = File(note.filePath)
        val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val newDir = File(root, "SyAi/$folder").apply { if (!exists()) mkdirs() }
        val newFile = File(newDir, "$newTitle.dpn")

        if (oldFile.absolutePath != newFile.absolutePath) {
            if (oldFile.renameTo(newFile)) {
                dao.updateNotePathAndTitle(noteId, newFile.absolutePath, newTitle, System.currentTimeMillis())
                dao.updateFolder(noteId, newFile.absolutePath, folder)
            }
        } else {
            dao.updateTitle(noteId, newTitle, System.currentTimeMillis())
        }
        dao.updateMetadata(noteId, textSize, color)
    }
}
