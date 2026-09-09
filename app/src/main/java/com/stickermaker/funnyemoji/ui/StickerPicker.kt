package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.data.SavedSticker
import com.stickermaker.funnyemoji.data.StudioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StickerPicker(
    collectionId: String, existing: Set<String>, onDismiss: () -> Unit,
    onAdded: () -> Unit, onCreate: () -> Unit,
    load: suspend () -> List<SavedSticker> = { StudioRepository.list() },
    add: suspend (Set<String>) -> Unit = { StudioRepository.addAll(collectionId, it) },
    image: @Composable (SavedSticker, Modifier) -> Unit = { sticker, modifier -> PrivateImage(sticker.imagePath, modifier, label = sticker.name) },
) {
    val scope = rememberCoroutineScope()
    var stickers by remember(collectionId) { mutableStateOf<List<SavedSticker>>(emptyList()) }
    var selected by rememberSaveable(collectionId) { mutableStateOf(arrayListOf<String>()) }
    var query by rememberSaveable(collectionId) { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(collectionId, retry) {
        loading = true; loadError = false
        try {
            stickers = load()
            selected = ArrayList(selected.filter { id -> stickers.any { it.id == id } && id !in existing })
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadError = true }
        finally { loading = false }
    }
    val selection = selected.filter { it !in existing }.toSet()
    val filtered = stickers.filter { it.name.contains(query.trim(), ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() },
        containerColor = Color(0xFFF5FFFD), scrimColor = Color.Black.copy(alpha = .6f),
        dragHandle = { Box(Modifier.padding(top = 12.dp, bottom = 24.dp).size(40.dp, 6.dp).background(Color(0xFF9CA3AF), RoundedCornerShape(3.dp))) },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !busy })) {
        Column(Modifier.fillMaxWidth().imePadding().heightIn(max = 620.dp).padding(horizontal = 20.dp)) {
            Text("Add to Collection", fontFamily = StudioFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(query, { query = it }, enabled = !busy, singleLine = true,
                placeholder = { Text("Search stickers", fontFamily = StudioFont) },
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White, unfocusedBorderColor = Color(0xFFD1D5DB), focusedBorderColor = Color(0xFF00B686)),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = StudioFont, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            TextButton(onClick = onCreate, enabled = !busy) { Text("Create a new sticker", fontFamily = StudioFont) }
            if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    loadError -> item { Column {
                        Text("Couldn't load stickers.", fontFamily = StudioFont)
                        TextButton(onClick = { retry++ }) { Text("Retry") }
                    } }
                    !loading && stickers.isEmpty() -> item { Text("No saved stickers yet. Create your first one.", fontFamily = StudioFont) }
                    !loading && filtered.isEmpty() -> item { Text("No stickers match your search.", fontFamily = StudioFont) }
                }
                if (!loadError) items(filtered, key = { it.id }) { sticker ->
                    val alreadyAdded = sticker.id in existing
                    val checked = alreadyAdded || sticker.id in selection
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
                        .border(1.dp, if (checked) Color(0xFF00B686) else Color(0xFFE7F7F3), RoundedCornerShape(12.dp))
                        .toggleable(value = checked, enabled = !busy && !loading && !alreadyAdded, role = Role.Checkbox) { value ->
                            selected = ArrayList(if (value) selected + sticker.id else selected - sticker.id)
                            saveError = false
                        }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        image(sticker, Modifier.size(48.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(sticker.name, fontFamily = StudioFont, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (alreadyAdded) Text("Already added", color = Color(0xFF00B686), fontFamily = StudioFont, fontSize = 12.sp)
                        }
                        Checkbox(checked = checked, onCheckedChange = null, enabled = !busy && !alreadyAdded)
                    }
                }
            }
            if (saveError) Text("Couldn't add stickers. Your selection is kept; try again.", fontFamily = StudioFont,
                fontSize = 13.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
            Button(colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B686)), enabled = !busy && !loading && !loadError && selection.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp), shape = RoundedCornerShape(12.dp), onClick = {
                    if (!busy) {
                        busy = true; saveError = false
                        scope.launch {
                            try { add(selection); onAdded(); onDismiss() }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { saveError = true }
                            finally { busy = false }
                        }
                    }
                }) { Text(if (busy) "Adding…" else "Add selected (${selection.size})", fontFamily = StudioFont, fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel", fontFamily = StudioFont) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
