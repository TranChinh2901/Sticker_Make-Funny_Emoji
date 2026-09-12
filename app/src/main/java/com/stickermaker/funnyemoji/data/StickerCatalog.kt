package com.stickermaker.funnyemoji.data

import androidx.annotation.DrawableRes
import com.stickermaker.funnyemoji.R

/** Bundled Figma fixtures, with stable identities shared by every browsing screen. */
data class CatalogSticker(val id: String, val name: String, val category: String, val tags: List<String>,
    @DrawableRes val imageRes: Int = R.drawable.home_reference_sticker)

object StickerCatalog {
    /** Offline fixtures with stable IDs shared by search, favorites and unlock previews. */
    val cats = listOf(
        "Kitty" to R.drawable.seed_cat_0,
        "Black Cat" to R.drawable.seed_cat_1,
        "White Cat" to R.drawable.seed_cat_2,
        "Siamese" to R.drawable.seed_cat_3,
        "Calico Cat" to R.drawable.seed_cat_4,
        "Persian Cat" to R.drawable.seed_cat_5,
        "Scottish Fold" to R.drawable.seed_cat_6,
        "Maine Coon" to R.drawable.seed_cat_7,
        "Ragdoll" to R.drawable.seed_cat_8,
    ).mapIndexed { index, (name, image) ->
        CatalogSticker("cat-$index", name, "Cat", listOf("cat", "cats", "kitten", "pet", "animal", "mèo", "meo"), image)
    }

    val entries = listOf("Trending", "Frame", "Animal").flatMap { category ->
        (0..2).map { index ->
            CatalogSticker("${category.lowercase()}-$index", "NickNam", category,
                listOf(category, "NickNam"))
        }
    } + cats + (0..2).map { CatalogSticker("hug-$it", "Hug ${it + 1}", "Hug", listOf("hug", "cat")) }

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
