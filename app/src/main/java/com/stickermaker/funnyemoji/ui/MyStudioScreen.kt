package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.*
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.*

internal val StudioFont = FontFamily(
    Font(R.font.be_vietnam_pro_regular),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)
private val StudioGreen = Color(0xFF00B686)
private val StudioText = Color(0xFF1A1C1D)

internal data class StudioOverview(
    val collections: List<StickerCollection> = emptyList(),
    val stickers: List<SavedSticker> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val favorites: Int = 0,
    val drafts: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
)

@Composable
internal fun MyStudioScreen(onCreate: () -> Unit, refresh: Int, onNavigate: (Int) -> Unit,
    onPremium: () -> Unit, onCollection: (StickerCollection) -> Unit, onSticker: (SavedSticker) -> Unit, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val favorites by LocalFavorites.current.snapshot.collectAsState()
    var overview by remember { mutableStateOf(StudioOverview(loading = true)) }
    var retry by remember { mutableIntStateOf(0) }
    var section by rememberSaveable { mutableStateOf("COLLECTIONS") }
    var menu by remember { mutableStateOf<StickerCollection?>(null) }
    var action by remember { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var mutationError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(refresh, retry) {
        overview = overview.copy(loading = true, error = null)
        val draftCount = withContext(Dispatchers.IO) { if (EditorDraft.read(context).hasContent) 1 else 0 }
        overview = overview.copy(drafts = draftCount)
        try {
            val collections = CollectionRepository.list()
            val stickers = StudioRepository.list()
            val counts = StudioRepository.collectionCounts()
            overview = overview.copy(collections = collections, stickers = stickers, counts = counts, loading = false)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            overview = overview.copy(loading = false, error = "Couldn't load your collections")
        }
    }
    val display = overview.copy(favorites = favorites.ids.size)
    MyStudioLayout(display, section, onCreate, onPremium, onNavigate,
        onSection = { if (it == "DRAFTS") onNavigate(1) else section = it },
        onCollection = onCollection, onOptions = { menu = it; mutationError = null },
        onRetry = { retry++ },
    ) {
        if (section == "FAVORITES") {
            item { FavoriteSyncStatus() }
            val entries = StickerCatalog.entries.filter { it.id in favorites.ids }
            if (entries.isEmpty()) item { StudioMessage("No favorites yet", "Tap a heart to keep a sticker here.") }
            items(entries.chunked(3), key = { it.first().id }) { row ->
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { CatalogCard(it.id, Modifier.weight(1f), onUnlock) }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        } else if (section == "RECENT") {
            if (overview.stickers.isEmpty() && overview.error == null && !overview.loading)
                item { StudioMessage("No stickers yet", "Create your first sticker.") }
            if (overview.error != null) item { StudioMessage("Couldn't load your stickers", "Please try again.", { retry++ }) }
            items(overview.stickers.chunked(3), key = { it.first().id }) { row ->
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { sticker ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White)
                            .clickable { onSticker(sticker) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            PrivateImage(sticker.imagePath, Modifier.fillMaxWidth().aspectRatio(1f), label = sticker.name)
                            StudioLabel(sticker.name, 12, maxLines = 1)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    menu?.let { collection ->
        AlertDialog(onDismissRequest = { if (!busy) { menu = null; action = null } },
            title = { Text(action?.let { "$it collection" } ?: collection.name, fontFamily = StudioFont) },
            text = {
                Column {
                    when (action) {
                        "Rename" -> OutlinedTextField(name, { name = it.take(80) }, singleLine = true, enabled = !busy, label = { Text("Collection name") })
                        "Delete" -> Text("Delete this collection? Your stickers will remain in My Studio.")
                        else -> {
                            TextButton(onClick = { name = collection.name; action = "Rename" }) { Text("Rename collection") }
                            TextButton(onClick = { action = "Delete" }) { Text("Delete collection") }
                        }
                    }
                    mutationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                if (action != null) TextButton(enabled = !busy && (action != "Rename" || name.isNotBlank()), onClick = {
                    busy = true; mutationError = null
                    scope.launch {
                        try {
                            if (action == "Rename") CollectionRepository.rename(collection.id, name)
                            else CollectionRepository.delete(collection.id)
                            menu = null; action = null; retry++
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { mutationError = "Couldn't update collection. Please try again." }
                        finally { busy = false }
                    }
                }) { Text(if (action == "Delete") "Delete" else "Save") }
            }, dismissButton = { TextButton(enabled = !busy, onClick = { menu = null; action = null }) { Text("Cancel") } })
    }
}

@Composable
internal fun MyStudioLayout(
    state: StudioOverview,
    section: String = "COLLECTIONS",
    onCreate: () -> Unit = {}, onPremium: () -> Unit = {}, onNavigate: (Int) -> Unit = {},
    onSection: (String) -> Unit = {}, onCollection: (StickerCollection) -> Unit = {},
    onOptions: (StickerCollection) -> Unit = {}, onRetry: () -> Unit = {},
    thumbnail: @Composable (StickerCollection, Modifier) -> Unit = { collection, modifier ->
        PrivateImage(collection.thumbnailPath, modifier, thumbnail = true, label = collection.name)
    },
    extraContent: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {},
) {
    Scaffold(containerColor = StickerBackground, modifier = Modifier.navigationBarsPadding(),
        topBar = {
            Box(Modifier.fillMaxWidth().height(100.dp)) {
                Box(Modifier.offset(16.0601.dp, 54.17.dp)) {
                    Asset(R.drawable.studio_title, 144.039.dp, 38.dp, description = "My Studio")
                }
                Box(Modifier.align(Alignment.TopEnd).padding(end = 20.dp, top = 58.dp).size(28.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onPremium, modifier = Modifier.requiredSize(48.dp)) {
                        Asset(R.drawable.studio_crown, 28.dp, description = "Get Premium")
                    }
                }
            }
        }, bottomBar = { MainNavigation(2, onNavigate) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item("summary") {
                val stats = listOf("RECENT", "FAVORITES", "DRAFTS", "COLLECTIONS")
                val icons = listOf(R.drawable.studio_recent, R.drawable.studio_favorites, R.drawable.studio_drafts, R.drawable.studio_collections)
                val counts = listOf(if (state.loading || state.error != null) "—" else state.stickers.size.toString(),
                    state.favorites.toString(), state.drafts.toString(),
                    if (state.loading || state.error != null) "—" else state.collections.size.toString())
                Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.forEachIndexed { i, label ->
                        Box(Modifier.weight(1f).height(87.dp).clip(RoundedCornerShape(12.dp))
                            .background(Color.White).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onSection(label) }) {
                            Box(Modifier.align(Alignment.TopCenter).padding(top = 9.dp)) { Asset(icons[i], 32.dp) }
                            StudioLabel(counts[i], 16, Modifier.align(Alignment.TopCenter).padding(top = 47.dp), FontWeight.Bold, lineHeight = 24)
                            StudioLabel(label, 8, Modifier.align(Alignment.TopCenter).padding(top = 67.dp),
                                color = Color(0xFF5D5E5E), lineHeight = 12, letterSpacing = .5f)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp))
                    .background(StudioGreen).clickable(role = Role.Button, onClick = onCreate).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(48.dp).background(Color.White.copy(alpha = .2f), CircleShape), contentAlignment = Alignment.Center) {
                        Asset(R.drawable.studio_plus, 24.dp)
                    }
                    Column(Modifier.requiredHeight(49.dp).offset(y = 3.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StudioLabel("New Collection", 16, weight = FontWeight.Bold, color = Color.White, lineHeight = 24, letterSpacing = -.15f)
                        StudioLabel("Create pack", 14, color = Color.White.copy(alpha = .8f), lineHeight = 21)
                    }
                }
                Spacer(Modifier.height(24.dp))
                StudioLabel(when (section) { "FAVORITES" -> "Favorites"; "RECENT" -> "Recent Stickers"; else -> "My Collections" },
                    18, Modifier.padding(horizontal = 24.dp).offset(y = .5.dp).height(27.dp), FontWeight.Bold, lineHeight = 27)
                Spacer(Modifier.height(18.dp))
            }
            if (section == "COLLECTIONS") {
                when {
                    state.loading -> item { StudioMessage("Loading collections…", "", null) }
                    state.error != null -> item { StudioMessage(state.error, "Check your connection and try again.", onRetry) }
                    state.collections.isEmpty() -> item {
                        Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(190.dp)
                            .background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(12.dp))) {
                            Box(Modifier.align(Alignment.TopCenter).padding(top = 20.dp)) { Asset(R.drawable.studio_empty_folder, 40.dp) }
                            StudioLabel("No collections yet", 16, Modifier.align(Alignment.TopCenter).padding(top = 69.5.dp), FontWeight.Bold, lineHeight = 24, letterSpacing = -.22f)
                            Row(Modifier.align(Alignment.TopCenter).padding(top = 113.dp).width(227.42.dp).height(56.dp)
                                .clip(CircleShape).background(StudioGreen).clickable(role = Role.Button, onClick = onCreate),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                                Asset(R.drawable.studio_plus, 20.dp)
                                StudioLabel("Create Collection", 16, weight = FontWeight.SemiBold, color = Color.White, lineHeight = 24)
                            }
                        }
                    }
                    else -> items(state.collections.chunked(2), key = { it.first().id }) { row ->
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEach { collection ->
                                Box(Modifier.weight(1f).height(163.22.dp).clip(RoundedCornerShape(24.dp))
                                    .background(runCatching { Color(android.graphics.Color.parseColor(collection.backgroundColor)) }.getOrDefault(Color(0xFFE7F7F3)))
                                    .border(1.dp, Color(0xFFFCE7F3).copy(alpha = .5f), RoundedCornerShape(24.dp))
                                    .clickable { onCollection(collection) }) {
                                    StudioLabel(collection.name, 15, Modifier.padding(start = 13.dp, end = 38.dp, top = 12.dp),
                                        FontWeight.SemiBold, Color(0xFF111827), lineHeight = 23, maxLines = 1)
                                    IconButton(onClick = { onOptions(collection) }, modifier = Modifier.align(Alignment.TopEnd).offset(3.dp, (-3).dp).size(48.dp)) {
                                        Asset(R.drawable.studio_collection_more, 24.dp, description = "Options for ${collection.name}")
                                    }
                                    thumbnail(collection, Modifier.padding(start = 17.dp, end = 17.dp, top = 41.75.dp).fillMaxWidth().height(72.47.dp).clip(RoundedCornerShape(8.dp)))
                                    StudioLabel("${state.counts[collection.id] ?: 0} stickers", 11,
                                        Modifier.padding(start = 17.dp, top = 132.47.dp), color = Color(0xFF6B7280), lineHeight = 17)
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            } else extraContent()
        }
    }
}

@Composable
private fun StudioMessage(title: String, subtitle: String, onRetry: (() -> Unit)? = null) {
    Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().heightIn(min = 190.dp)
        .background(Color.White, RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(12.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
        StudioLabel(title, 16, weight = FontWeight.Bold)
        if (subtitle.isNotEmpty()) StudioLabel(subtitle, 12, color = Color(0xFF6B7280))
        if (onRetry != null) TextButton(onClick = onRetry) { Text("Retry", fontFamily = StudioFont) }
    }
}

@Composable
internal fun StudioLabel(value: String, size: Int, modifier: Modifier = Modifier, weight: FontWeight = FontWeight.Normal,
    color: Color = StudioText, lineHeight: Int = size * 3 / 2, maxLines: Int = Int.MAX_VALUE, letterSpacing: Float = 0f) {
    Text(value, modifier, color = color, fontFamily = StudioFont, fontSize = size.sp, fontWeight = weight,
        lineHeight = lineHeight.sp, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false), letterSpacing = letterSpacing.sp))
}
