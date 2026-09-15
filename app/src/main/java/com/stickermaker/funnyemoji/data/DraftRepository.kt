package com.stickermaker.funnyemoji.data

import com.stickermaker.funnyemoji.data.local.DraftDao
import com.stickermaker.funnyemoji.data.local.DraftEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.AtomicFile
import java.io.File
import java.io.IOException

data class DraftSnapshot(
    val document: StickerDocument,
    val collectionId: String?,
    val updatedAt: Long,
)

/** Stores the active draft locally; no Supabase calls are needed. */
class DraftRepository(private val dao: DraftDao) {
    /** Import the previous file once. Keep it intact if decoding or the database write fails. */
    suspend fun readOrMigrate(legacyFile: File, collectionId: String? = null): DraftSnapshot? =
        migrationMutex.withLock {
            withContext(Dispatchers.IO) {
                val existing = read()
                val legacy = AtomicFile(legacyFile)
                val hasLegacy = legacyFile.exists() || File(legacyFile.path + ".bak").exists()
                if (!hasLegacy) return@withContext existing
                if (existing == null) {
                    val document = Json.decodeFromString<StickerDocument>(legacy.readFully().decodeToString())
                    save(document, collectionId)
                }
                // Also clean up if a previous process stopped after committing to Room.
                legacy.delete()
                if (legacyFile.exists() || File(legacyFile.path + ".bak").exists()) {
                    throw IOException("Couldn't finish migrating the draft file")
                }
                read()
            }
        }

    suspend fun save(document: StickerDocument, collectionId: String? = null) =
        withContext(Dispatchers.IO) {
            dao.save(
                DraftEntity(
                    documentJson = Json.encodeToString(document),
                    updatedAt = System.currentTimeMillis(),
                    collectionId = collectionId,
                ),
            )
        }

    suspend fun read(): DraftSnapshot? = withContext(Dispatchers.IO) {
        val entity = dao.read() ?: return@withContext null
        DraftSnapshot(
            document = Json.decodeFromString<StickerDocument>(entity.documentJson),
            collectionId = entity.collectionId,
            updatedAt = entity.updatedAt,
        )
    }

    suspend fun delete() = dao.delete()

    private companion object {
        val migrationMutex = Mutex()
    }
}
