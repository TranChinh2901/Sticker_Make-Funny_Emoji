package com.stickermaker.funnyemoji.data

/** Bundled Figma fixtures, with stable identities shared by every browsing screen. */
data class CatalogSticker(val id: String, val name: String, val category: String, val tags: List<String>)

object StickerCatalog {
    val entries = listOf("Trending", "Frame", "Animal").flatMap { category ->
        (0..2).map { index ->
            CatalogSticker("${category.lowercase()}-$index", "NickNam", category,
                listOf(category, "NickNam"))
        }
    }

    fun search(query: String): List<CatalogSticker> {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
        return entries.filter { sticker ->
            val searchable = (listOf(sticker.name, sticker.category) + sticker.tags).joinToString(" ").lowercase()
            words.all { it in searchable }
        }
    }
}

/** Pending choices override a downloaded snapshot until the server acknowledges them. */
data class FavoriteSnapshot(val ids: Set<String> = emptySet(), val pending: Map<String, Boolean> = emptyMap()) {
    fun choose(id: String, selected: Boolean) = copy(
        ids = if (selected) ids + id else ids - id, pending = pending + (id to selected))
    fun merge(remote: Set<String>) = copy(ids = pending.entries.fold(remote) { ids, (id, selected) ->
        if (selected) ids + id else ids - id
    })
    fun acknowledge(id: String, selected: Boolean) =
        if (pending[id] == selected) copy(pending = pending - id) else this
}
