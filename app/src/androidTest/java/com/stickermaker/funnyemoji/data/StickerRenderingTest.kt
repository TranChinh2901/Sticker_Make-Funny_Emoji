package com.stickermaker.funnyemoji.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class StickerRenderingTest {
    // Test package storage: never overwrite a user's draft/imports.
    private val context get() = InstrumentationRegistry.getInstrumentation().context

    @Test fun transparentExportHasNoCheckerboard() {
        val image = renderSticker(context, StickerDocument())
        try {
            assertEquals(512, image.width); assertEquals(512, image.height)
            assertEquals(0, Color.alpha(image.getPixel(0, 0)))
            assertEquals(0, Color.alpha(image.getPixel(256, 256)))
        } finally { image.recycle() }
    }

    @Test fun backgroundAndPngRoundTripKeepExactColors() {
        val image = renderSticker(context, StickerDocument(background = 0xFF00B686))
        try {
            val bytes = image.pngBytes()
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            try {
                assertEquals(0xFF00B686.toInt(), decoded.getPixel(5, 5))
                assertEquals(image.getPixel(400, 300), decoded.getPixel(400, 300))
            } finally { decoded.recycle() }
        } finally { image.recycle() }
    }

    @Test fun importedLandscapeKeepsAspectRatioAndTransparentPadding() {
        val directory = File(context.filesDir, "imports").apply { mkdirs() }
        val file = File(directory, "test-landscape.png")
        val source = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.RED); file.writeBytes(source.pngBytes()); source.recycle()
        val image = renderSticker(context, StickerDocument(layers = listOf(StickerLayer(kind = "image", value = file.name))))
        try {
            assertEquals(Color.RED, image.getPixel(256, 256))
            assertEquals(Color.RED, image.getPixel(100, 256))
            assertEquals(0, Color.alpha(image.getPixel(256, 100)))
            assertEquals(0, Color.alpha(image.getPixel(50, 256)))
        } finally { image.recycle(); file.delete() }
    }

    @Test fun strokeUsesNormalizedCoordinatesAndPreservesTransparencyOutsideIt() {
        val doc = StickerDocument(strokes = listOf(InkStroke(listOf(InkPoint(.25f, .5f), InkPoint(.75f, .5f)), 0xFFFF0000, 10f)))
        val image = renderSticker(context, doc)
        try {
            assertEquals(Color.RED, image.getPixel(256, 256))
            assertEquals(0, Color.alpha(image.getPixel(256, 220)))
        } finally { image.recycle() }
    }

    @Test fun lastLayerRendersInFrontAndMovesToExpectedPosition() {
        val red = InkStroke(listOf(InkPoint(.5f, .5f)), 0xFFFF0000, 20f)
        val image = renderSticker(context, StickerDocument(background = 0xFF00B686, strokes = listOf(red)))
        try {
            assertEquals(Color.RED, image.getPixel(256, 256))
            assertEquals(0xFF00B686.toInt(), image.getPixel(50, 50))
        } finally { image.recycle() }
    }

    @Test fun draftRestoresLayersColorsPositionsAndBrushData() {
        val original = StickerDocument(background = 0xFFFBCFE8, layers = listOf(StickerLayer(kind = "text", value = "Xin chào", x = .2f, y = .7f, scale = 1.4f, outline = 4f)),
            strokes = listOf(InkStroke(listOf(InkPoint(.1f, .2f), InkPoint(.8f, .9f)), 0xFF0099FF, 8f)))
        try {
            EditorDraft.write(context, original)
            assertEquals(original, EditorDraft.read(context))
        } finally { File(context.filesDir, "sticker-draft.json").delete() }
    }
}
