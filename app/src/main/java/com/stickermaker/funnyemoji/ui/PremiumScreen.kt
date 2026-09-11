package com.stickermaker.funnyemoji.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R

private val PremiumGreen = Color(0xFF12AD82)
private val PremiumInk = Color(0xFF1F2937)

@Composable
internal fun PremiumScreen(onClose: () -> Unit) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    PremiumLayout(monthly, { monthly = it }, onClose,
        onUpgrade = { message = "Giá là mẫu từ thiết kế. Google Play Billing chưa được cấu hình; chưa phát sinh giao dịch hoặc cấp Premium." },
        onTerms = { message = "Chưa có đường dẫn Terms được cấu hình." },
        onPrivacy = { message = "Chưa có đường dẫn Privacy Policy được cấu hình." })
    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Chưa khả dụng") }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Đóng") } })
    }
}

/** Coordinates are from Figma 133:144, in the app's shared 390 dp artboard density. */
@Composable
internal fun PremiumLayout(
    monthly: Boolean, onPlanChange: (Boolean) -> Unit, onClose: () -> Unit,
    onUpgrade: () -> Unit, onTerms: () -> Unit, onPrivacy: () -> Unit,
) {
    Box(Modifier.fillMaxSize().premiumGradient(false).navigationBarsPadding()) {
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(844.dp).selectableGroup()) {
                Image(painterResource(R.drawable.premium_figma_hero), null,
                    Modifier.offset(49.dp, 51.dp).size(292.dp, 219.dp), contentScale = ContentScale.Fit)
                IconButton(onClick = onClose, Modifier.offset(6.dp, 44.dp).size(48.dp)) {
                    Asset(R.drawable.premium_figma_close, 20.dp, description = "Close Premium")
                }
                Label(R.drawable.premium_figma_title, "Upgrade Your Premium", 48f, 283f, 294f, 33f)
                val icons = listOf(R.drawable.premium_figma_feature_0, R.drawable.premium_figma_feature_1,
                    R.drawable.premium_figma_feature_2, R.drawable.premium_figma_feature_3)
                val texts = listOf("Access Every Cute Screen Pet", "Use Mixed Pet Mode Freely",
                    "Customize Animations Your Way", "Remove Ads for Smooth Play")
                val labels = listOf(R.drawable.premium_figma_feature_text_0, R.drawable.premium_figma_feature_text_1,
                    null, R.drawable.premium_figma_feature_text_3)
                val widths = listOf(218f, 196f, 236f, 210f)
                icons.forEachIndexed { i, icon ->
                    val y = 328f + i * 38.362934f
                    Label(icon, null, 60.21236f, y, 24f, 24f)
                    labels[i]?.let { Label(it, texts[i], 93.78765f, y, widths[i], 24f) }
                        ?: NativeLabel(texts[i], 93.78765f, y, 236f, 24f, 16, true)
                }
                PremiumPlan(false, !monthly, Modifier.offset(20.dp, 495.0888.dp).size(350.dp, 72.dp)) { onPlanChange(false) }
                PremiumPlan(true, monthly, Modifier.offset(20.dp, 583.0888.dp).size(350.dp, 72.dp)) { onPlanChange(true) }
                Label(R.drawable.premium_figma_ribbon, "Best Offer", 211f, 489.06146f, 141f, 18f)
                Label(R.drawable.premium_figma_shield, null, 102f, 683.5888f, 16f, 16f)
                NativeLabel("Auto Renewable, Cancel Anytime", 122f, 683.0888f, 186f, 17f, 12, false, Color(0xFF4B5563))
                Box(Modifier.offset(20.dp, 712.0888.dp).size(350.dp, 56.dp)
                    .premiumShadow(false).clip(RoundedCornerShape(16.dp)).background(PremiumGreen)
                    .clickable(role = Role.Button, onClick = onUpgrade), contentAlignment = Alignment.Center) {
                    Text("Upgrade Now", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        fontFamily = FontFamily.SansSerif, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
                }
                Box(Modifier.offset(135.dp, 766.dp).size(41.dp, 44.dp).clickable(role = Role.Button, onClick = onTerms)) {
                    Label(R.drawable.premium_figma_terms, "Terms", 10.5f, 14.0888f, 27f, 14f)
                }
                Box(Modifier.offset(180.dp, 766.dp).size(68.dp, 44.dp).clickable(role = Role.Button, onClick = onPrivacy)) {
                    Label(R.drawable.premium_figma_privacy, "Privacy Policy", .5f, 14.0888f, 60f, 14f)
                }
                listOf(176.5f, 244.5f).forEach { x ->
                    Box(Modifier.offset(x.dp, 781.5888.dp).size(.5.dp, 11.dp).background(Color(0xFF4B5563)))
                }
            }
        }
    }
}

@Composable
private fun PremiumPlan(monthly: Boolean, active: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val ink = if (active) PremiumInk else Color(0xFF4B5563)
    Box(modifier.then(if (active) Modifier.premiumShadow(true) else Modifier)
        .clip(shape).then(if (active) Modifier.premiumGradient(true) else Modifier.background(Color.White))
        .then(if (active) Modifier.border(2.dp, PremiumGreen, shape) else Modifier)
        .selectable(active, role = Role.RadioButton, onClick = onSelect)
        .semantics(mergeDescendants = true) {}) {
        if (monthly) {
            if (active) Label(R.drawable.premium_figma_monthly_selected, "Monthly Access", 16f, 9.5f, 132f, 25f, ink)
            else NativeLabel("Monthly Access", 16f, 9.5f, 132f, 25f, 18, false, ink)
            Label(R.drawable.premium_figma_monthly_subtitle, "Only $24.99 per month", 16f, 42.5f, 142f, 20f, ink)
            Label(R.drawable.premium_figma_monthly_price, "$6.25", 272f, 16.5f, 42f, 22f, ink)
            Label(R.drawable.premium_figma_per_week, "Per week", 272f, 38.5f, 46f, 17f, ink)
        } else {
            Label(if (active) R.drawable.premium_figma_weekly_selected else R.drawable.premium_figma_weekly,
                "Weekly", 16f, 23.5f, if (active) 61f else 56f, 25f, ink)
            Label(R.drawable.premium_figma_weekly_price, "$2.99", 272f, 25f, 46f, 22f, ink)
        }
    }
}

/** Original outlined Figma labels retain SF Pro Rounded shapes and accessible descriptions. */
@Composable
private fun Label(@DrawableRes id: Int, text: String?, x: Float, y: Float, width: Float, height: Float, tint: Color? = null) {
    Image(painterResource(id), text, Modifier.offset(x.dp, y.dp).size(width.dp, height.dp),
        contentScale = ContentScale.FillBounds, colorFilter = tint?.let { ColorFilter.tint(it) })
}

// Four labels were unavailable in the Figma export; keep real text until their original assets are available.
@Composable
private fun NativeLabel(text: String, x: Float, y: Float, width: Float, height: Float, size: Int, bold: Boolean, color: Color = PremiumInk) {
    Box(Modifier.offset(x.dp, y.dp).size(width.dp, height.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, fontSize = size.sp, lineHeight = height.sp, fontFamily = FontFamily.SansSerif,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color, maxLines = 1,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
    }
}

private fun Modifier.premiumShadow(glow: Boolean) = drawWithCache {
    val padding = 20.dp.toPx()
    val bitmap = android.graphics.Bitmap.createBitmap((size.width + padding * 2).toInt(),
        (size.height + padding * 2).toInt(), android.graphics.Bitmap.Config.ARGB_8888)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = if (glow) android.graphics.Color.argb(102, 25, 216, 163) else android.graphics.Color.argb(38, 0, 0, 0)
        maskFilter = android.graphics.BlurMaskFilter((if (glow) 6 else 1).dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    val dy = if (glow) 0f else 4.dp.toPx()
    val radius = (if (glow) 12 else 16).dp.toPx()
    android.graphics.Canvas(bitmap).drawRoundRect(padding, padding + dy, padding + size.width,
        padding + size.height + dy, radius, radius, paint)
    val shadow = bitmap.asImageBitmap()
    onDrawBehind { drawImage(shadow, topLeft = Offset(-padding, -padding)) }
}

/** CSS gradient endpoints depend on both the element width and height. */
private fun Modifier.premiumGradient(plan: Boolean) = drawWithCache {
    val direction = if (plan) Offset(.9128f, .4084f) else Offset(.99892f, .04639f)
    val extent = size.width * direction.x + size.height * direction.y
    val center = Offset(size.width / 2, size.height / 2)
    val half = direction * (extent / 2)
    val brush = if (plan) Brush.linearGradient(listOf(Color.White, Color(0xFFD5F5F0)), center - half, center + half)
        else Brush.linearGradient(0f to Color(0xFFEAF7E2), .53954f to Color(0xFFE7F6E6),
            .98442f to Color(0xFFD2F2ED), start = center - half, end = center + half)
    onDrawBehind { drawRect(brush) }
}
