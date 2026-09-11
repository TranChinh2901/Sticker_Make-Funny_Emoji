package com.stickermaker.funnyemoji.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stickermaker.funnyemoji.data.StudioRepository
import kotlinx.coroutines.*
import java.util.UUID

/** Same IDs and offline outbox on Preview, Search, Category and My Studio. */
@Composable
internal fun HugCatalogCard(index: Int, modifier: Modifier = Modifier.width(120.dp), onUnlock: () -> Unit,
    onSaved: () -> Unit = {}) {
    val store = LocalFavorites.current
    val state by store.snapshot.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by rememberSaveable { mutableStateOf(false) }
    var operationId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    BackHandler(enabled = busy) {}
    HugCard(index, "hug-$index" in state.ids, onFavorite = {
        store.choose("hug-$index", "hug-$index" !in state.ids)
        scope.launch { store.sync() }
    }, onUse = { if (index == 1) { confirm = true; error = null; saved = false } else onUnlock() }, modifier, enabled = !busy)
    if (confirm) AlertDialog(onDismissRequest = { if (!busy) confirm = false }, title = { Text(if (saved) "Sticker saved" else "Use Hug ${index + 1}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (saved) "Added to My Studio. Open it there to share or add to a collection." else "Save this image to My Studio?")
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (saved) { confirm = false; return@TextButton }
                busy = true; error = null
                scope.launch {
                    try {
                        val bytes = withContext(Dispatchers.IO) { context.resources.openRawResource(hugAssets[index]).use { it.readBytes() } }
                        StudioRepository.save(operationId, "Hug ${index + 1}", bytes)
                        saved = true; operationId = UUID.randomUUID().toString(); onSaved()
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = "Couldn't save. Check your connection and retry." }
                    finally { busy = false }
                }
            }) { Text(if (saved) "Done" else if (error != null) "Retry" else "Save to My Studio") }
        }, dismissButton = { if (!saved) TextButton(enabled = !busy, onClick = { confirm = false }) { Text("Cancel") } })
}
