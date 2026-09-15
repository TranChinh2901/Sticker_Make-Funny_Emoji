package com.stickermaker.funnyemoji.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DraftDao {
    @Upsert
    suspend fun save(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE id = 1")
    suspend fun read(): DraftEntity?

    @Query("DELETE FROM drafts WHERE id = 1")
    suspend fun delete()
}
