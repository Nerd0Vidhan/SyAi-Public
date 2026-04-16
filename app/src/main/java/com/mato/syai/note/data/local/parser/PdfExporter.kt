package com.mato.syai.note.data.local.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.mato.syai.note.domain.local.model.ChecklistPayload
import com.mato.syai.note.domain.local.model.BrushStyle
import com.mato.syai.note.domain.local.model.DrawingPayload
import com.mato.syai.note.domain.local.model.ImagePayload
import com.mato.syai.note.domain.local.model.NoteContent
import com.mato.syai.note.domain.local.model.NoteObject
import com.mato.syai.note.domain.local.model.ObjectType
import com.mato.syai.note.domain.local.model.TextPayload
import java.io.File
import java.io.FileOutputStream

class PdfExporter(private val context: Context) {

    fun export(
        bitmaps: List<Bitmap>,
        fileName: String
    ): File {

        val pdf = PdfDocument()

        bitmaps.forEachIndexed { index, bitmap ->

            val pageInfo = PdfDocument.PageInfo.Builder(
                bitmap.width,
                bitmap.height,
                index + 1
            ).create()

            val page = pdf.startPage(pageInfo)

            val canvas: Canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            pdf.finishPage(page)
        }

        val file = File(
            context.getExternalFilesDir(null),
            "$fileName.pdf"
        )

        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        return file
    }


    fun exportFromData(content: NoteContent, fileName: String): File? {
        val pdf = PdfDocument()

        content.pages.forEachIndexed { index, pageData ->
            val pageWidth = pageData.widthPoints.toInt()
            val pageHeight = pageData.heightPoints.toInt()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            // Paint the background
            canvas.drawColor(pageData.backgroundColor)

            // Render each object based on its type
            pageData.renderableItems.forEach { obj ->
                when (obj.type) {
                    ObjectType.TEXT -> drawText(canvas, obj)
                    ObjectType.LINEAR_TEXT -> drawText(canvas, obj)
                    ObjectType.DRAWING -> drawDrawing(canvas, obj)
                    ObjectType.IMAGE -> drawImage(canvas, obj)
                    ObjectType.CHECKLIST -> drawChecklist(canvas, obj)
                    else -> {}
                }
            }
            pdf.finishPage(page)
        }

        return try {
            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val dir = File(root, "SyAi").apply { if (!exists()) mkdirs() }
            val file = File(dir, "$fileName.pdf")
            pdf.writeTo(FileOutputStream(file))
            pdf.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun drawText(canvas: Canvas, obj: NoteObject) {
        val payload = obj.payload as? TextPayload ?: return
        val style = payload.style

        val paint = TextPaint().apply {
            color = style.color
            textSize = style.fontSize
            isFakeBoldText = style.isBold
            textSkewX = if (style.isItalic) -0.25f else 0f
            isUnderlineText = style.isUnderline
        }

        val alignment = when (style.alignment) {
            "CENTER" -> Layout.Alignment.ALIGN_CENTER
            "RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_NORMAL
        }

        val builder = StaticLayout.Builder.obtain(
            payload.text,
            0,
            payload.text.length,
            paint,
            obj.bounds.width.toInt()
        )
            .setAlignment(alignment)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)

        canvas.save()
        canvas.translate(obj.transform.x, obj.transform.y)
        builder.build().draw(canvas)
        canvas.restore()
    }

    private fun drawDrawing(canvas: Canvas, obj: NoteObject) {
        val payload = obj.payload as? DrawingPayload ?: return
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        payload.strokes.forEach { stroke ->
            paint.color = stroke.color
            paint.alpha = when (stroke.brushStyle) {
                BrushStyle.PENCIL -> 170
                BrushStyle.MARKER -> 230
                BrushStyle.HIGHLIGHTER -> 90
                BrushStyle.PEN -> 255
            }
            paint.strokeWidth = when (stroke.brushStyle) {
                BrushStyle.PENCIL -> stroke.width * 0.8f
                BrushStyle.MARKER -> stroke.width * 1.2f
                BrushStyle.HIGHLIGHTER -> stroke.width * 1.5f
                BrushStyle.PEN -> stroke.width
            }
            val path = Path()
            if (stroke.points.isNotEmpty()) {
                path.moveTo(stroke.points[0].x, stroke.points[0].y)
                for (i in 1 until stroke.points.size) {
                    path.lineTo(stroke.points[i].x, stroke.points[i].y)
                }
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawImage(canvas: Canvas, obj: NoteObject) {
        val payload = obj.payload as? ImagePayload ?: return
        try {
            // Note: In production, use a more robust image loading (Coil/Glide)
            // to fetch the bitmap before starting the PDF export.
            val inputStream =
                context.contentResolver.openInputStream(android.net.Uri.parse(payload.uri))
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val rect = RectF(
                obj.transform.x, obj.transform.y,
                obj.transform.x + obj.bounds.width,
                obj.transform.y + obj.bounds.height
            )
            canvas.drawBitmap(bitmap, null, rect, null)
        } catch (e: Exception) { /* Handle missing image */
        }
    }

    private fun drawChecklist(canvas: Canvas, obj: NoteObject) {
        val payload = obj.payload as? ChecklistPayload ?: return
        val paint = Paint().apply { textSize = 14f; color = Color.BLACK }
        var currentY = obj.transform.y

        payload.items.forEach { item ->
            val boxChar = if (item.isChecked) "☑ " else "☐ "
            canvas.drawText(boxChar + item.text, obj.transform.x, currentY, paint)
            currentY += 24f
        }
    }

    public fun renderFirstPageToBitmap(content: NoteContent): Bitmap? {
        val page = content.pages.firstOrNull() ?: return null

        val bitmap = Bitmap.createBitmap(
            page.widthPoints.toInt(),
            page.heightPoints.toInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        // draw background
        canvas.drawColor(page.backgroundColor)

        // reuse SAME rendering logic as PdfExporter
        page.renderableItems.forEach { obj ->
            when (obj.type) {
                ObjectType.TEXT,
                ObjectType.LINEAR_TEXT -> drawText(canvas, obj)

                ObjectType.DRAWING -> drawDrawing(canvas, obj)
                ObjectType.IMAGE -> drawImage(canvas, obj)
                ObjectType.CHECKLIST -> drawChecklist(canvas, obj)
                else -> {}
            }
        }

        return bitmap
    }
}
