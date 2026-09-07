package com.stickermaker.funnyemoji.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R

val StickerGreen = Color(0xFF12AD82)
val StickerBackground = Color(0xFFF8FBFF)
val StickerInk = Color(0xFF1F2937)
@OptIn(ExperimentalTextApi::class)
val Baloo = FontFamily(
    Font(R.font.baloo2, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.baloo2, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.baloo2, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
)
val Poppins = FontFamily(Font(R.font.poppins_regular))
val Inter = FontFamily(Font(R.font.inter))

private val AppColors = lightColorScheme(
    primary = StickerGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7F0),
    secondary = Color(0xFF009A8A),
    background = StickerBackground,
    surface = Color.White,
    onSurface = StickerInk,
    onBackground = StickerInk,
)

@Composable
fun StickerMakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = Typography(bodyLarge = TextStyle(fontFamily = Baloo, fontSize = 16.sp,
            letterSpacing = 0.sp, platformStyle = PlatformTextStyle(includeFontPadding = false))),
        content = content,
    )
}
