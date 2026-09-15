package com.stickermaker.funnyemoji.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stickermaker.funnyemoji.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DraftRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrationSurvivesReopenAndDeletedDraftDoesNotReturn() = runBlocking {
        val name = "draft-test-${UUID.randomUUID()}.db"
        val legacy = File(context.cacheDir, "$name.json")
        var database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val original = StickerDocument(
            background = 0xFFFBCFE8,
            layers = listOf(StickerLayer(kind = "text", value = "Xin chào", x = .2f, scale = 1.4f)),
            strokes = listOf(InkStroke(listOf(InkPoint(.1f, .2f)), 0xFF0099FF, 8f)),
        )
        try {
            legacy.writeText(Json.encodeToString(original))
            val migrated = DraftRepository(database.draftDao()).readOrMigrate(legacy, "collection-1")
            assertEquals(original, migrated?.document)
            assertEquals("collection-1", migrated?.collectionId)
            assertFalse(legacy.exists())
            database.close()
            database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val repository = DraftRepository(database.draftDao())
            assertEquals(migrated, repository.read())
            val changed = original.copy(pattern = true)
            repository.save(changed, "collection-1")
            assertEquals(changed, repository.read()?.document)
            repository.delete()
            assertNull(repository.readOrMigrate(legacy))
        } finally {
            database.close()
            context.deleteDatabase(name)
            legacy.delete()
        }
    }

    @Test fun corruptLegacyIsKeptAndDoesNotBecomeAnEmptyDraft() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val legacy = File(context.cacheDir, "draft-corrupt-${UUID.randomUUID()}.json")
        try {
            legacy.writeText("broken JSON")
            val repository = DraftRepository(database.draftDao())
            try {
                repository.readOrMigrate(legacy)
                fail("A corrupt draft must report failure")
            } catch (_: kotlinx.serialization.SerializationException) {
                assertEquals("broken JSON", legacy.readText())
                assertNull(repository.read())
            }
        } finally { database.close(); legacy.delete() }
    }

    @Test fun roomWinsAfterMigrationWasInterruptedBeforeFileCleanup() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val legacy = File(context.cacheDir, "draft-old-${UUID.randomUUID()}.json")
        try {
            legacy.writeText(Json.encodeToString(StickerDocument()))
            val repository = DraftRepository(database.draftDao())
            val current = StickerDocument(pattern = true)
            repository.save(current)
            assertEquals(current, repository.readOrMigrate(legacy)?.document)
            assertFalse(legacy.exists())
            repository.delete()
            assertNull(repository.readOrMigrate(legacy))
        } finally { database.close(); legacy.delete() }
    }
}
