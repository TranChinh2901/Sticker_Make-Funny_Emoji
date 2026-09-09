package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stickermaker.funnyemoji.MainActivity
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.SavedSticker
import com.stickermaker.funnyemoji.data.StickerCollection
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class CollectionDetailVisualTest {
    @Test fun gridActionsAndUnavailableStates() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val adds = AtomicInteger()
        val previews = AtomicInteger()
        val removals = AtomicInteger()
        val options = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun render(state: String) {
                scenario.onActivity { activity ->
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView).hide(WindowInsetsCompat.Type.systemBars())
                    activity.setContent {
                        StickerMakerTheme {
                            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                                Box(Modifier.fillMaxSize()) {
                                    Box(Modifier.size(390.dp, 844.dp).consumeWindowInsets(WindowInsets.navigationBars)) {
                                        CollectionDetailLayout(
                                            collection = StickerCollection("visual-only", "hehe ok", "#FFFFFF"),
                                            stickers = if (state == "populated") listOf(SavedSticker("visual-sticker", "Happy cat", "fixture")) else emptyList(),
                                            loading = state == "loading", error = if (state == "error") "Couldn't load stickers" else null,
                                            onAdd = { adds.incrementAndGet() }, onPreview = { previews.incrementAndGet() }, onRemove = { removals.incrementAndGet() },
                                            options = { androidx.compose.material3.IconButton(onClick = { options.incrementAndGet() }) { Asset(R.drawable.detail_mingcute_more2_line, 24.dp) } },
                                            thumbnail = { Image(painterResource(R.drawable.studio_sample_thumbnail), null, it) },
                                            stickerImage = { _, modifier -> Image(painterResource(R.drawable.studio_sample_thumbnail), null, modifier) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(1200)
                val full = instrumentation.uiAutomation.takeScreenshot()
                val cropped = Bitmap.createBitmap(full, 0, 0, 390, 844)
                File(instrumentation.targetContext.cacheDir, "collection-detail-$state.png").outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
                cropped.recycle(); full.recycle()
            }
            fun tap(x: Float, y: Float) {
                val now = SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                    val event = MotionEvent.obtain(now, now + 50, action, x, y, 0)
                    instrumentation.uiAutomation.injectInputEvent(event, true); event.recycle()
                }
                instrumentation.waitForIdleSync()
            }
            render("empty")
            tap(358f, 68f)
            assertEquals("Collection menu", 1, options.get())
            tap(68f, 315f); tap(190f, 790f)
            assertEquals("Both Add entry points", 2, adds.get())
            render("populated")
            tap(180f, 315f)
            assertEquals("Sticker shares first row with Add", 1, previews.get())
            tap(180f, 412f)
            assertEquals("Remove remains a separate action", 1, removals.get())
            assertEquals(1, previews.get())
            render("loading")
            tap(190f, 790f)
            render("error")
            tap(190f, 790f)
            assertEquals("Unavailable memberships cannot be edited", 2, adds.get())
        }
    }
}
