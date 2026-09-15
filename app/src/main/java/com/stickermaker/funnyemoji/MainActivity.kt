package com.stickermaker.funnyemoji

import android.os.Bundle
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.stickermaker.funnyemoji.notifications.DraftReminderScheduler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stickermaker.funnyemoji.ui.StickerApp
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme
import com.stickermaker.funnyemoji.notifications.DraftNotifications

class MainActivity : ComponentActivity() {
    private var resumeDraftRequest by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resumeDraftRequest = savedInstanceState?.getLong("resume_draft_request") ?: 0L
        handleDraftIntent(intent)
        DraftNotifications.createChannel(this)
        enableEdgeToEdge()
        setContent { StickerMakerTheme { StickerApp(resumeDraftRequest) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDraftIntent(intent)
    }

    private fun handleDraftIntent(intent: Intent) {
        if (intent.action != DraftNotifications.ACTION_RESUME_DRAFT) return
        intent.action = null
        resumeDraftRequest++
        val ticket = DraftReminderScheduler.markActive()
        lifecycleScope.launch {
            try { DraftReminderScheduler.cancel(applicationContext, ticket) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* The editor retries cancellation on start. */ }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("resume_draft_request", resumeDraftRequest)
        super.onSaveInstanceState(outState)
    }
}
