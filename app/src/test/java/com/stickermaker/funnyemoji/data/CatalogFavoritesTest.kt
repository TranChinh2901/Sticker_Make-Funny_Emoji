package com.stickermaker.funnyemoji.data

import org.junit.Assert.*
import org.junit.Test

class CatalogFavoritesTest {
    @Test fun `cat fixtures match generic queries and specific names`() {
        val cats = StickerCatalog.cats
        assertEquals(9, cats.size)
        assertTrue(StickerCatalog.search("  CAT  ").containsAll(cats))
        assertTrue(StickerCatalog.search("mèo").containsAll(cats))
        assertEquals(listOf(cats[1]), StickerCatalog.search("black cat"))
        assertEquals(listOf(cats[6]), StickerCatalog.search("Scottish Fold"))
        assertTrue(StickerCatalog.search("purple spaceship cat").isEmpty())
        assertEquals(cats.size, cats.map { it.imageRes }.distinct().size)
    }

    @Test fun `search matches category case and whitespace using stable IDs`() {
        val home = StickerCatalog.entries.filter { it.category == "Trending" }
        assertEquals(home, StickerCatalog.search("  TRENDING  "))
        assertEquals(home, StickerCatalog.search("NickNam trending"))
        assertEquals(StickerCatalog.entries, StickerCatalog.search(" "))
        assertTrue(StickerCatalog.search("not-a-sticker").isEmpty())
        assertEquals(StickerCatalog.entries.size, StickerCatalog.entries.map { it.id }.distinct().size)
    }

    @Test fun `offline unfavorite is not resurrected by stale server snapshot`() {
        val local = FavoriteSnapshot(setOf("trending-0")).choose("trending-0", false)
        val merged = local.merge(setOf("trending-0", "animal-1"))
        assertEquals(setOf("animal-1"), merged.ids)
        assertEquals(mapOf("trending-0" to false), merged.pending)
    }

    @Test fun `toggle during request survives the old acknowledgement`() {
        val queued = FavoriteSnapshot().choose("frame-0", true)
        val toggled = queued.choose("frame-0", false)
        assertEquals(toggled, toggled.acknowledge("frame-0", true))
        assertEquals(emptyMap<String, Boolean>(), toggled.acknowledge("frame-0", false).pending)
    }

    @Test fun `offline additions and removals merge with remote changes independently`() {
        val local = FavoriteSnapshot().choose("frame-0", true).choose("animal-0", false)
        val merged = local.merge(setOf("animal-0", "trending-1"))
        assertEquals(setOf("frame-0", "trending-1"), merged.ids)
        assertEquals(setOf("animal-0"), merged.acknowledge("frame-0", true).pending.keys)
    }
}
