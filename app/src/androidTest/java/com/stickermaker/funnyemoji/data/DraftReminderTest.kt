package com.stickermaker.funnyemoji.data

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.stickermaker.funnyemoji.data.local.AppDatabase
import com.stickermaker.funnyemoji.notifications.DraftNotifications
import com.stickermaker.funnyemoji.notifications.DraftReminderScheduler as Reminders
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftReminderTest {
    @Test fun scheduleReplaceCancelAndDeliverRespectTheCurrentDraft() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = AppDatabase.getInstance(context).draftDao()
        val original = dao.read()
        val repository = DraftRepository(dao)
        val manager = WorkManager.getInstance(context)
        fun pending() = manager.getWorkInfosForUniqueWork(Reminders.WORK_NAME).get().filter { !it.state.isFinished }
        val notifications = context.getSystemService(NotificationManager::class.java)
        fun visible() = notifications.activeNotifications.filter { it.id == DraftNotifications.REMINDER_NOTIFICATION_ID }
        suspend fun awaitNotificationCount(count: Int) {
            repeat(40) {
                if (visible().size == count) return
                delay(50)
            }
            assertEquals(count, visible().size)
        }
        try {
            DraftNotifications.createChannel(context)
            assertTrue("Enable notifications before running this test", DraftNotifications.isAllowed(context))
            Reminders.cancel(context, Reminders.markActive())
            repository.save(StickerDocument(pattern = true))
            val stale = Reminders.markAway()
            Reminders.schedule(context, stale)
            val first = pending().single()
            assertEquals(Reminders.DEFAULT_DELAY_MS, first.initialDelayMillis)
            Reminders.schedule(context, Reminders.markAway())
            val second = pending().single()
            assertNotEquals(first.id, second.id)
            val replaced = manager.getWorkInfoById(first.id).get()
            assertTrue("Replaced work is removed or cancelled", replaced == null || replaced.state == WorkInfo.State.CANCELLED)
            Reminders.cancel(context, Reminders.markActive())
            Reminders.schedule(context, stale)
            assertTrue("Returning before the save finishes invalidates its schedule", pending().isEmpty())

            Reminders.schedule(context, Reminders.markAway())
            val delivery = pending().single()
            Reminders.deliver(context, delivery.id.toString())
            awaitNotificationCount(1)
            notifications.cancel(DraftNotifications.REMINDER_NOTIFICATION_ID)
            awaitNotificationCount(0)
            Reminders.deliver(context, delivery.id.toString())
            delay(200)
            assertTrue("Consumed work cannot notify again", visible().isEmpty())
            assertTrue(repository.read()!!.document.hasContent)

            Reminders.schedule(context, Reminders.markAway())
            val deleted = pending().single()
            repository.delete()
            Reminders.deliver(context, deleted.id.toString())
            assertTrue("Deleted draft must not notify", visible().isEmpty())
            Reminders.schedule(context, Reminders.markAway())
            assertTrue("Empty draft must not schedule", pending().isEmpty())
        } finally {
            Reminders.cancel(context, Reminders.markActive())
            if (original == null) dao.delete() else dao.save(original)
        }
    }
}
