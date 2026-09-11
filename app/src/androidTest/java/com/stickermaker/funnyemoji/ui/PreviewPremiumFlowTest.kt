package com.stickermaker.funnyemoji.ui

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.stickermaker.funnyemoji.data.FavoritesStore
import com.stickermaker.funnyemoji.data.SavedSticker
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Production preview, unlock and premium components; no sticker writes or purchases. */
@RunWith(AndroidJUnit4::class)
class PreviewPremiumFlowTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun settle() { instrumentation.waitForIdleSync(); SystemClock.sleep(700) }
    private fun nodes(label: String): List<AccessibilityNodeInfo> {
        val matches = mutableListOf<AccessibilityNodeInfo>()
        fun visit(node: AccessibilityNodeInfo) {
            if (!node.refresh()) return
            if (node.text?.toString() == label || node.contentDescription?.toString() == label) matches += node
            repeat(node.childCount) { node.getChild(it)?.let(::visit) }
        }
        instrumentation.uiAutomation.rootInActiveWindow?.let(::visit)
        return matches
    }
    private fun click(label: String, index: Int = 0) {
        val deadline = SystemClock.uptimeMillis() + 5000
        var found = nodes(label)
        while (found.size <= index && SystemClock.uptimeMillis() < deadline) { settle(); found = nodes(label) }
        assertTrue("Missing $label at $index", found.size > index)
        var target: AccessibilityNodeInfo? = found[index]
        while (target != null && !target.isClickable) target = target.parent
        assertNotNull("No clickable parent for $label", target)
        assertTrue(target!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)); settle()
    }
    @Test fun lockedHugsOpenCorrectPreviewAndReturnFromPremium() {
        val selectedHug = mutableStateOf<Int?>(null)
        val premium = mutableStateOf(false)
        val store = FavoritesStore(instrumentation.targetContext)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                WindowCompat.getInsetsController(activity.window, activity.window.decorView).hide(WindowInsetsCompat.Type.systemBars())
                activity.setContent {
                    StickerMakerTheme {
                        CompositionLocalProvider(LocalDensity provides Density(1f, 1f), LocalFavorites provides store) {
                            val saved = rememberSaveableStateHolder()
                            Box(Modifier.fillMaxSize()) {
                                Box(Modifier.size(390.dp, 844.dp).consumeWindowInsets(WindowInsets.navigationBars)) {
                                    saved.SaveableStateProvider(if (premium.value) "premium" else "preview") {
                                        if (premium.value) PremiumScreen { premium.value = false }
                                        else SavedStickerScreen(SavedSticker("preview-fixture", "Preview fixture", "fixture/no-network-image"),
                                            onBack = {}, onNewCollection = {}, onDeleted = {}, onUnlock = { selectedHug.value = it })
                                    }
                                    selectedHug.value?.let { index ->
                                        CatalogUnlockSheet("hug-$index", onDismiss = { selectedHug.value = null },
                                            onPremium = { selectedHug.value = null; premium.value = true })
                                    }
                                }
                            }
                        }
                    }
                }
            }
            settle()
            click("Unlock")
            scenario.onActivity { assertEquals(0, selectedHug.value) }
            assertTrue(nodes("Hug 1").isNotEmpty())
            click("Get Premium")
            click("Monthly Access")
            click("Upgrade Now")
            assertTrue("Billing must remain unavailable", nodes("Chưa khả dụng").isNotEmpty())
            click("Đóng")
            click("Close Premium")
            click("View More")
            fun scrollCards(node: AccessibilityNodeInfo): Boolean {
                node.refresh()
                if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
                repeat(node.childCount) { if (node.getChild(it)?.let(::scrollCards) == true) return true }
                return false
            }
            assertTrue("Hug carousel scrolls", scrollCards(instrumentation.uiAutomation.rootInActiveWindow))
            settle()
            click("Unlock", nodes("Unlock").size - 1)
            scenario.onActivity { assertEquals(2, selectedHug.value) }
            assertTrue(nodes("Hug 3").isNotEmpty())
            click("Get It Free")
            assertTrue("Ads must not grant an unlock", nodes("Chưa khả dụng").isNotEmpty())
            click("Đóng")
            click("Get Premium")
            // The parent saveable-state holder retains the selected monthly plan on re-entry.
            var plan: AccessibilityNodeInfo? = nodes("Monthly Access").firstOrNull()
            while (plan != null && !plan.isCheckable) plan = plan.parent
            assertNotNull(plan); assertTrue(plan!!.isChecked)
            click("Close Premium")
            assertTrue("Return to sticker preview", nodes("Sticker options").isNotEmpty())
            scenario.onActivity { assertNull(selectedHug.value); assertFalse(premium.value) }
        }
    }
    @Test fun catalogueUnlockKeepsTheSelectedIdentityAcrossDifferentCards() {
        val selected = mutableStateOf<String?>(null)
        val premiumRequests = java.util.concurrent.atomic.AtomicInteger()
        val store = FavoritesStore(instrumentation.targetContext)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    StickerMakerTheme {
                        CompositionLocalProvider(LocalFavorites provides store, LocalDensity provides Density(1f, 1f)) {
                            Column(Modifier.fillMaxSize().padding(top = 50.dp)) {
                                listOf("hug-0", "hug-2", "trending-0").forEach { id ->
                                    CatalogCard(id, Modifier.width(120.dp)) { selected.value = it }
                                }
                            }
                            selected.value?.let { id ->
                                CatalogUnlockSheet(id, onDismiss = { selected.value = null },
                                    onPremium = { selected.value = null; premiumRequests.incrementAndGet() })
                            }
                        }
                    }
                }
            }
            settle()
            listOf(1 to "hug-2", 0 to "hug-0", 2 to "trending-0").forEach { (index, id) ->
                click("Unlock", index)
                scenario.onActivity { assertEquals(id, selected.value) }
                val name = com.stickermaker.funnyemoji.data.StickerCatalog.entries.first { it.id == id }.name
                assertTrue("Correct unlock preview: $name", nodes(name).isNotEmpty())
                click("Get Premium")
                scenario.onActivity { assertNull(selected.value) }
            }
            assertEquals(3, premiumRequests.get())
        }
    }

}
