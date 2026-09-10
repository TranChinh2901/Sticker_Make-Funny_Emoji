package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.os.Bundle
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.stickermaker.funnyemoji.MainActivity
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.StickerCollection
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class EditorDraftExitTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    @Test fun backWaitsForDraftAndRetainsEditorOnFailure() {
        val fail = AtomicBoolean(false)
        val block = AtomicBoolean(false)
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val exits = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity -> activity.setContent {
                    StickerMakerTheme {
                        EditorScreen(onBack = { exits.incrementAndGet() }, onSaved = {}, saveDraft = {
                            if (fail.get()) throw IOException("Simulated device write failure")
                            if (block.get()) {
                                started.countDown()
                                check(release.await(5, TimeUnit.SECONDS))
                            }
                        })
                    }
                } }
                instrumentation.waitForIdleSync()
                SystemClock.sleep(1500)
                waitFor { button("Back")?.isEnabled == true }
                fail.set(true)
                click("Back")
                waitFor { findText("Couldn't save your draft.") != null }
                assertEquals("Failed save stays in editor", 0, exits.get())
                fail.set(false); block.set(true)
                click("Back")
                assertTrue(started.await(3, TimeUnit.SECONDS))
                assertEquals("Navigation must wait for persistence", 0, exits.get())
                click("Back", enabled = false)
                release.countDown()
                waitFor { exits.get() == 1 }
            } finally { release.countDown() }
        }
    }

    private fun descendants(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (node == null || !node.refresh()) return emptyList()
        return listOf(node) + (0 until node.childCount).flatMap { descendants(node.getChild(it)) }
    }

    private fun findText(text: String) = descendants(instrumentation.uiAutomation.rootInActiveWindow)
        .firstOrNull { it.text?.toString()?.contains(text) == true || it.contentDescription?.toString()?.contains(text) == true }

    private fun button(text: String): AccessibilityNodeInfo? {
        var node = findText(text) ?: return null
        while (!node.isClickable && node.parent != null) node = node.parent.also { it.refresh() }
        node.refresh()
        return node
    }

    private fun click(text: String, enabled: Boolean = true) {
        waitFor { button(text)?.isEnabled == enabled }
        val node = requireNotNull(button(text))
        if (enabled) {
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
