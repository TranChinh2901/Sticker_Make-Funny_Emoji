package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.SavedSticker
import com.stickermaker.funnyemoji.data.StickerCollection
import com.stickermaker.funnyemoji.ui.theme.Baloo
import com.stickermaker.funnyemoji.ui.theme.StickerBackground

private val DetailGreen = Color(0xFF00B686)

@Composable
internal fun CollectionDetailLayout(
    collection: StickerCollection,
    stickers: List<SavedSticker>,
    loading: Boolean = false,
    busy: Boolean = false,
    error: String? = null,
    onBack: () -> Unit = {},
    onAdd: () -> Unit = {},
    onPreview: (SavedSticker) -> Unit = {},
    onRemove: (SavedSticker) -> Unit = {},
    onRetry: () -> Unit = {},
    options: @Composable () -> Unit = {},
    thumbnail: @Composable (Modifier) -> Unit = { modifier ->
        PrivateImage(collection.thumbnailPath, modifier, thumbnail = true, label = collection.name)
    },
    stickerImage: @Composable (SavedSticker, Modifier) -> Unit = { sticker, modifier ->
        PrivateImage(sticker.imagePath, modifier, label = sticker.name)
    },
) {
    val canAdd = !loading && !busy && error == null
    Scaffold(containerColor = StickerBackground, modifier = Modifier.navigationBarsPadding(),
        topBar = {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 47.dp).height(48.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !busy) { Asset(R.drawable.detail_ion_chevron_back, 30.dp, description = "Back") }
                Text(collection.name, Modifier.weight(1f).padding(start = 12.dp), fontFamily = Baloo,
                    fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 38.sp, color = Color(0xFF1F2937),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                options()
            }
        }, bottomBar = {
            Box(Modifier.background(Brush.verticalGradient(listOf(Color(0x00F8F9FA), Color(0xFFF8F9FA))))
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)) {
                Button(onClick = onAdd, enabled = canAdd, modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = DetailGreen)) {
                    Asset(R.drawable.detail_svg1, 20.dp)
                    Spacer(Modifier.width(8.dp))
                    StudioLabel("Add More Stickers", 16, weight = FontWeight.SemiBold, color = Color.White, lineHeight = 24)
                }
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val cellWidth = minOf(104.66.dp, (maxWidth - 56.dp) / 3)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 20.dp)) {
                item("info") {
                    Surface(color = Color.White, shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE7F7F3)), shadowElevation = 1.dp) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            val color = runCatching { Color(android.graphics.Color.parseColor(collection.backgroundColor)) }.getOrDefault(Color.White)
                            thumbnail(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(color))
                            Column(Modifier.weight(1f)) {
                                StudioLabel(collection.name, 18, Modifier.height(23.dp), weight = FontWeight.Bold, color = Color(0xFF191C1D), lineHeight = 23, maxLines = 1)
                                Spacer(Modifier.height(2.dp))
                                StudioLabel(when { loading -> "Loading…"; error != null -> "Count unavailable"; else -> if (stickers.size == 1) "1 sticker" else "${stickers.size} stickers" },
                                    14, Modifier.height(20.dp), color = Color(0xFF40484A), lineHeight = 20)
                                Spacer(Modifier.height(4.dp))
                                StudioLabel("My collection", 14, Modifier.height(20.dp), weight = FontWeight.SemiBold, color = DetailGreen, lineHeight = 20)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    StudioLabel("Stickers", 18, Modifier.height(28.dp), weight = FontWeight.Bold, color = Color(0xFF191C1D), lineHeight = 28)
                    Spacer(Modifier.height(12.dp))
                }
                if (loading || busy) item("progress") {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = DetailGreen)
                    Spacer(Modifier.height(12.dp))
                }
                if (error != null) item("error") {
                    Text(error, fontFamily = StudioFont, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, enabled = !busy) { Text("Retry", fontFamily = StudioFont) }
                }
                val cells: List<SavedSticker?> = listOf(null) + stickers
                items(cells.chunked(3), key = { row -> row.first()?.id ?: "add" }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { sticker ->
                            if (sticker == null) {
                                Box(Modifier.width(cellWidth).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.White)
                                    .border(1.dp, DetailGreen, RoundedCornerShape(12.dp))
                                    .clickable(enabled = canAdd, role = Role.Button, onClick = onAdd), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(Modifier.size(28.dp).background(DetailGreen, CircleShape), contentAlignment = Alignment.Center) { Asset(R.drawable.detail_svg, 17.5.dp) }
                                        StudioLabel("Add", 14, weight = FontWeight.SemiBold, color = DetailGreen, lineHeight = 20)
                                    }
                                }
                            } else {
                                Column(Modifier.width(cellWidth).clip(RoundedCornerShape(12.dp)).background(Color.White)
                                    .border(1.dp, Color(0xFFE7F7F3), RoundedCornerShape(12.dp))) {
                                    Box(Modifier.fillMaxWidth().aspectRatio(1f).clickable(enabled = !busy, role = Role.Button) { onPreview(sticker) }.padding(8.dp)) {
                                        stickerImage(sticker, Modifier.fillMaxSize())
                                    }
                                    StudioLabel(sticker.name, 12, Modifier.padding(horizontal = 8.dp), lineHeight = 18, maxLines = 1)
                                    TextButton(enabled = !busy, onClick = { onRemove(sticker) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                        StudioLabel("Remove", 12, color = DetailGreen, lineHeight = 18)
                                    }
                                }
                            }
                        }

                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
