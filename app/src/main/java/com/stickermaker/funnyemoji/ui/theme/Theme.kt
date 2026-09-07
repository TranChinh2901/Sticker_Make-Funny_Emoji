package com.stickermaker.funnyemoji.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColors = lightColorScheme(
    primary = Color(0xFFFF5C5C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFF725573),
    background = Color(0xFFFFF8F6),
    surface = Color(0xFFFFF8F6),
)

@Composable
fun StickerMakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content = content,
    )
}
