package com.mato.syai.note.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.mato.syai.note.data.local.parser.PdfExporter
import com.mato.syai.note.domain.local.model.NoteContent
import com.mato.syai.note.domain.local.model.ObjectType
import java.io.File
import java.io.FileOutputStream

object ThumbnailUtils {

    fun generateAndSave(context: Context, content: NoteContent): String? {
        val bitmap = PdfExporter(context).renderFirstPageToBitmap(content) ?: return null
        return saveThumbnail(context, bitmap)
    }

    private fun generateFileName(): String {
        val number = (System.currentTimeMillis() % 1_000_000_000)
        return "thumbnail_$number.jpg"
    }

    private fun saveThumbnail(context: Context, bitmap: Bitmap): String {
        val fileName = generateFileName()
        val file = File(context.filesDir, fileName)

        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, it)
        }

        return fileName
    }

    fun loadThumbnail(context: Context, fileName: String): Bitmap? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}