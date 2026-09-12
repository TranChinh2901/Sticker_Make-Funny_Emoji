package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*

private val PreviewFont = FontFamily(
    Font(R.font.be_vietnam_pro_regular),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

internal val PreviewGreen = Color(0xFF00C48C)
internal val hugAssets = listOf(R.drawable.preview_hug_1, R.drawable.preview_hug_2, R.drawable.preview_hug_3)

/** Figma 353:3755, 390 x 843. The app supplies the artboard density and real sticker image. */
@Composable
internal fun StickerPreviewLayout(
    onBack: () -> Unit, onShare: () -> Unit, onCollection: () -> Unit, onViewMore: () -> Unit,
    enabled: Boolean = true, options: @Composable () -> Unit,
    preview: @Composable (Modifier) -> Unit, related: @Composable (Int) -> Unit,
    status: @Composable () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(StickerBackground).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(90.dp).padding(start = 20.dp, end = 20.dp, top = 52.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = onBack, enabled = enabled, modifier = Modifier.requiredSize(48.dp)) {
                    Asset(R.drawable.preview_back, 30.dp, description = "Back")
                }
            }
            Spacer(Modifier.width(20.dp))
            Text("Preview sticker", Modifier.weight(1f), fontFamily = Baloo, fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937), maxLines = 1,
                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { options() }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(314.103.dp).clipToBounds().padding(horizontal = 17.949.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.matchParentSize().previewShadow())
                preview(Modifier.fillMaxSize())
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PreviewAction("Add to Your Stickers", R.drawable.preview_whatsapp, 17.5f, 8, enabled, onShare)
                PreviewAction("Add to Collection", R.drawable.preview_chat, 24f, 4, enabled, onCollection, 8)
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                    PreviewText("Hug", 20, 28, Modifier.weight(1f), FontWeight.Bold)
                    Row(Modifier.width(108.dp).height(30.dp).border(1.dp, PreviewGreen, CircleShape).clip(CircleShape)
                        .clickable(enabled = enabled, role = Role.Button, onClick = onViewMore).padding(horizontal = 13.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PreviewText("View More", 14, 20, weight = FontWeight.Medium, color = PreviewGreen)
                        Asset(R.drawable.preview_chevron, 6.dp, 10.5.dp)
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) { repeat(3) { related(it) } }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) { status() }
        }
    }
}

@Composable
private fun PreviewAction(label: String, icon: Int, iconSize: Float, gap: Int, enabled: Boolean, onClick: () -> Unit, leading: Int = 0) {
    Row(Modifier.fillMaxWidth().height(56.dp).clip(CircleShape).background(PreviewGreen)
        .alpha(if (enabled) 1f else .5f).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(start = leading.dp), horizontalArrangement = Arrangement.spacedBy(gap.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        Asset(icon, iconSize.dp)
        PreviewText(label, 16, 24, weight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
internal fun HugCard(index: Int, favorite: Boolean, onFavorite: () -> Unit, onUse: () -> Unit,
    modifier: Modifier = Modifier.width(120.dp), enabled: Boolean = true) {
    Box(modifier.height(154.dp).shadow(1.dp, RoundedCornerShape(12.dp)).background(Color.White, RoundedCornerShape(12.dp))
        .border(1.dp, PreviewGreen, RoundedCornerShape(12.dp))) {
        Image(painterResource(hugAssets[index]), "Hug ${index + 1}",
            Modifier.align(Alignment.TopCenter).padding(top = 25.dp).size(80.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp)) {
            CatalogActionButton(locked = index != 1, onClick = onUse, enabled = enabled)
        }

        IconToggleButton(favorite, { onFavorite() }, enabled = enabled,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 7.dp, y = (-5).dp).size(48.dp)) {
            Asset(if (favorite) R.drawable.home_solar_heart_bold else R.drawable.preview_heart, 16.dp, 13.663.dp,
                description = if (favorite) "Remove hug-$index from favorites" else "Favorite hug-$index",
                tint = if (favorite) Color(0xFFFF7387) else null)
        }
    }
}

@Composable
internal fun PreviewText(value: String, size: Int, line: Int, modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal, color: Color = Color(0xFF111827)) {
    Text(value, modifier, fontFamily = PreviewFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight,
        color = color, maxLines = 1, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)))
}

/** CSS box-shadow from Figma, cached in software so the blur also works on Android 8–11. */
private fun Modifier.previewShadow() = drawWithCache {
    val padding = 18.dp.toPx()
    val bitmap = android.graphics.Bitmap.createBitmap(
        (size.width + padding * 2).toInt().coerceAtLeast(1),
        (size.height + padding * 2).toInt().coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(26, 0, 0, 0)
        // CSS blur is approximately twice its Gaussian standard deviation.
        maskFilter = android.graphics.BlurMaskFilter(4.487.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    android.graphics.Canvas(bitmap).drawRect(padding, padding + 3.59.dp.toPx(),
        padding + size.width, padding + size.height + 3.59.dp.toPx(), paint)
    val shadow = bitmap.asImageBitmap()
    onDrawBehind { drawImage(shadow, topLeft = Offset(-padding, -padding)) }
}
