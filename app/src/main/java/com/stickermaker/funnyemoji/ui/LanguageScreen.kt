package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.Baloo
import com.stickermaker.funnyemoji.ui.theme.Inter

private data class LanguageOption(val code: String, val label: String, val flag: Int)
private val languageOptions = listOf(
    LanguageOption("fr", "Français", R.drawable.language_fr),
    LanguageOption("en", "English", R.drawable.language_en),
    LanguageOption("hi", "हिन्दी (India)", R.drawable.language_hi),
    LanguageOption("es", "Español", R.drawable.language_es),
    LanguageOption("zh", "Chinese", R.drawable.language_zh),
    LanguageOption("pt", "Português (Portugal)", R.drawable.language_pt),
    LanguageOption("ru", "Русский", R.drawable.language_ru),
    LanguageOption("vi", "Tiếng Việt", R.drawable.language_vi),
)

/** Figma 335:571 geometry; expose only the preferences accepted by the app and server. */
@Composable
internal fun LanguageScreen(language: String, onSelect: (String) -> Unit, onConfirm: () -> Unit,
    onBack: (() -> Unit)? = null, busy: Boolean = false, error: String? = null) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Startup is outside StickerApp's artboard provider; Settings is already scaled to 390.
        val scale = (maxWidth.value / 390f).coerceIn(.8f, 1.3f)
        CompositionLocalProvider(LocalDensity provides Density(density.density * scale, density.fontScale)) {
            LanguageLayout(language, onSelect, onConfirm, onBack, busy, error)
        }
    }
}

@Composable
private fun LanguageLayout(language: String, onSelect: (String) -> Unit, onConfirm: () -> Unit,
    onBack: (() -> Unit)?, busy: Boolean, error: String?) {
    val green = Color(0xFF12AD82)
    Column(Modifier.fillMaxSize().background(Color(0xFFF5FFFD)).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(98.dp).padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (onBack != null) IconButton(onClick = onBack, enabled = !busy, modifier = Modifier.requiredSize(48.dp)) {
                    Asset(R.drawable.preview_back, 24.dp, description = "Cancel language selection")
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Choose Language", Modifier.wrapContentSize(unbounded = true), fontFamily = Baloo, fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xFF101828), maxLines = 1,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
            }
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = green, strokeWidth = 2.dp)
                else IconButton(onClick = onConfirm, modifier = Modifier.requiredSize(48.dp)) {
                    Asset(R.drawable.language_confirm, 24.dp, description = "Confirm language")
                }
            }
        }
        error?.let { Text(it, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.error) }
        LazyColumn(Modifier.fillMaxWidth().weight(1f).selectableGroup(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(languageOptions, key = { it.code }) { option ->
                val active = language == option.code
                val shape = RoundedCornerShape(12.dp)
                Row(Modifier.fillMaxWidth().height(56.dp).shadow(1.dp, shape).background(Color.White, shape)
                    .then(if (active) Modifier.border(2.dp, green, shape) else Modifier)
                    .clip(shape).selectable(selected = active, enabled = !busy, role = Role.RadioButton, onClick = { onSelect(option.code) })
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape)) {
                        if (option.code == "zh") CroppedAsset(option.flag, 28.dp, 28.dp, 1.7021f, 1f, .0003f, 0f)
                        else Image(painterResource(option.flag), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(option.label, Modifier.weight(1f), fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp,
                        color = Color(0xFF101828), maxLines = 1,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
                    Canvas(Modifier.size(24.dp)) {
                        drawCircle(if (active) green else Color(0xFF737373), radius = 9.dp.toPx(), style = Stroke(2.dp.toPx()))
                        if (active) drawCircle(green, radius = 5.dp.toPx())
                    }
                }
            }
        }
    }
}
