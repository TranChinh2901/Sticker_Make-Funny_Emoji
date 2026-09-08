package com.stickermaker.funnyemoji.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

internal class PngExport(
    val busy: Boolean,
    val message: String?,
    val save: (String, suspend () -> ByteArray) -> Unit,
)

/** Render/download before opening the system picker; retain that exact PNG through recreation. */
@Composable
internal fun rememberPngExport(): PngExport {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingFile by rememberSaveable { mutableStateOf<String?>(null) }
    var preparing by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val source = pendingFile?.let { File(context.cacheDir, it) }
        if (uri == null) {
            source?.delete(); pendingFile = null
        } else scope.launch {
            preparing = true
            try {
                withContext(Dispatchers.IO) {
                    check(source != null && source.exists()) { "Export expired" }
                    context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                        source.inputStream().use { it.copyTo(output) }
                    } ?: error("Cannot write image")
                }
                message = "PNG saved to your chosen location."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not save PNG. Choose a location and try again." }
            finally { source?.delete(); pendingFile = null; preparing = false }
        }
    }
    return PngExport(preparing || pendingFile != null, message) { name, bytes ->
        if (!preparing && pendingFile == null) {
            preparing = true; message = null
            scope.launch {
                try {
                    val png = bytes()
                    val fileName = "sticker-export-${UUID.randomUUID()}.png"
                    withContext(Dispatchers.IO) { File(context.cacheDir, fileName).writeBytes(png) }
                    pendingFile = fileName
                    picker.launch(name.replace(Regex("[^\\p{L}\\p{N} _-]"), "_").take(80).ifBlank { "sticker" } + ".png")
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { pendingFile = null; message = "Could not prepare PNG. Try again." }
                finally { preparing = false }
            }
        }
    }
}
