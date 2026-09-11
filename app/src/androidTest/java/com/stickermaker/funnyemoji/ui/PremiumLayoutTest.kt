package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stickermaker.funnyemoji.MainActivity
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PremiumLayoutTest {
    @Test fun planSelectionAndIndependentActions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monthly = mutableStateOf(false)
        val close = AtomicInteger(); val upgrade = AtomicInteger()
        val terms = AtomicInteger(); val privacy = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView).hide(WindowInsetsCompat.Type.systemBars())
                activity.setContent {
                    StickerMakerTheme {
                        CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.size(390.dp, 844.dp).consumeWindowInsets(WindowInsets.navigationBars)) {
                                    PremiumLayout(monthly.value, { monthly.value = it }, { close.incrementAndGet() },
                                        { upgrade.incrementAndGet() }, { terms.incrementAndGet() }, { privacy.incrementAndGet() })
                                }
                            }
                        }
                    }
                }
            }
            instrumentation.waitForIdleSync(); SystemClock.sleep(1500)
            fun screenshot(name: String) {
                val full = instrumentation.uiAutomation.takeScreenshot()
                val crop = Bitmap.createBitmap(full, 0, 0, 390, 844)
                File(instrumentation.targetContext.cacheDir, name).outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
                crop.recycle(); full.recycle()
            }
            fun tap(x: Float, y: Float) {
                val now = SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                    MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, x, y, 0).also {
                        it.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                        assertTrue("Touch injection", instrumentation.uiAutomation.injectInputEvent(it, true)); it.recycle()
                        SystemClock.sleep(80)
                    }
                }
                instrumentation.waitForIdleSync(); SystemClock.sleep(200)
            }
            screenshot("premium-weekly.png")
            tap(180f, 620f)
            scenario.onActivity { assertTrue(monthly.value) }
            screenshot("premium-monthly.png")
            tap(180f, 530f)
            scenario.onActivity { assertFalse(monthly.value) }
            tap(195f, 740f); tap(155f, 786f); tap(214f, 786f); tap(30f, 68f)
            assertEquals(1, upgrade.get()); assertEquals(1, terms.get())
            assertEquals(1, privacy.get()); assertEquals(1, close.get())
        }
    }
}
