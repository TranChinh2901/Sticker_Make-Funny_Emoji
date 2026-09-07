package com.stickermaker.funnyemoji.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StickerCollection(
    val id: String,
    val name: String,
    @SerialName("background_color") val backgroundColor: String,
)

@Serializable
private data class NewCollection(val name: String, @SerialName("background_color") val backgroundColor: String)

object CollectionRepository {
    suspend fun create(name: String, backgroundColor: String): StickerCollection {
        val trimmed = name.trim()
        require(trimmed.length in 1..80) { "Tên bộ sưu tập cần từ 1 đến 80 ký tự." }
        require(Regex("^#[0-9A-Fa-f]{6}$").matches(backgroundColor)) { "Màu nền không hợp lệ." }
        check(SupabaseProvider.isConfigured) { "Chưa cấu hình Supabase." }
        val client = SupabaseProvider.client
        check(client.auth.currentUserOrNull() != null) { "Bạn cần đăng nhập trước khi lưu bộ sưu tập lên server." }
        return client.from("sticker_collections").insert(NewCollection(trimmed, backgroundColor)) {
            select()
        }.decodeSingle<StickerCollection>()
    }
}
