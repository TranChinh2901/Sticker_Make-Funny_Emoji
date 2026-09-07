package com.stickermaker.funnyemoji.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*

@Composable
internal fun Asset(@DrawableRes id: Int, width: Dp, height: Dp = width, description: String? = null, tint: Color? = null) {
    Image(painterResource(id), description, Modifier.size(width, height), contentScale = ContentScale.Fit,
        colorFilter = tint?.let(ColorFilter::tint))
}

// Preserve the independent viewport and source crop supplied by Figma.
@Composable
internal fun CroppedAsset(@DrawableRes id: Int, width: Dp, height: Dp, scaleX: Float, scaleY: Float, left: Float, top: Float, description: String? = null) {
    Box(Modifier.size(width, height).clip(RoundedCornerShape(0.dp))) {
        Image(painterResource(id), description,
            Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                .offset(width * left, height * top).requiredSize(width * scaleX, height * scaleY),
            contentScale = ContentScale.FillBounds)
    }
}

@Composable
internal fun AssetButton(@DrawableRes id: Int, label: String, onClick: () -> Unit, size: Dp = 24.dp, tint: Color? = null) {
    IconButton(onClick = onClick) { Asset(id, size, description = label, tint = tint) }
}

@Composable
internal fun BrandHeader(title: String? = null, onPremium: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        if (title == null) CroppedAsset(R.drawable.home_chat_gpt_image18484423_thg720261_photoroom1,
            172.242.dp, 28.dp, 1.197f, 3.1323f, -.1133f, -1.0056f, "Sticker Maker")
        else Text(title, fontFamily = Baloo, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = StickerGreen)
        AssetButton(R.drawable.home_material_symbols_crown_rounded, "Get Premium", onPremium, 28.dp)
    }
}

@Composable
internal fun MainNavigation(selected: Int, onSelect: (Int) -> Unit) {
    val names = listOf("Home", "Customize", "My Studio", "Settings")
    val icons = listOf(R.drawable.home_solar_home_angle_bold,
        R.drawable.home_si_ai_edit_alt2_line, R.drawable.home_group, R.drawable.home_group1)
    Surface(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), shadowElevation = 10.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(76.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            names.forEachIndexed { index, name ->
                Column(Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab) { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        val leaf = if (index == 2) 21.5.dp else if (index == 3) 21.dp else 24.dp
                        Asset(icons[index], leaf)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(name, fontSize = 11.4.sp, fontFamily = Inter, maxLines = 1,
                        color = if (selected == index) Color(0xFF009A8A) else Color(0xFF878787),
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
internal fun GreenButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick, modifier.heightIn(min = 48.dp), shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)) {
        Text(label, fontFamily = Baloo, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

@Composable
internal fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(CircleShape).background(if (selected) StickerGreen else Color(0xFFF0FDFA))
        .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(label, fontFamily = Baloo, fontSize = 14.sp, color = if (selected) Color.White else Color(0xFF115E59))
    }
}
