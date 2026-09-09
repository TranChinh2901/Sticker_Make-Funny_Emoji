package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
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
import com.stickermaker.funnyemoji.data.StickerCollection
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Renders production composables with explicit visual fixtures; never seeds the user's database. */
@RunWith(AndroidJUnit4::class)
class MyStudioVisualTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun emptyAndPopulatedDesignsKeepTheirActions() {
        val creates = AtomicInteger()
        val opens = AtomicInteger()
        val options = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            fun render(populated: Boolean) {
                scenario.onActivity { activity ->
                    WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                        .hide(WindowInsetsCompat.Type.systemBars())
                    activity.setContent {
                        StickerMakerTheme {
                            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                                Box(Modifier.fillMaxSize()) {
                                    Box(Modifier.size(390.dp, 844.dp).consumeWindowInsets(WindowInsets.navigationBars)) {
                                        MyStudioLayout(
                                            state = StudioOverview(drafts = 1, collections = if (populated)
                                                listOf(StickerCollection("visual-only", "hehe ok", "#E7F7F3")) else emptyList()),
                                            onCreate = { creates.incrementAndGet() },
                                            onCollection = { opens.incrementAndGet() },
                                            onOptions = { options.incrementAndGet() },
                                            thumbnail = { _, modifier -> Image(painterResource(R.drawable.studio_sample_thumbnail), null, modifier) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(1200)
                // A fresh emulator may show Android's immersive-mode education dialog.
                instrumentation.uiAutomation.rootInActiveWindow
                    ?.findAccessibilityNodeInfosByText("Got it")?.firstOrNull()
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                instrumentation.waitForIdleSync()
                SystemClock.sleep(200)
                val capture = instrumentation.uiAutomation.takeScreenshot()
                val cropped = Bitmap.createBitmap(capture, 0, 0, 390, 844)
                val output = File(instrumentation.targetContext.cacheDir, if (populated) "studio-populated.png" else "studio-empty.png")
                output.outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) }
                cropped.recycle(); capture.recycle()
            }
            render(false)
            tap(195f, 489f)
            assertEquals("Empty-state create button", 1, creates.get())
            tap(160f, 243f)
            assertEquals("New Collection banner", 2, creates.get())
            render(true)
            tap(80f, 430f)
            assertEquals("Collection thumbnail opens detail", 1, opens.get())
            tap(166f, 369f)
            assertEquals("Options uses a separate touch target", 1, options.get())
            assertEquals("Options must not open the collection", 1, opens.get())
        }
    }
    private fun tap(x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(now, now + 50, action, x, y, 0)
            instrumentation.uiAutomation.injectInputEvent(event, true)
            event.recycle()
        }
        instrumentation.waitForIdleSync()
    }
}
