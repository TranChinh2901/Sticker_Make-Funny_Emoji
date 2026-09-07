package com.stickermaker.funnyemoji.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.content.FileProvider
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.*
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.*
import java.io.File

@Composable
internal fun PrivateImage(path: String?, modifier: Modifier, thumbnail: Boolean = false, label: String = "Sticker") {
    var image by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(path) { mutableStateOf(false) }
    var retry by remember(path) { mutableIntStateOf(0) }
    LaunchedEffect(path, retry) {
        if (path == null) return@LaunchedEffect
        failed = false
        try {
            val bytes = StudioRepository.image(path, thumbnail)
            image = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
            failed = image == null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            image != null -> Image(image!!, label, Modifier.fillMaxSize())
            failed -> TextButton(onClick = { retry++ }) { Text("Retry", fontSize = 11.sp) }
            path != null -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
internal fun StudioHeader(title: String, onBack: () -> Unit, action: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 40.dp).height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        AssetButton(R.drawable.detail_ion_chevron_back, "Back", onBack, 30.dp)
        Text(title, Modifier.weight(1f).padding(start = 12.dp), fontFamily = Baloo, fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        action()
    }
}

@Composable
internal fun CollectionDetailScreen(collection: StickerCollection, onBack: () -> Unit, onCreate: () -> Unit,
    onChanged: () -> Unit, refresh: Int, onPreview: (SavedSticker) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable(collection.id) { mutableStateOf(collection.name) }
    var stickers by remember { mutableStateOf<List<SavedSticker>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var revision by remember { mutableIntStateOf(0) }
    var more by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var draftName by remember { mutableStateOf(name) }
    var delete by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var remove by remember { mutableStateOf<SavedSticker?>(null) }
    fun mutate(action: suspend () -> Unit) {
        busy = true; error = null
        scope.launch {
            try { action(); revision++; onChanged() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Không thể cập nhật. Kiểm tra kết nối rồi thử lại." }
            finally { busy = false }
        }
    }
    LaunchedEffect(collection.id, revision, refresh) {
        loading = true; error = null
        try { stickers = StudioRepository.list(collection.id) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Không thể tải sticker. Kiểm tra kết nối rồi thử lại." }
        finally { loading = false }
    }
    Scaffold(containerColor = StickerBackground, modifier = Modifier.navigationBarsPadding(),
        topBar = { StudioHeader(name, { if (!busy) onBack() }) {
            Box {
                AssetButton(R.drawable.detail_mingcute_more2_line, "Collection options", { more = true })
                DropdownMenu(more, { more = false }) {
                    DropdownMenuItem(text = { Text("Rename collection") }, onClick = { more = false; draftName = name; rename = true })
                    DropdownMenuItem(text = { Text("Delete collection") }, onClick = { more = false; delete = true })
                }
            }
        } }, bottomBar = {
            Button(onClick = { picker = true }, enabled = !busy, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)) {
                Asset(R.drawable.detail_svg1, 20.dp); Spacer(Modifier.width(8.dp)); Text("Add More Stickers")
            }
        }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFE7F7F3)), shadowElevation = 1.dp) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val color = runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(collection.backgroundColor)) }.getOrDefault(StickerBackground)
                        PrivateImage(collection.thumbnailPath, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(color), true, "Collection thumbnail")
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(name, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(if (loading) "Loading…" else "${stickers.size} stickers", fontFamily = Inter, fontSize = 14.sp)
                            Text("My collection", color = StickerGreen, fontFamily = Inter, fontSize = 14.sp)
                        }
                    }
                }
            }
            item { Text("Stickers", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (error != null) item { Column { Text(error!!, color = MaterialTheme.colorScheme.error); TextButton(onClick = { revision++ }) { Text("Retry") } } }
            if (loading || busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item {
                OutlinedButton(onClick = { picker = true }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, StickerGreen), modifier = Modifier.size(104.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(28.dp).background(StickerGreen, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { Asset(R.drawable.detail_svg, 17.5.dp) }
                        Text("Add")
                    }
                }
            }
            items(stickers.chunked(3), key = { it.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { sticker ->
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, StickerGreen)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PrivateImage(sticker.imagePath, Modifier.fillMaxWidth().aspectRatio(1f).clickable { onPreview(sticker) }.padding(8.dp), label = sticker.name)
                                Text(sticker.name, maxLines = 1, fontFamily = Baloo, fontSize = 12.sp)
                                TextButton(enabled = !busy, onClick = { remove = sticker }) { Text("Remove", fontSize = 11.sp) }
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
    if (rename) AlertDialog(onDismissRequest = { if (!busy) rename = false }, title = { Text("Rename collection") }, text = {
        Column { OutlinedTextField(draftName, { draftName = it.take(80) }, enabled = !busy, singleLine = true); error?.let { Text(it) } }
    }, confirmButton = { TextButton(enabled = !busy && draftName.isNotBlank(), onClick = { mutate {
        CollectionRepository.rename(collection.id, draftName); name = draftName.trim(); rename = false
    } }) { Text("Save") } }, dismissButton = { TextButton(enabled = !busy, onClick = { rename = false }) { Text("Cancel") } })
    if (delete) AlertDialog(onDismissRequest = { if (!busy) delete = false }, title = { Text("Delete collection?") },
        text = { Column { Text("The collection will be removed. Your stickers remain in My Studio."); error?.let { Text(it) } } },
        confirmButton = { TextButton(enabled = !busy, onClick = { mutate { CollectionRepository.delete(collection.id); delete = false; onBack() } }) { Text("Delete") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { delete = false }) { Text("Cancel") } })
    remove?.let { sticker -> AlertDialog(onDismissRequest = { if (!busy) remove = null }, title = { Text("Remove sticker?") },
        text = { Column { Text("Remove ${sticker.name} from this collection?"); error?.let { Text(it) } } },
        confirmButton = { TextButton(enabled = !busy, onClick = { mutate { StudioRepository.remove(collection.id, sticker.id); remove = null } }) { Text("Remove") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { remove = null }) { Text("Cancel") } }) }
    if (picker) StickerPicker(collection.id, stickers.map { it.id }.toSet(), onDismiss = { picker = false },
        onAdded = { revision++; onChanged() }, onCreate = { picker = false; onCreate() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StickerPicker(collectionId: String, existing: Set<String>, onDismiss: () -> Unit, onAdded: () -> Unit, onCreate: () -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<SavedSticker>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(retry) {
        loading = true; error = null
        try { items = StudioRepository.list() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Không thể tải sticker." }
        finally { loading = false }
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 540.dp).padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Text("Add to Collection", fontFamily = Baloo, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            item { Button(enabled = !busy, onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Create a new sticker") } }
            if (loading || busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (error != null) item { Text(error!!); TextButton(onClick = { retry++ }) { Text("Retry") } }
            if (!loading && items.isEmpty() && error == null) item { Text("No saved stickers yet. Create your first one.", Modifier.padding(vertical = 20.dp)) }
            items(items, key = { it.id }) { sticker ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrivateImage(sticker.imagePath, Modifier.size(56.dp), label = sticker.name)
                    Text(sticker.name, Modifier.weight(1f).padding(12.dp))
                    TextButton(enabled = !busy && sticker.id !in existing, onClick = {
                        busy = true; error = null
                        scope.launch {
                            try { StudioRepository.add(collectionId, sticker.id); onAdded(); onDismiss() }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { error = "Không thể thêm sticker. Thử lại." }
                            finally { busy = false }
                        }
                    }) { Text(if (sticker.id in existing) "Added" else "Add") }
                }
            }
        }
    }
}

@Composable
internal fun SavedStickerScreen(sticker: SavedSticker, onBack: () -> Unit, onNewCollection: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var collections by remember { mutableStateOf<List<StickerCollection>>(emptyList()) }
    var chooseCollection by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(StickerBackground).navigationBarsPadding()) {
        StudioHeader("Preview sticker", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PrivateImage(sticker.imagePath, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)).background(androidx.compose.ui.graphics.Color.White), label = sticker.name)
            Text(sticker.name, fontFamily = Baloo, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), onClick = {
                busy = true; error = null
                scope.launch {
                    try {
                        val bytes = StudioRepository.image(sticker.imagePath)
                        val file = withContext(Dispatchers.IO) {
                            File(context.cacheDir, "shared").mkdirs()
                            File(context.cacheDir, "shared/${sticker.id}.png").apply { writeBytes(bytes) }
                        }
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                        val intent = Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(intent, "Share sticker"))
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = "Không thể chia sẻ ảnh. Kiểm tra kết nối và thử lại." }
                    finally { busy = false }
                }
            }) { Text("Share PNG") }
            Button(enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), onClick = {
                busy = true; error = null
                scope.launch {
                    try { collections = CollectionRepository.list(); chooseCollection = true }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = "Không thể tải bộ sưu tập." }
                    finally { busy = false }
                }
            }) { Text("Add to Collection") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            message?.let { Text(it, color = StickerGreen) }
        }
    }
    if (chooseCollection) AlertDialog(onDismissRequest = { if (!busy) chooseCollection = false }, title = { Text("Choose collection") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            if (collections.isEmpty()) Text("Create a collection to organize your stickers.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            collections.forEach { collection ->
                TextButton(enabled = !busy, onClick = {
                    busy = true; error = null
                    scope.launch {
                        try { StudioRepository.add(collection.id, sticker.id); chooseCollection = false; message = "Added to ${collection.name}" }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Không thể thêm sticker. Thử lại." }
                        finally { busy = false }
                    }
                }) { Text(collection.name) }
            }
        }
    }, confirmButton = { TextButton(enabled = !busy, onClick = { chooseCollection = false; onNewCollection() }) { Text("New Collection") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { chooseCollection = false }) { Text("Cancel") } })
}
