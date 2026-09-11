package com.stickermaker.funnyemoji.data

import android.content.Context
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject

@Serializable
private data class FavoriteRow(@SerialName("sticker_id") val stickerId: String)

/** Device cache plus retryable outbox. A failed request never claims cloud success. */
class FavoritesStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("sticker-favorites", 0)
    private val knownIds = StickerCatalog.entries.map { it.id }.toSet()
    private val state = MutableStateFlow(FavoriteSnapshot(
        preferences.getStringSet("ids", emptySet()).orEmpty().intersect(knownIds),
        runCatching {
            val json = JSONObject(preferences.getString("pending", "{}") ?: "{}")
            json.keys().asSequence().filter { it in knownIds }.associateWith { json.getBoolean(it) }
        }.getOrDefault(emptyMap())))
    val snapshot = state.asStateFlow()
    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val mutex = Mutex()

    fun choose(id: String, selected: Boolean) {
        require(id in knownIds)
        state.value = state.value.choose(id, selected)
        persist()
    }

    private fun persist() {
        preferences.edit().putStringSet("ids", state.value.ids)
            .putString("pending", JSONObject(state.value.pending).toString()).apply()
    }

    suspend fun sync() = mutex.withLock {
        _syncing.value = true; _error.value = null
        try {
            val owner = DemoSession.userId()
            val previousOwner = preferences.getString("owner", null)
            if (previousOwner != null && previousOwner != owner) {
                state.value = FavoriteSnapshot(); persist()
            }
            preferences.edit().putString("owner", owner).apply()
            val client = SupabaseProvider.client
            val remote = client.from("sticker_favorites").select { filter { eq("user_id", owner) } }
                .decodeList<FavoriteRow>().map { it.stickerId }.toSet().intersect(knownIds)
            state.value = state.value.merge(remote); persist()
            // One unsupported catalogue ID must not block other pending favorites.
            // Read a fresh snapshot each pass: a toggle during an in-flight request wins.
            val failed = mutableSetOf<String>()
            while (true) {
                val (id, selected) = state.value.pending.entries.firstOrNull { it.key !in failed } ?: break
                try {
                    if (selected) client.from("sticker_favorites").upsert(FavoriteRow(id))
                    else client.from("sticker_favorites").delete { filter { eq("user_id", owner); eq("sticker_id", id) } }
                    state.value = state.value.acknowledge(id, selected); persist()
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { failed += id }
            }
            if (failed.isNotEmpty()) _error.value = "Saved on this device. Some favorites couldn't sync; retry when connected."
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { _error.value = "Saved on this device. Cloud sync unavailable; retry when connected." }
        finally { _syncing.value = false }
    }
}
