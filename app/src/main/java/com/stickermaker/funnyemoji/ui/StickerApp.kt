package com.stickermaker.funnyemoji.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.launch

/** Home: Mixed Mode.svg; Search: Home.svg. Supabase integration is a separate step. */
@Composable
fun StickerApp() {
    var startupComplete by rememberSaveable { mutableStateOf(false) }
    if (!startupComplete) {
        StartupScreen(onReady = { startupComplete = true })
        return
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var collectionRefresh by rememberSaveable { mutableIntStateOf(0) }
    var collectionOpen by rememberSaveable { mutableStateOf(false) }
    var premiumOpen by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var unlockOpen by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<com.stickermaker.funnyemoji.data.StickerCollection?>(null) }
    var preview by remember { mutableStateOf<com.stickermaker.funnyemoji.data.SavedSticker?>(null) }
    val screenState = rememberSaveableStateHolder()
    val navigate: (Int) -> Unit = { tab = it; detail = null; preview = null; category = null; searchOpen = false }
    BackHandler(enabled = premiumOpen || searchOpen || category != null || tab != 0 || detail != null || preview != null) {
        when {
            premiumOpen -> premiumOpen = false
            preview != null -> preview = null
            searchOpen -> searchOpen = false
            category != null -> category = null
            detail != null -> detail = null
            else -> tab = 0
        }
    }
    val deviceDensity = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artboardScale = (maxWidth.value / 390f).coerceIn(0.8f, 1.3f)
        CompositionLocalProvider(LocalDensity provides Density(deviceDensity.density * artboardScale, deviceDensity.fontScale)) {
            val key = when {
                premiumOpen -> "premium"
                preview != null -> "preview-${preview!!.id}"
                tab == 1 -> "editor"
                detail != null -> "collection-${detail!!.id}"
                searchOpen -> "search"
                category != null -> "category-$category"
                else -> "tab-$tab"
            }
            screenState.SaveableStateProvider(key) {
                when {
                    premiumOpen -> PremiumScreen(onClose = { premiumOpen = false })
                    preview != null -> SavedStickerScreen(preview!!, onBack = { preview = null }, onNewCollection = { collectionOpen = true })
                    tab == 1 -> EditorScreen(onBack = { tab = if (detail != null) 2 else 0 }, onSaved = {
                        preview = it; tab = 2; collectionRefresh++
                    })
                    detail != null -> CollectionDetailScreen(detail!!, onBack = { detail = null },
                        onCreate = { tab = 1 }, onChanged = { collectionRefresh++ }, refresh = collectionRefresh,
                        onPreview = { preview = it })
                    searchOpen -> SearchScaffold(onBack = { searchOpen = false }, onUnlock = { unlockOpen = true }, onPremium = { premiumOpen = true })
                    category != null -> CategoryScreen(title = category!!, onBack = { category = null },
                        onPremium = { premiumOpen = true }, onSearch = { searchOpen = true }, onUnlock = { unlockOpen = true })
                    tab == 2 -> MyStudioScreen(onCreate = { collectionOpen = true }, refresh = collectionRefresh,
                        onNavigate = navigate, onPremium = { premiumOpen = true }, onCollection = { detail = it }, onSticker = { preview = it })
                    tab == 3 -> SettingsScreen(onNavigate = navigate, onPremium = { premiumOpen = true })
                    else -> MixedModeHome(onSearch = { searchOpen = true }, onUnlock = { unlockOpen = true },
                        onViewMore = { category = if (it == "Trendding") "Trending" else it },
                        onPremium = { premiumOpen = true }, onNavigate = navigate)
                }
            }
            if (collectionOpen) NewCollectionSheet(onDismiss = { collectionOpen = false }, onCreated = { collectionRefresh++ })
            if (unlockOpen) UnlockStickerSheet(onDismiss = { unlockOpen = false }, onPremium = { unlockOpen = false; premiumOpen = true })
        }
    }
}

@Composable
private fun SearchScaffold(onBack: () -> Unit, onUnlock: () -> Unit, onPremium: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val later: (String) -> Unit = { feature ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("$feature sẽ được triển khai ở bước tiếp theo.")
        }
    }
    Scaffold(
        modifier = Modifier.background(StickerBackground).navigationBarsPadding(),
        containerColor = StickerBackground,
        topBar = {
            Row(
                Modifier.fillMaxWidth().height(100.dp).padding(start = 15.dp, end = 20.dp, top = 44.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp, 48.dp)) {
                    Asset(R.drawable.home_ic_round_arrow_back_ios, 22.dp, description = "Back", tint = Color(0xFF333333))
                }
                Text("Search", Modifier.weight(1f).padding(start = 9.dp), fontFamily = Baloo,
                    fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF101828))
                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                    IconButton(onClick = onPremium, modifier = Modifier.requiredSize(48.dp)) {
                        Asset(R.drawable.home_material_symbols_crown_rounded, 28.dp, description = "Get Premium")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        HomeContent(Modifier.padding(padding), onUnlock = onUnlock)
    }
}

@Composable
private fun HomeContent(modifier: Modifier = Modifier, onUnlock: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var favoriteIds by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    // These are explicit design fixtures, not records fetched from Supabase.
    val hasResults = query.isBlank() || "NickNam".contains(query.trim(), ignoreCase = true)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "search") {
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = TextStyle(fontFamily = Poppins, fontSize = 16.sp, color = Color(0xFF4B5563)),
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(48.dp)
                    .border(1.dp, StickerGreen, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .semantics { contentDescription = "Search categories, tags" },
                decorationBox = { field ->
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search, categories, tags,...", fontFamily = Poppins,
                                fontSize = 16.sp, color = Color(0xFF4B5563), maxLines = 1)
                            field()
                        }
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                            Asset(R.drawable.home_icomoon_free_search, 19.286.dp)
                        }
                    }
                },
            )
        }
        item(key = "most-searched") {
            Column(Modifier.padding(start = 15.dp, end = 15.dp, top = 24.dp)) {
                Row(Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Asset(R.drawable.home_reference_search_fire, 27.257.dp)
                    Text("Most Searched", fontFamily = Baloo, fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
                }
                Spacer(Modifier.height(14.dp))
                SearchTagRow(listOf("Trending" to 74, "Sanrio" to 62, "Snoopy" to 68, "Hellokitty" to 78)) { query = it }
                Spacer(Modifier.height(8.dp))
                SearchTagRow(listOf("Cat" to 47, "Sanrio" to 62, "Memw" to 64, "Rare" to 52, "Dog" to 50)) { query = it }
            }
        }
        item(key = "recommend") {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp).height(28.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Asset(R.drawable.home_reference_recommend, 20.dp)
                Text("Recommend", fontFamily = Baloo, fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            }
        }
        if (!hasResults) {
            item { Text("No stickers found", Modifier.padding(20.dp), fontFamily = Baloo, fontSize = 18.sp) }
        } else {
            items(4, key = { "recommend-row-$it" }) { row ->
                Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { column ->
                        val id = "recommend-${row * 3 + column}"
                        StickerCard(id, Modifier.weight(1f), id in favoriteIds, { selected ->
                            favoriteIds = ArrayList(favoriteIds).apply {
                                if (selected) { if (id !in this) add(id) } else remove(id)
                            }
                        }, onUnlock)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTagRow(tags: List<Pair<String, Int>>, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { (label, width) ->
            Box(Modifier.width(width.dp).height(28.dp).clip(CircleShape).background(Color(0xFFD5F2ED))
                .clickable(role = Role.Button) { onSelect(label) }, contentAlignment = Alignment.Center) {
                Text(label, fontFamily = Inter, fontSize = 12.sp, color = Color(0xFF2C3B48), maxLines = 1)
            }
        }
    }
}

@Composable
internal fun StickerCard(
    key: String, modifier: Modifier, favorite: Boolean,
    onFavoriteChange: (Boolean) -> Unit, onUnlock: () -> Unit,
) {
    Box(modifier.height(140.dp).border(1.dp, StickerGreen, RoundedCornerShape(16.dp))
        .background(Color.White, RoundedCornerShape(16.dp))) {
        // Independent slots prevent font metrics from squeezing the label in a fixed-height Column.
        Box(Modifier.align(Alignment.TopCenter).padding(top = 14.dp).size(70.dp), contentAlignment = Alignment.Center) {
            Asset(R.drawable.home_reference_sticker, 70.dp, description = "NickNam sticker")
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = 89.dp).fillMaxWidth().height(22.dp),
            contentAlignment = Alignment.Center) {
            Text("NickNam", modifier = Modifier.requiredHeight(18.dp).wrapContentHeight(Alignment.CenterVertically, unbounded = true),
                fontFamily = Baloo, fontSize = 12.sp, lineHeight = 16.sp,
                fontWeight = FontWeight.Medium, maxLines = 1,
                style = TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.None,
                    ),
                ))
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = 111.dp)) {
            Row(Modifier.width(80.dp).height(20.dp).clip(CircleShape).background(StickerGreen)
                .clickable(role = Role.Button, onClick = onUnlock),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically) {
                Asset(R.drawable.home_tabler_lock_filled, 10.dp)
                Text("Unlock", fontFamily = Baloo, fontSize = 10.sp, lineHeight = 10.sp,
                    color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        IconToggleButton(favorite, onFavoriteChange,
            Modifier.align(Alignment.TopEnd).offset(x = 1.2.dp, y = (-.9).dp).size(40.dp)) {
            Box(Modifier.size(18.26.dp).shadow(1.dp, CircleShape).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Asset(R.drawable.home_solar_heart_bold, 14.dp,
                    description = if (favorite) "Remove $key from favorites" else "Favorite $key",
                    tint = if (favorite) Color(0xFFFD7387) else null)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePreview() { StickerMakerTheme { StickerApp() } }
