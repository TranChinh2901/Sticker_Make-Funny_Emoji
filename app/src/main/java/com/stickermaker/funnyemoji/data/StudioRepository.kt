package com.stickermaker.funnyemoji.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavedSticker(val id: String, val name: String, @SerialName("image_path") val imagePath: String)
@Serializable
private data class CollectionLink(@SerialName("collection_id") val collectionId: String,
    @SerialName("sticker_id") val stickerId: String)
@Serializable
data class UserSettings(@SerialName("user_id") val userId: String, val language: String = "en")
@Serializable
private data class Feedback(val id: String, val message: String)

object StudioRepository {
    private val client get() = SupabaseProvider.client
    suspend fun list(collectionId: String? = null): List<SavedSticker> {
        val owner = DemoSession.userId()
        val all = client.from("stickers").select {
            filter { eq("user_id", owner) }; order("created_at", Order.DESCENDING)
        }.decodeList<SavedSticker>()
        if (collectionId == null) return all
        val ids = client.from("collection_stickers").select {
            filter { eq("collection_id", collectionId); eq("user_id", owner) }
        }.decodeList<CollectionLink>().map { it.stickerId }.toSet()
        return all.filter { it.id in ids }
    }

    /** Stable operation ID makes retry after a lost response idempotent. Never overwrite images. */
    suspend fun save(id: String, name: String, png: ByteArray): SavedSticker {
        require(name.trim().length in 1..80)
        require(png.size in 1..5 * 1024 * 1024)
        val owner = DemoSession.userId()
        val existing = client.from("stickers").select { filter { eq("id", id); eq("user_id", owner) } }
            .decodeList<SavedSticker>().firstOrNull()
        if (existing != null) return existing
        val path = "$owner/$id.png"
        // If an earlier upload succeeded but its response was lost, verify it by download.
        try {
            client.storage.from("sticker-images").upload(path, png) { contentType = ContentType.Image.PNG }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (failure: Exception) {
            val uploaded = try { client.storage.from("sticker-images").downloadAuthenticated(path) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { throw failure }
            check(uploaded.contentEquals(png)) { "Ảnh đã thay đổi. Hãy lưu thành sticker mới." }
        }
        return client.from("stickers").upsert(SavedSticker(id, name.trim(), path)) { select() }
            .decodeSingle<SavedSticker>()
    }

    suspend fun add(collectionId: String, stickerId: String) {
        DemoSession.userId()
        client.from("collection_stickers").upsert(CollectionLink(collectionId, stickerId))
    }
    suspend fun remove(collectionId: String, stickerId: String) {
        val owner = DemoSession.userId()
        client.from("collection_stickers").delete { filter {
            eq("collection_id", collectionId); eq("sticker_id", stickerId); eq("user_id", owner)
        } }
    }
    suspend fun image(path: String, thumbnail: Boolean = false): ByteArray {
        val owner = DemoSession.userId()
        require(path.startsWith("$owner/"))
        return client.storage.from(if (thumbnail) "collection-thumbnails" else "sticker-images").downloadAuthenticated(path)
    }
    suspend fun settings(): UserSettings? {
        val owner = DemoSession.userId()
        return client.from("user_settings").select { filter { eq("user_id", owner) } }.decodeList<UserSettings>().firstOrNull()
    }
    suspend fun language(code: String) {
        require(code in listOf("en", "vi"))
        client.from("user_settings").upsert(UserSettings(DemoSession.userId(), code))
    }
    suspend fun feedback(id: String, message: String) {
        require(message.trim().length in 10..2000)
        val owner = DemoSession.userId()
        if (client.from("app_feedback").select { filter { eq("id", id); eq("user_id", owner) } }
                .decodeList<Feedback>().isNotEmpty()) return
        client.from("app_feedback").insert(Feedback(id, message.trim()))
    }
}
