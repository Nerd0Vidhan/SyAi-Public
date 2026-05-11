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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.mato.syai.note.domain.local.model.ChecklistPayload
import com.mato.syai.note.domain.local.model.BrushStyle
import com.mato.syai.note.domain.local.model.DrawingPayload
import com.mato.syai.note.domain.local.model.ImagePayload
import com.mato.syai.note.domain.local.model.LinearContentEntry
import com.mato.syai.note.domain.local.model.NoteContent
import com.mato.syai.note.domain.local.model.NoteObject
import com.mato.syai.note.domain.local.model.ObjectType
import com.mato.syai.note.domain.local.model.TextPayload
import com.mato.syai.note.domain.local.model.LinearTextPayload
import com.mato.syai.note.domain.local.model.ListMarker
import com.mato.syai.note.domain.local.model.ListPayload
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
            val renderables = pageData.renderableItems
            val linearTexts = pageData.linearContent.filter { it.type == ObjectType.LINEAR_TEXT }
            
            // Combine and sort by layer
            val allDrawables = (renderables.map { it to it.layer } + linearTexts.map { it to it.layer }).sortedBy { it.second }
            
            allDrawables.forEach { (item, _) ->
                when (item) {
                    is NoteObject -> {
                        when (item.type) {
                            ObjectType.TEXT -> drawText(canvas, item)
                            ObjectType.DRAWING -> drawDrawing(canvas, item)
                            ObjectType.IMAGE -> drawImage(canvas, item)
                            ObjectType.CHECKLIST -> drawChecklist(canvas, item)
                            ObjectType.LIST -> drawList(canvas, item)
                            else -> {}
                        }
                    }
                    is LinearContentEntry -> {
                        drawLinearText(canvas, item)
                    }
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
        val spannable = SpannableString(payload.text)
        
        payload.spans.forEach { span ->
            val start = Math.max(0, Math.min(span.start, payload.text.length))
            val end = Math.max(0, Math.min(span.end, payload.text.length))
            if (start < end) {
                spannable.setSpan(ForegroundColorSpan(span.style.color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(AbsoluteSizeSpan(span.style.fontSize.toInt(), true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val type = if (span.style.isBold && span.style.isItalic) android.graphics.Typeface.BOLD_ITALIC
                           else if (span.style.isBold) android.graphics.Typeface.BOLD
                           else if (span.style.isItalic) android.graphics.Typeface.ITALIC
                           else android.graphics.Typeface.NORMAL
                spannable.setSpan(StyleSpan(type), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
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
            spannable,
            0,
            spannable.length,
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

    private fun drawLinearText(canvas: Canvas, entry: LinearContentEntry) {
        val spannable = SpannableString(entry.value)
        
        entry.spans.forEach { span ->
            val start = Math.max(0, Math.min(span.start, entry.value.length))
            val end = Math.max(0, Math.min(span.end, entry.value.length))
            if (start < end) {
                spannable.setSpan(ForegroundColorSpan(span.style.color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(AbsoluteSizeSpan(span.style.fontSize.toInt(), true), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val type = if (span.style.isBold && span.style.isItalic) android.graphics.Typeface.BOLD_ITALIC
                           else if (span.style.isBold) android.graphics.Typeface.BOLD
                           else if (span.style.isItalic) android.graphics.Typeface.ITALIC
                           else android.graphics.Typeface.NORMAL
                spannable.setSpan(StyleSpan(type), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
        val style = entry.style

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
            spannable,
            0,
            spannable.length,
            paint,
            entry.bounds.width.toInt()
        )
            .setAlignment(alignment)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)

        canvas.save()
        canvas.translate(entry.transform.x, entry.transform.y)
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
                BrushStyle.ERASER -> 255
            }
            paint.strokeWidth = when (stroke.brushStyle) {
                BrushStyle.PENCIL -> stroke.width * 0.8f
                BrushStyle.MARKER -> stroke.width * 1.2f
                BrushStyle.HIGHLIGHTER -> stroke.width * 1.5f
                BrushStyle.PEN -> stroke.width
                BrushStyle.ERASER -> stroke.width
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

    private fun drawList(canvas: Canvas, obj: NoteObject) {
        val payload = obj.payload as? ListPayload ?: return
        val paint = TextPaint().apply { 
            isAntiAlias = true
        }
        var currentY = obj.transform.y

        payload.items.forEachIndexed { index, item ->
            val marker = when (payload.style) {
                ListMarker.BULLET -> "• "
                ListMarker.DASH -> "- "
                ListMarker.CHECK -> "✓ "
                ListMarker.STAR -> "★ "
                ListMarker.NUMBER -> "${index + 1}. "
                ListMarker.ROMAN -> "${toRoman(index + 1)}. "
                else -> "• "
            }
            
            paint.textSize = item.style.fontSize
            paint.color = item.style.color
            
            val text = marker + item.text
            val builder = StaticLayout.Builder.obtain(
                text,
                0,
                text.length,
                paint,
                obj.bounds.width.toInt().coerceAtLeast(1)
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1f)
                .setIncludePad(true)
            
            val layout = builder.build()
            canvas.save()
            canvas.translate(obj.transform.x, currentY)
            layout.draw(canvas)
            canvas.restore()
            
            currentY += layout.height + 4f
        }
    }

    private fun toRoman(n: Int): String {
        val map = mapOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I")
        var num = n
        val res = StringBuilder()
        map.keys.sortedDescending().forEach { key ->
            while (num >= key) {
                res.append(map[key])
                num -= key
            }
        }
        return res.toString()
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

        val renderables = page.renderableItems
        val linearTexts = page.linearContent.filter { it.type == ObjectType.LINEAR_TEXT }
        
        val allDrawables = (renderables.map { it to it.layer } + linearTexts.map { it to it.layer }).sortedBy { it.second }
        
        allDrawables.forEach { (item, _) ->
            when (item) {
                is NoteObject -> {
                    when (item.type) {
                        ObjectType.TEXT -> drawText(canvas, item)
                        ObjectType.DRAWING -> drawDrawing(canvas, item)
                        ObjectType.IMAGE -> drawImage(canvas, item)
                        ObjectType.CHECKLIST -> drawChecklist(canvas, item)
                        ObjectType.LIST -> drawList(canvas, item)
                        else -> {}
                    }
                }
                is LinearContentEntry -> {
                    drawLinearText(canvas, item)
                }
            }
        }

        return bitmap
    }
}
