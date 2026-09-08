package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.launch

/** Category Shimeji List.svg. Sticker rows are local design fixtures until backend mapping exists. */
@Composable
internal fun CategoryScreen(title: String, onBack: () -> Unit, onSearch: () -> Unit, onUnlock: () -> Unit, onPremium: () -> Unit) {
    val tabs = listOf(title to 88, "Sanrio" to 66, "Snoopy" to 72, "Animal" to 68, "Anime" to 66)
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier.navigationBarsPadding(), containerColor = StickerBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Row(Modifier.fillMaxWidth().height(100.dp).padding(start = 15.dp, end = 10.dp, top = 44.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.width(40.dp)) {
                    Asset(R.drawable.home_ic_round_arrow_back_ios, 22.dp, description = "Back", tint = Color(0xFF333333))
                }
                Text(title, Modifier.weight(1f).padding(start = 9.dp), fontFamily = Baloo,
                    fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onSearch) {
                    Asset(R.drawable.home_icomoon_free_search, 20.dp, description = "Search")
                }
                IconButton(onClick = onPremium) { Asset(R.drawable.home_material_symbols_crown_rounded, 28.dp, description = "Get Premium") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs.size, key = { it }) { index ->
                    val (label, width) = tabs[index]
                    val active = index == selectedTab
                    Box(Modifier.width(width.dp).height(32.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (active) StickerGreen else Color.White)
                        .semantics { selected = active }
                        .clickable(role = Role.Tab) {
                            selectedTab = index
                            scope.launch { grid.scrollToItem(0) }
                        }, contentAlignment = Alignment.Center) {
                        Text(label, fontFamily = Baloo, fontSize = 14.sp, maxLines = 1,
                            fontWeight = FontWeight.Medium, color = if (active) Color.White else StickerInk)
                    }
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(3), state = grid, modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val results = com.stickermaker.funnyemoji.data.StickerCatalog.search(tabs[selectedTab].first)
                if (results.isEmpty()) item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Text("No stickers in this category yet", Modifier.padding(vertical = 32.dp), fontFamily = Baloo)
                }
                items(results, key = { it.id }) { sticker ->
                    CatalogCard(sticker.id, Modifier.fillMaxWidth(), onUnlock)
                }
            }
        }
    }
}
