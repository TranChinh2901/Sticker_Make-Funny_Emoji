package com.stickermaker.funnyemoji.data

import android.content.Context
import android.graphics.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.UUID

@Serializable
data class StickerLayer(val id: String = UUID.randomUUID().toString(), val kind: String,
    val value: String, val x: Float = .5f, val y: Float = .5f, val scale: Float = 1f,
    val color: Long = 0xFF1F2937, val outline: Float = 0f)
@Serializable
data class InkPoint(val x: Float, val y: Float)
@Serializable
data class InkStroke(val points: List<InkPoint>, val color: Long, val width: Float)
@Serializable
data class StickerDocument(val background: Long = 0, val pattern: Boolean = false,
    val layers: List<StickerLayer> = emptyList(), val strokes: List<InkStroke> = emptyList()) {
    val hasContent get() = background != 0L || pattern || layers.isNotEmpty() || strokes.isNotEmpty()
}

/** Coordinates are normalized, so the preview and 512px PNG share the same renderer. */
internal fun renderSticker(context: Context, document: StickerDocument): Bitmap {
    val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(document.background.toInt())
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    if (document.pattern) {
        paint.color = 0x22808080
        for (x in 0..16) for (y in 0..16) canvas.drawCircle(x * 32f, y * 32f, 3f, paint)
    }
    document.layers.forEach { layer ->
        canvas.save()
        canvas.translate(layer.x * 512, layer.y * 512)
        canvas.scale(layer.scale, layer.scale)
        paint.reset(); paint.isAntiAlias = true; paint.isFilterBitmap = true
        if (layer.kind == "image") {
            val file = File(context.filesDir, "imports/${layer.value}")
            val source = BitmapFactory.decodeFile(file.path)
            if (source != null) {
                val factor = 340f / maxOf(source.width, source.height)
                val rect = RectF(-source.width * factor / 2, -source.height * factor / 2,
                    source.width * factor / 2, source.height * factor / 2)
                if (layer.outline > 0) {
                    paint.colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                    repeat(24) { index ->
                        val angle = index * Math.PI / 12
                        canvas.save()
                        canvas.translate((kotlin.math.cos(angle) * layer.outline).toFloat(), (kotlin.math.sin(angle) * layer.outline).toFloat())
                        canvas.drawBitmap(source, null, rect, paint); canvas.restore()
                    }
                    paint.colorFilter = null
                }
                canvas.drawBitmap(source, null, rect, paint)
                source.recycle()
            }
        } else {
            paint.textSize = if (layer.kind == "emoji") 120f else 56f
            paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            paint.textAlign = Paint.Align.CENTER
            val baseline = -(paint.ascent() + paint.descent()) / 2
            if (layer.outline > 0) {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = layer.outline * 2
                paint.color = Color.WHITE; canvas.drawText(layer.value, 0f, baseline, paint)
            }
            paint.style = Paint.Style.FILL; paint.color = layer.color.toInt()
            canvas.drawText(layer.value, 0f, baseline, paint)
        }
        canvas.restore()
    }
    document.strokes.forEach { stroke ->
        paint.reset(); paint.isAntiAlias = true; paint.color = stroke.color.toInt()
        paint.strokeWidth = stroke.width; paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        val path = Path()
        stroke.points.forEachIndexed { index, p ->
            if (index == 0) path.moveTo(p.x * 512, p.y * 512) else path.lineTo(p.x * 512, p.y * 512)
        }
        if (stroke.points.size == 1) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(stroke.points[0].x * 512, stroke.points[0].y * 512, stroke.width / 2, paint)
        } else canvas.drawPath(path, paint)
    }
    return bitmap
}

internal fun Bitmap.pngBytes(): ByteArray = ByteArrayOutputStream().use {
    check(compress(Bitmap.CompressFormat.PNG, 100, it)); it.toByteArray()
}

internal object EditorDraft {
    fun read(context: Context): StickerDocument = runCatching {
        Json.decodeFromString<StickerDocument>(File(context.filesDir, "sticker-draft.json").readText())
    }.getOrDefault(StickerDocument())
    fun write(context: Context, document: StickerDocument) {
        val target = android.util.AtomicFile(File(context.filesDir, "sticker-draft.json"))
        val stream = target.startWrite()
        try { stream.write(Json.encodeToString(document).toByteArray()); target.finishWrite(stream) }
        catch (failure: Exception) { target.failWrite(stream); throw failure }
    }
}
