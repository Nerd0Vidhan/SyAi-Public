package com.mato.syai.note.data.local.database

import androidx.room.*
import com.mato.syai.note.domain.local.model.BackGroundType
import com.mato.syai.note.domain.local.model.PageDimensions
import com.mato.syai.note.domain.local.model.PagePadding
import com.mato.syai.note.domain.local.model.PageSize
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notes_path", indices = [Index(value = ["filePath"], unique = true)])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val noteId: Long = 0,
    val filePath: String,
    val title: String,
    val folderName: String,
    val lastModified: Long,
    val preview: String = "",
    val imagePreview:String?=null,
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "note_metadata",
    foreignKeys = [ForeignKey(
        entity = NoteEntity::class,
        parentColumns = ["noteId"],
        childColumns = ["noteId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class MetadataEntity(
    @PrimaryKey val noteId: Long,
    val textSize: Float,
    val background : String?=null,
    val colorHex : Int = 0xFFF8E0C3.toInt(),
    val defaultTextSize: Float = 12f,
    val cursorColor: Int = 0xFF0D0127.toInt(),
    val backgroundType: String = BackGroundType.SOLID.type,
    val totalPages: Int = 1,
    val pageSize: PageSize = PageSize.A4
)

// Helper POJO for Room to join tables
data class NoteWithMetadata(
    @Embedded val note: NoteEntity,
    @Relation(parentColumn = "noteId", entityColumn = "noteId")
    val metadata: MetadataEntity?
)