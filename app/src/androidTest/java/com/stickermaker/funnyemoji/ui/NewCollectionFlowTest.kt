package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.view.MotionEvent
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stickermaker.funnyemoji.MainActivity
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class NewCollectionFlowTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test fun failedCreateRetainsInputAndRetryClosesOnlyAfterSuccess() {
        val attempts = AtomicInteger()
        val created = AtomicInteger()
        val dismissed = AtomicInteger()
        val finishSave = CompletableDeferred<Unit>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    .hide(WindowInsetsCompat.Type.systemBars())
                activity.setContent {
                    StickerMakerTheme {
                        var visible by remember { mutableStateOf(true) }
                        MyStudioLayout(StudioOverview(drafts = 1))
                        if (visible) NewCollectionSheet(
                            onDismiss = { dismissed.incrementAndGet(); visible = false },
                            onCreated = { created.incrementAndGet() },
                            createCollection = { name, color, image ->
                                assertEquals("Weekend cats", name)
                                assertEquals("#00B686", color)
                                assertNull(image)
                                if (attempts.incrementAndGet() == 1) error("Simulated unavailable server")
                                finishSave.await()
                            },
                        )
                    }
                }
            }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(1500)
            screenshot("new-collection-open.png")
            waitFor { findText("New Collection") != null }
            SystemClock.sleep(800)
            // Empty form cannot create a collection.
            click("Create")
            assertEquals(0, attempts.get())
            val edit = descendants(instrumentation.uiAutomation.rootInActiveWindow)
                .first { it.isEditable }
            assertTrue(edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "  Weekend cats  ") }))
            instrumentation.waitForIdleSync()
            val green = descendants(instrumentation.uiAutomation.rootInActiveWindow)
                .first { it.contentDescription?.toString() == "Background #00B686" }
            assertTrue(green.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            screenshot("new-collection-form.png")
            click("Create")
            screenshot("new-collection-after-click.png")
            assertEquals("Create must reach persistence callback", 1, attempts.get())
            waitFor { findText("Couldn't create collection.") != null }
            assertNotNull(findText("Weekend cats"))
            assertEquals(0, created.get())
            assertEquals(0, dismissed.get())
            screenshot("new-collection-retry.png")
            click("Create")
            waitFor { attempts.get() == 2 }
            click("Cancel")
            assertEquals("Saving form cannot be dismissed by Cancel", 0, dismissed.get())
            assertEquals("No duplicate save", 2, attempts.get())
            finishSave.complete(Unit)
            waitFor { dismissed.get() == 1 }
            assertEquals(1, created.get())
            assertEquals(2, attempts.get())
        }
    }

    private fun descendants(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
        if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { descendants(node.getChild(it)) }

    private fun findText(text: String) = descendants(instrumentation.uiAutomation.rootInActiveWindow)
        .firstOrNull { it.text?.toString()?.contains(text) == true || it.contentDescription?.toString()?.contains(text) == true }

    private fun click(text: String) {
        var node = findText(text) ?: error("Missing $text")
        while (!node.isClickable && node.parent != null) node = node.parent
        android.util.Log.i("StudioFlowProbe", "[DEBUG-studio] click $text enabled=${node.isEnabled} label=${node.text} description=${node.contentDescription}")
        if (node.isEnabled) {
            val bounds = Rect().also { node.getBoundsInScreen(it) }
            val now = SystemClock.uptimeMillis()
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                val event = MotionEvent.obtain(now, now + 50, action, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                instrumentation.uiAutomation.injectInputEvent(event, true)
                event.recycle()
            }
        }
        instrumentation.waitForIdleSync()
    }

    private fun waitFor(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (!condition() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(100)
        assertTrue("UI state did not arrive", condition())
    }

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(300)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.cacheDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
