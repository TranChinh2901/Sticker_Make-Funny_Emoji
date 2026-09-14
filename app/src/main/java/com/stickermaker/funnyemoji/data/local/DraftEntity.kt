package com.stickermaker.funnyemoji.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One active draft, matching the editor's existing single-draft behavior. */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: Long = 1L,
    // Serialized StickerDocument; imported images remain in the app's files directory.
    val documentJson: String,
    val updatedAt: Long,
    val collectionId: String? = null,
)
