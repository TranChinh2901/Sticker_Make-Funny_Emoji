package com.stickermaker.funnyemoji.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.UUID

/** Opt-in integration test. Creates and cleans only its own uniquely identified records. */
@RunWith(AndroidJUnit4::class)
class StudioCloudTest {
    @Test fun collectionAndStickerRoundTrip() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveSupabase") == "true")
        withTimeout(90_000) {
            val stickerId = UUID.randomUUID().toString()
            val title = "Studio verification ${stickerId.take(8)}"
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
            val png = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            bitmap.recycle()
            var collection: StickerCollection? = null
            var sticker: SavedSticker? = null
            try {
                collection = CollectionRepository.create(title, "#E7F7F3", png)
                assertTrue(CollectionRepository.list().any { it.id == collection.id })
                assertArrayEquals(png, StudioRepository.image(requireNotNull(collection.thumbnailPath), true))
                CollectionRepository.rename(collection.id, "$title renamed")
                assertEquals("$title renamed", CollectionRepository.list().first { it.id == collection.id }.name)
                sticker = StudioRepository.save(stickerId, title, png)
                assertArrayEquals(png, StudioRepository.image(sticker.imagePath))
                StudioRepository.add(collection.id, sticker.id)
                StudioRepository.add(collection.id, sticker.id)
                assertEquals(1, StudioRepository.collectionCounts()[collection.id])
                assertEquals(listOf(sticker.id), StudioRepository.list(collection.id).map { it.id })
                StudioRepository.remove(collection.id, sticker.id)
                assertTrue(StudioRepository.list(collection.id).isEmpty())
                StudioRepository.add(collection.id, sticker.id)
                CollectionRepository.delete(collection.id)
                assertFalse(CollectionRepository.list().any { it.id == collection.id })
                assertTrue("Deleting a collection must retain its stickers", StudioRepository.list().any { it.id == sticker.id })
                assertNull(StudioRepository.collectionCounts()[collection.id])
            } finally {
                // Only test-created IDs and objects are eligible for cleanup.
                collection?.let {
                    CollectionRepository.delete(it.id)
                    it.thumbnailPath?.let { path -> SupabaseProvider.client.storage.from("collection-thumbnails").delete(listOf(path)) }
                }
                StudioRepository.delete(stickerId)
                sticker?.let { SupabaseProvider.client.storage.from("sticker-images").delete(listOf(it.imagePath)) }
            }
        }
    }
}
