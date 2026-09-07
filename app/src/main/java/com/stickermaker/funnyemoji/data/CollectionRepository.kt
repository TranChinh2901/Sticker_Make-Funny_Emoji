package com.stickermaker.funnyemoji.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StickerCollection(
    val id: String,
    val name: String,
    @SerialName("background_color") val backgroundColor: String,
    @SerialName("thumbnail_path") val thumbnailPath: String? = null,
)

@Serializable
private data class NewCollection(
    val name: String,
    @SerialName("background_color") val backgroundColor: String,
    @SerialName("thumbnail_path") val thumbnailPath: String? = null,
)

object CollectionRepository {
    suspend fun rename(id: String, name: String) {
        require(name.trim().length in 1..80) { "Tên cần từ 1 đến 80 ký tự." }
        val owner = DemoSession.userId()
        SupabaseProvider.client.from("sticker_collections").update({ set("name", name.trim()) }) {
            filter { eq("id", id); eq("user_id", owner) }
        }
    }

    suspend fun delete(id: String) {
        val owner = DemoSession.userId()
        SupabaseProvider.client.from("sticker_collections").delete {
            filter { eq("id", id); eq("user_id", owner) }
        }
    }

    suspend fun list(): List<StickerCollection> {
        check(SupabaseProvider.isConfigured) { "Chưa cấu hình Supabase." }
        val client = SupabaseProvider.client
        val userId = DemoSession.userId()
        return client.from("sticker_collections").select {
            filter { eq("user_id", userId) }
            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeList<StickerCollection>()
    }

    suspend fun create(name: String, backgroundColor: String, thumbnail: ByteArray? = null): StickerCollection {
        val trimmed = name.trim()
        require(trimmed.length in 1..80) { "Tên bộ sưu tập cần từ 1 đến 80 ký tự." }
        require(Regex("^#[0-9A-Fa-f]{6}$").matches(backgroundColor)) { "Màu nền không hợp lệ." }
        check(SupabaseProvider.isConfigured) { "Chưa cấu hình Supabase." }
        val client = SupabaseProvider.client
        val userId = DemoSession.userId()
        val thumbnailPath = thumbnail?.let { bytes ->
            require(bytes.size <= 5 * 1024 * 1024) { "Ảnh quá lớn." }
            val path = "$userId/${UUID.randomUUID()}.png"
            client.storage.from("collection-thumbnails").upload(path, bytes) {
                contentType = ContentType.Image.PNG
                upsert = false
            }
            path
        }
        // Do not delete on insert failure: a timed-out request may have committed the row.
        // Unreferenced uploads can be collected server-side after a grace period.
        return client.from("sticker_collections").insert(NewCollection(trimmed, backgroundColor, thumbnailPath)) {
            select()
        }.decodeSingle<StickerCollection>()
    }
}
