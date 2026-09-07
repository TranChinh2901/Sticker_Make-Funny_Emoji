package com.stickermaker.funnyemoji.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Reads at most 10 MiB and downsamples before decoding untrusted picker images. */
internal suspend fun loadCollectionThumbnail(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val bytes = resolver.openInputStream(uri)?.use { input ->
        val limit = 10 * 1024 * 1024
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            require(output.size() + count <= limit) { "Ảnh phải nhỏ hơn 10 MB." }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: error("Không thể đọc ảnh đã chọn.")
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    require(options.outWidth > 0 && options.outHeight > 0) { "Ảnh không hợp lệ hoặc định dạng không được hỗ trợ." }
    options.inSampleSize = 1
    while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2
    options.inJustDecodeBounds = false
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: error("Không thể giải mã ảnh.")
    try {
        ByteArrayOutputStream().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Không thể xử lý ảnh." }
            output.toByteArray().also { require(it.size <= 5 * 1024 * 1024) { "Ảnh sau xử lý quá lớn." } }
        }
    } finally { bitmap.recycle() }
}
