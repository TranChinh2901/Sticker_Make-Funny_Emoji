package com.stickermaker.funnyemoji.data

import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Guest sessions retain per-user RLS without presenting a login screen. */
internal object DemoSession {
    private val mutex = Mutex()

    suspend fun userId(): String = mutex.withLock {
        check(SupabaseProvider.isConfigured) { "Chưa cấu hình Supabase." }
        val auth = SupabaseProvider.client.auth
        // Wait for the persisted session before creating a new guest identity.
        auth.awaitInitialization()
        if (auth.currentUserOrNull() == null) auth.signInAnonymously()
        checkNotNull(auth.currentUserOrNull()) {
            "Không thể tạo phiên khách. Kiểm tra Anonymous Sign-Ins trong Supabase."
        }.id
    }
}
