package com.stickermaker.funnyemoji.data

import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@Serializable
private data class VerifiedFavorite(@SerialName("sticker_id") val id: String)

@RunWith(AndroidJUnit4::class)
class FavoritesCloudTest {
    @Test fun pendingHugDoesNotBlockExistingCatalogFavorites() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveSupabase") == "true")
        withTimeout(60_000) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val owner = DemoSession.userId()
            val table = SupabaseProvider.client.from("sticker_favorites")
            suspend fun remote() = table.select { filter { eq("user_id", owner) } }.decodeList<VerifiedFavorite>().map { it.id }.toSet()
            val before = remote()
            // Isolate the test outbox from the app's real device preferences.
            val testContext = object : android.content.ContextWrapper(instrumentation.targetContext) {
                override fun getApplicationContext(): android.content.Context = this
                override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences =
                    baseContext.getSharedPreferences("preview-verification-$name", mode)
            }
            val preferences = testContext.getSharedPreferences("sticker-favorites", 0)
            preferences.edit().clear().commit()
            val store = FavoritesStore(testContext)
            try {
                store.choose("hug-0", true)
                store.choose("trending-0", true)
                store.sync()
                val after = remote()
                assertTrue("Existing IDs sync even if Hug needs a schema migration", "trending-0" in after)
                assertFalse(store.snapshot.value.pending.containsKey("trending-0"))
                assertTrue("Hug is acknowledged or retained for retry", "hug-0" in after || store.snapshot.value.pending["hug-0"] == true)
                instrumentation.sendStatus(0, Bundle().apply {
                    putString("hugFavorites", if ("hug-0" in after) "cloud-ready" else "migration-required")
                })
            } finally {
                // Restore only the two rows this test may have newly created.
                for (id in listOf("hug-0", "trending-0").filter { it !in before }) {
                    table.delete { filter { eq("user_id", owner); eq("sticker_id", id) } }
                }
                preferences.edit().clear().commit()
            }
        }
    }
}
