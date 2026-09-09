package com.stickermaker.funnyemoji.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.data.CollectionRepository
import com.stickermaker.funnyemoji.data.StickerCollection
import com.stickermaker.funnyemoji.data.StudioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionPicker(
    stickerId: String, onDismiss: () -> Unit, onCreate: () -> Unit, onAdded: (StickerCollection) -> Unit,
    load: suspend () -> Pair<List<StickerCollection>, Set<String>> = { CollectionRepository.list() to StudioRepository.collectionsFor(stickerId) },
    add: suspend (String) -> Unit = { StudioRepository.add(it, stickerId) },
) {
    var collections by remember { mutableStateOf<List<StickerCollection>>(emptyList()) }
    var memberships by remember { mutableStateOf<Set<String>>(emptySet()) }
    var query by rememberSaveable(stickerId) { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(stickerId, retry) {
        loading = true; loadError = false
        try { val result = load(); collections = result.first; memberships = result.second }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadError = true }
        finally { loading = false }
    }
    val busy = saving != null
    val filtered = collections.filter { it.name.contains(query.trim(), ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = { if (!busy) onDismiss() }, containerColor = Color(0xFFF5FFFD),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { it != SheetValue.Hidden || !busy })) {
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).imePadding().padding(horizontal = 20.dp)) {
            Text("Choose collection", fontFamily = StudioFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(query, { query = it }, enabled = !busy, singleLine = true,
                placeholder = { Text("Search collections", fontFamily = StudioFont) },
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = StudioFont, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
            TextButton(onClick = onCreate, enabled = !busy) { Text("New Collection", fontFamily = StudioFont) }
            if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(Modifier.weight(1f, fill = false), contentPadding = PaddingValues(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    loadError -> item { Column {
                        Text("Couldn't load collections. Please try again.", fontFamily = StudioFont)
                        TextButton(onClick = { retry++ }) { Text("Retry") }
                    } }
                    !loading && collections.isEmpty() -> item { Text("Create a collection to organize your stickers.", fontFamily = StudioFont) }
                    !loading && filtered.isEmpty() -> item { Text("No collections match your search.", fontFamily = StudioFont) }
                }
                if (!loading && !loadError) items(filtered, key = { it.id }) { collection ->
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE7F7F3))) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(collection.name, Modifier.weight(1f), fontFamily = StudioFont, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            TextButton(enabled = !busy && collection.id !in memberships, onClick = {
                                if (!busy) {
                                    saving = collection.id; saveError = false
                                    scope.launch {
                                        try {
                                            add(collection.id)
                                            memberships = memberships + collection.id
                                            onAdded(collection)
                                            onDismiss()
                                        } catch (cancelled: CancellationException) { throw cancelled }
                                        catch (_: Exception) { saveError = true }
                                        finally { saving = null }
                                    }
                                }
                            }) { Text(if (collection.id in memberships) "Already added" else "Add", fontFamily = StudioFont) }
                        }
                    }
                }
            }
            if (saveError) Text("Couldn't add sticker. Please try again.", fontFamily = StudioFont, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel", fontFamily = StudioFont) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
