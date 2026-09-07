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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.data.*
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.CancellationException

@Composable
internal fun MyStudioScreen(onCreate: () -> Unit, refresh: Int, onNavigate: (Int) -> Unit,
    onPremium: () -> Unit, onCollection: (StickerCollection) -> Unit, onSticker: (SavedSticker) -> Unit) {
    var collections by remember { mutableStateOf<List<StickerCollection>>(emptyList()) }
    var stickers by remember { mutableStateOf<List<SavedSticker>>(emptyList()) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh, retry, tab) {
        loading = true; error = null
        try {
            if (tab == 0) collections = CollectionRepository.list() else stickers = StudioRepository.list()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Không thể tải dữ liệu. Kiểm tra kết nối rồi thử lại." }
        finally { loading = false }
    }
    Scaffold(containerColor = StickerBackground, modifier = Modifier.navigationBarsPadding(),
        topBar = { BrandHeader("My Studio", onPremium) }, bottomBar = { MainNavigation(2, onNavigate) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill("Collections", tab == 0) { tab = 0 }; Pill("My Stickers", tab == 1) { tab = 1 }
            } }
            item { Button(onClick = if (tab == 0) onCreate else { { onNavigate(1) } }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                Text(if (tab == 0) "+ New Collection" else "+ Create Sticker", fontFamily = Baloo, fontSize = 18.sp)
            } }
            if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (error != null) item { Column { Text(error!!, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = { retry++ }) { Text("Retry") } } }
            if (!loading && error == null && (if (tab == 0) collections.isEmpty() else stickers.isEmpty())) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (tab == 0) "Your creativity starts here" else "Make something uniquely you", fontFamily = Baloo, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (tab == 0) "Create your first sticker collection." else "Your saved stickers will appear here.", fontFamily = Baloo, color = Color.Gray)
                }
            }
            if (tab == 0) items(collections, key = { it.id }) { collection ->
                Surface(Modifier.fillMaxWidth().clickable { onCollection(collection) }, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFFE7F7F3))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        val background = runCatching { Color(android.graphics.Color.parseColor(collection.backgroundColor)) }.getOrDefault(StickerBackground)
                        PrivateImage(collection.thumbnailPath, Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)).background(background), true, "Collection thumbnail")
                        Column(Modifier.weight(1f)) {
                            Text(collection.name, fontFamily = Baloo, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text("View collection", fontFamily = Baloo, color = StickerGreen)
                        }
                        Asset(com.stickermaker.funnyemoji.R.drawable.settings_arrows_chevron_chevron_right, 16.dp)
                    }
                }
            } else items(stickers.chunked(3), key = { it.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { sticker ->
                        Surface(Modifier.weight(1f).clickable { onSticker(sticker) }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, StickerGreen)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                PrivateImage(sticker.imagePath, Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp), label = sticker.name)
                                Text(sticker.name, Modifier.padding(8.dp), fontFamily = Baloo, maxLines = 1)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
