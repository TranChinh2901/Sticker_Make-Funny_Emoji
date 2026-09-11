package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Production layout with explicit image fixture; does not replace or seed saved user content. */
@RunWith(AndroidJUnit4::class)
class StickerPreviewVisualTest {
    @Test fun designGeometryIndependentActionsAndBusyState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val shares = AtomicInteger(); val collections = AtomicInteger(); val favorites = AtomicInteger()
        val uses = AtomicInteger(); val more = AtomicInteger(); val options = AtomicInteger()
        val enabled = mutableStateOf(true)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView).hide(WindowInsetsCompat.Type.systemBars())
                activity.setContent {
                    StickerMakerTheme {
                        CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.size(390.dp, 843.dp).consumeWindowInsets(WindowInsets.navigationBars)) {
                                    StickerPreviewLayout(
                                        onBack = {}, onShare = { shares.incrementAndGet() }, onCollection = { collections.incrementAndGet() },
                                        onViewMore = { more.incrementAndGet() }, enabled = enabled.value,
                                        options = { IconButton(onClick = { options.incrementAndGet() }, Modifier.requiredSize(48.dp)) { Asset(R.drawable.preview_more, 24.dp) } },
                                        preview = { modifier -> Box(modifier, contentAlignment = Alignment.Center) {
                                            Image(painterResource(R.drawable.preview_sample), "Fixture", Modifier.fillMaxWidth().height(195.086.dp), contentScale = ContentScale.FillBounds)
                                        } },
                                        related = { index -> HugCard(index, false, { favorites.incrementAndGet() }, { uses.incrementAndGet() }) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            instrumentation.waitForIdleSync(); SystemClock.sleep(1000)
            instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Got it")?.firstOrNull()
                ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            instrumentation.waitForIdleSync(); SystemClock.sleep(200)
            val full = instrumentation.uiAutomation.takeScreenshot()
            val crop = Bitmap.createBitmap(full, 0, 0, 390, 843)
            File(instrumentation.targetContext.cacheDir, "preview-layout.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
            crop.recycle(); full.recycle()
            fun tap(x: Float, y: Float) {
                val now = SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                    MotionEvent.obtain(now, now + 50, action, x, y, 0).also {
                        instrumentation.uiAutomation.injectInputEvent(it, true); it.recycle()
                    }
                }
                instrumentation.waitForIdleSync()
            }
            tap(195f, 452f); tap(195f, 520f); tap(314f, 582f); tap(358f, 71f)
            tap(123f, 633f)
            assertEquals(1, favorites.get()); assertEquals(0, uses.get())
            tap(215f, 745f)
            assertEquals(1, uses.get()); assertEquals(1, favorites.get())
            assertEquals(1, shares.get()); assertEquals(1, collections.get())
            assertEquals(1, more.get()); assertEquals(1, options.get())
            scenario.onActivity { enabled.value = false }
            instrumentation.waitForIdleSync()
            tap(195f, 452f); tap(195f, 520f)
            assertEquals("Busy share blocked", 1, shares.get()); assertEquals("Busy collection blocked", 1, collections.get())
        }
    }
}
