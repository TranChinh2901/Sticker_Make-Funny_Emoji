package com.stickermaker.funnyemoji.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.stickermaker.funnyemoji.data.DraftRepository
import com.stickermaker.funnyemoji.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong


object DraftReminderScheduler {
    const val WORK_NAME = "unfinished-sticker-reminder"
    const val DEFAULT_DELAY_MS = 10 * 1000L 
    // const val DEFAULT_DELAY_MS = 30 * 60 * 1000L 
    private val mutex = Mutex()
    private val revision = AtomicLong()
    @Volatile private var editorActive = false


    fun markActive(): Long { editorActive = true; return revision.incrementAndGet() }
    fun markAway(): Long { editorActive = false; return revision.incrementAndGet() }

    private fun preferences(context: Context) = context.getSharedPreferences("draft-reminder", 0)

    suspend fun cancel(context: Context, ticket: Long) = withContext(Dispatchers.IO) {
        mutex.withLock { if (ticket == revision.get()) clear(context) }
    }

    private fun clear(context: Context) {
        check(preferences(context).edit().remove("work_id").commit())
        DraftNotifications.cancelReminder(context)
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME).result.get()
    }

    suspend fun schedule(context: Context, ticket: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (ticket != revision.get() || editorActive) return@withLock
            val draft = DraftRepository(AppDatabase.getInstance(context).draftDao()).read()
            if (draft?.document?.hasContent != true || !DraftNotifications.isAllowed(context)) {
                clear(context)
                return@withLock
            }
            if (ticket != revision.get() || editorActive) return@withLock
            val request = OneTimeWorkRequestBuilder<DraftReminderWorker>()
                .setInitialDelay(DEFAULT_DELAY_MS, TimeUnit.MILLISECONDS).build()
            check(preferences(context).edit().putString("work_id", request.id.toString()).commit())
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
                .result.get()
        }
    }

    suspend fun deliver(context: Context, workId: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (editorActive || preferences(context).getString("work_id", null) != workId) return@withLock
            val draft = DraftRepository(AppDatabase.getInstance(context).draftDao()).read()
            if (editorActive || preferences(context).getString("work_id", null) != workId) return@withLock
            check(preferences(context).edit().remove("work_id").commit())
            if (draft?.document?.hasContent == true && !editorActive) DraftNotifications.showReminder(context)
        }
    }
}
