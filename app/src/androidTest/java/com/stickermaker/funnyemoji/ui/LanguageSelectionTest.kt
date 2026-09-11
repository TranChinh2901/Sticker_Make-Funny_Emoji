package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
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
import com.stickermaker.funnyemoji.data.*
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LanguageSelectionTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun tap(x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            MotionEvent.obtain(now, now + 50, action, x, y, 0).also {
                instrumentation.uiAutomation.injectInputEvent(it, true); it.recycle()
            }
        }
        instrumentation.waitForIdleSync()
    }
    private fun settle() {
        instrumentation.waitForIdleSync(); SystemClock.sleep(1500)
        instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Got it")?.firstOrNull()
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        instrumentation.waitForIdleSync()
    }
    private fun render(scenario: ActivityScenario<MainActivity>, content: @Composable () -> Unit) {
        scenario.onActivity { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).hide(WindowInsetsCompat.Type.systemBars())
            activity.setContent {
                StickerMakerTheme {
                    CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.size(390.dp, 844.dp).consumeWindowInsets(WindowInsets.navigationBars)) { content() }
                        }
                    }
                }
            }
        }
        settle()
    }
    @Test fun selectionIsExplicitAndBusyStateBlocksChanges() {
        val language = mutableStateOf("fr")
        val busy = mutableStateOf(false)
        var confirmed: String? = null
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            render(scenario) { LanguageScreen(language.value, { language.value = it }, { confirmed = language.value }, busy = busy.value) }
            val full = instrumentation.uiAutomation.takeScreenshot()
            val crop = Bitmap.createBitmap(full, 0, 0, 390, 844)
            File(instrumentation.targetContext.cacheDir, "language-layout.png").outputStream().use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
            crop.recycle(); full.recycle()
            tap(140f, 194f)
            assertEquals("en", language.value); assertNull("Selecting does not implicitly confirm", confirmed)
            scenario.onActivity { busy.value = true }; instrumentation.waitForIdleSync()
            tap(140f, 126f); tap(358f, 65f)
            assertEquals("en", language.value); assertNull(confirmed)
            scenario.onActivity { busy.value = false }; instrumentation.waitForIdleSync()
            tap(358f, 65f)
            assertEquals("en", confirmed)
        }
    }

    @Test fun settingsConfirmationPersistsAndCancelDoesNot() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("liveSupabase") == "true")
        withTimeout(60_000) {
            val owner = DemoSession.userId()
            val before = StudioRepository.settings()
            val preferences = instrumentation.targetContext.getSharedPreferences("startup", 0)
            val previousLocal = preferences.getString("language", null)
            val selected = if (before?.language == "fr") "vi" else "fr"
            fun clickText(value: String) {
                val deadline = SystemClock.uptimeMillis() + 5000
                var found: AccessibilityNodeInfo? = null
                while (found == null && SystemClock.uptimeMillis() < deadline) {
                    fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                        if (node == null) return null
                        node.refresh()
                        if (node.text?.toString() == value) return node
                        for (i in 0 until node.childCount) find(node.getChild(i))?.let { return it }
                        return null
                    }
                    found = find(instrumentation.uiAutomation.rootInActiveWindow)
                    if (found == null) SystemClock.sleep(100)
                }
                val node = requireNotNull(found) { "Missing UI text: $value" }
                val rect = Rect(); node.getBoundsInScreen(rect)
                tap(rect.exactCenterX(), rect.exactCenterY())
            }
            try {
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    render(scenario) { SettingsScreen({}, {}) }
                    val capture = instrumentation.uiAutomation.takeScreenshot()
                    File(instrumentation.targetContext.cacheDir, "settings-language-start.png").outputStream().use { capture.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    capture.recycle()
                    clickText("Language Setting")
                    tap(140f, if (selected == "fr") 126f else 602f)
                    tap(32f, 65f) // Cancel: no server mutation.
                    assertEquals(before, StudioRepository.settings())
                    clickText("Language Setting")
                    tap(140f, if (selected == "fr") 126f else 602f)
                    tap(358f, 65f)
                    val deadline = SystemClock.uptimeMillis() + 15000
                    while (preferences.getString("language", null) != selected && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
                    assertEquals(selected, preferences.getString("language", null))
                    assertEquals(selected, StudioRepository.settings()?.language)
                    settle()
                    clickText("Language Setting") // Fresh accessibility tree confirms return to Settings.
                    tap(32f, 65f)
                }
            } finally {
                if (before != null) StudioRepository.language(before.language)
                else SupabaseProvider.client.from("user_settings").delete { filter { eq("user_id", owner) } }
                preferences.edit().apply { if (previousLocal == null) remove("language") else putString("language", previousLocal) }.commit()
            }
        }
    }
}
