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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.launch

@Composable
internal fun MixedModeHome(onSearch: () -> Unit, onUnlock: () -> Unit, onViewMore: (String) -> Unit, onPremium: () -> Unit, onNavigate: (Int) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scroll = rememberLazyListState()
    val later: (String) -> Unit = { feature ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar("$feature sẽ được triển khai ở bước tiếp theo.")
        }
    }
    Scaffold(
        modifier = Modifier.background(Color.White).navigationBarsPadding(),
        containerColor = StickerBackground,
        topBar = { BrandHeader(onPremium = onPremium) },
        bottomBar = {
            MainNavigation(0, onNavigate)
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(), state = scroll,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "search") {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color.White).border(1.dp, StickerGreen, RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClick = onSearch).padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Search categories, tags,...", Modifier.weight(1f), fontFamily = Poppins,
                        fontSize = 16.sp, color = Color(0xFF4B5563), maxLines = 1)
                    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
                        Asset(R.drawable.home_icomoon_free_search, 19.286.dp, description = "Search")
                    }
                }
            }
            item(key = "banner") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ViralBanner { scope.launch { scroll.animateScrollToItem(2) } }
                    Asset(R.drawable.home_frame2087328581, 36.dp, 6.dp)
                }
            }
            listOf("Trending", "Frame", "Animal").forEach { title ->
                item(key = title) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth().height(27.257.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Asset(if (title == "Frame") R.drawable.home_noto_framed_picture else R.drawable.home_noto_fire, 27.257.dp)
                                Text(title, fontFamily = Baloo, fontSize = 18.sp, lineHeight = 25.2.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Row(Modifier.width(97.dp).height(24.dp).clip(CircleShape).border(1.dp, StickerGreen, CircleShape)
                                .clickable(role = Role.Button) { onViewMore(title) }.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("View More", fontFamily = Baloo, fontSize = 14.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
                                Box(Modifier.scale(scaleX = -1f, scaleY = 1f)) { Asset(R.drawable.home_ic_round_arrow_back_ios, 16.dp) }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            com.stickermaker.funnyemoji.data.StickerCatalog.search(title).take(3).forEach { sticker ->
                                CatalogCard(sticker.id, Modifier.weight(1f), onUnlock)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViralBanner(onExplore: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), shadowElevation = 6.dp) {
        Box(Modifier.fillMaxWidth().height(156.dp)) {
            Image(painterResource(R.drawable.home_section_viral_banner), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Column(Modifier.padding(20.dp)) {
                val heading = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFFD7387))) { append("Viral ") }
                    withStyle(SpanStyle(color = Color(0xFF1EB992))) { append("Sticker Packs") }
                }
                Box(Modifier.height(33.6.dp)) {
                    Text("Viral Sticker Packs", color = Color.White, fontFamily = Baloo, fontSize = 24.sp,
                        fontWeight = FontWeight.Bold, lineHeight = 33.6.sp,
                        style = TextStyle(drawStyle = Stroke(width = with(LocalDensity.current) { 3.dp.toPx() })))
                    Text(heading, fontFamily = Baloo, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 33.6.sp)
                }
                Text("Cute, funny and trending stickers\nupdated regularly.", Modifier.height(33.6.dp), fontFamily = Baloo,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2D2D2D), lineHeight = 16.8.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.clip(CircleShape).background(Color(0xFF00B686)).clickable(role = Role.Button, onClick = onExplore)
                    .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Asset(R.drawable.home_griddy_icons_send_filled, 20.dp)
                    Text("Explore Now", fontFamily = Baloo, fontSize = 14.sp, lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
