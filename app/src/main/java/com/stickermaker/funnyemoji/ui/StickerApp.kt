package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.launch

/** Step 1: Home UI. The remaining destinations deliberately stay on Home. */
@Composable
fun StickerApp() {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val later: (String) -> Unit = { feature ->
        scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar("$feature sẽ được triển khai ở bước tiếp theo.") }
    }
    Scaffold(containerColor = StickerBackground,
        topBar = { Column(Modifier.statusBarsPadding()) { BrandHeader(onPremium = { later("Premium") }) } },
        bottomBar = { MainNavigation(0) { if (it != 0) later(listOf("Home", "Customize", "My Studio", "Settings")[it]) } },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        HomeContent(Modifier.padding(padding), onUnlock = { later("Unlock Sticker") }, onViewMore = { later(it) })
    }
}

@Composable
private fun HomeContent(modifier: Modifier = Modifier, onUnlock: () -> Unit, onViewMore: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val scroll = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val sections = listOf("Trendding", "Frame", "Animal")
    val visible = sections.filter { query.isBlank() || it.contains(query.trim(), true) || "NickNam".contains(query.trim(), true) }
    LazyColumn(modifier.fillMaxSize(), state = scroll, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            BasicTextField(query, { query = it }, singleLine = true,
                textStyle = TextStyle(fontFamily = Poppins, fontSize = 16.sp, color = Color(0xFF4B5563)),
                modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, StickerGreen, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp)).semantics { contentDescription = "Search categories, tags" },
                decorationBox = { field ->
                    Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search categories, tags,...", fontFamily = Poppins, fontSize = 16.sp, color = Color(0xFF4B5563), maxLines = 1)
                            field()
                        }
                        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) { Asset(R.drawable.home_icomoon_free_search, 19.286.dp) }
                    }
                })
        }
        if (query.isBlank()) item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ViralBanner { scope.launch { scroll.animateScrollToItem(2) } }
                Asset(R.drawable.home_frame2087328581, 36.dp, 6.dp)
            }
        }
        if (visible.isEmpty()) item { Text("No stickers found", Modifier.padding(vertical = 32.dp), fontFamily = Baloo, fontSize = 18.sp) }
        visible.forEach { title -> item(key = title) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Asset(if (title == "Frame") R.drawable.home_noto_framed_picture else R.drawable.home_noto_fire, 27.257.dp)
                        Text(title, fontFamily = Baloo, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.height(28.dp).clickable(role = Role.Button) { onViewMore(title) }, contentAlignment = Alignment.Center) {
                        Row(Modifier.border(1.dp, StickerGreen, CircleShape).height(24.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("View More", fontFamily = Baloo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Box(Modifier.scale(scaleX = -1f, scaleY = 1f)) { Asset(R.drawable.home_ic_round_arrow_back_ios, 16.dp) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(3) { index -> StickerCard("$title ${index + 1}", Modifier.weight(1f), onUnlock) }
                }
            }
        } }
    }
}

@Composable
private fun ViralBanner(onExplore: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
        Box(Modifier.fillMaxWidth().heightIn(min = 156.dp)) {
            Image(painterResource(R.drawable.home_section_viral_banner), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Column(Modifier.padding(20.dp)) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFD7387))) { append("Viral ") }
                    withStyle(SpanStyle(color = Color(0xFF1EB992))) { append("Sticker Packs") }
                }, fontFamily = Baloo, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 33.6.sp)
                Text("Cute, funny and trending stickers\nupdated regularly.", fontFamily = Baloo, fontSize = 12.sp,
                    color = Color(0xFF2D2D2D), lineHeight = 16.8.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.clip(CircleShape).background(Color(0xFF00B686)).clickable(role = Role.Button, onClick = onExplore)
                    .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Asset(R.drawable.home_griddy_icons_send_filled, 20.dp)
                    Text("Explore Now", fontFamily = Baloo, fontSize = 14.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StickerCard(key: String, modifier: Modifier, onUnlock: () -> Unit) {
    var favorite by rememberSaveable(key) { mutableStateOf(false) }
    Box(modifier.height(140.dp).border(1.dp, StickerGreen, RoundedCornerShape(16.dp)).background(Color.White, RoundedCornerShape(16.dp))) {
        Column(Modifier.fillMaxSize().padding(top = 9.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(80.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) { Asset(R.drawable.home_frame2087328646, 56.021.dp, 70.dp, "NickNam sticker") }
            }
            Text("NickNam", fontFamily = Baloo, fontSize = 12.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.width(80.dp).height(20.dp).clip(CircleShape).background(StickerGreen).clickable(role = Role.Button, onClick = onUnlock),
                horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Asset(R.drawable.home_tabler_lock_filled, 10.dp)
                Text("Unlock", fontFamily = Baloo, fontSize = 10.sp, lineHeight = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        IconToggleButton(favorite, { favorite = it }, Modifier.align(Alignment.TopEnd).size(40.dp)) {
            Box(Modifier.size(18.26.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                Asset(R.drawable.home_solar_heart_bold, 14.dp, description = if (favorite) "Remove $key from favorites" else "Favorite $key",
                    tint = if (favorite) Color(0xFFFD7387) else null)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun HomePreview() { StickerMakerTheme { StickerApp() } }
