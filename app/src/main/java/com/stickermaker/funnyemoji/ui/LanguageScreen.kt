package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.ui.theme.Baloo

@Composable
internal fun LanguageScreen(language: String, onSelect: (String) -> Unit, onConfirm: () -> Unit) {
    val green = Color(0xFF12AD82)
    val choices = listOf(
        Triple("fr", "🇫🇷", "Français"),
        Triple("en", "🇬🇧", "English"),
        Triple("vi", "🇻🇳", "Tiếng Việt"),
        Triple("hi", "🇮🇳", "हिन्दी (India)"),
        Triple("es", "🇪🇸", "Español"),
        Triple("zh", "🇨🇳", "Chinese"),
        Triple("pt", "🇵🇹", "Português (Portugal)"),
        Triple("ru", "🇷🇺", "Русский"),
    )
    Column(Modifier.fillMaxSize().background(Color.White).safeDrawingPadding()) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            Text("Choose Language", Modifier.align(Alignment.Center), fontFamily = Baloo,
                fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
            IconButton(onClick = onConfirm, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp)
                .semantics { contentDescription = "Confirm language" }) {
                Canvas(Modifier.size(24.dp)) {
                    val check = Path().apply {
                        moveTo(size.width * 0.17f, size.height * 0.50f)
                        lineTo(size.width * 0.42f, size.height * 0.75f)
                        lineTo(size.width * 0.83f, size.height * 0.25f)
                    }
                    drawPath(check, Color(0xFF00B882), style = Stroke(
                        width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round,
                    ))
                }
            }
        }
        LazyColumn(Modifier.fillMaxWidth().weight(1f), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(choices, key = { it.first }) { (code, flag, title) ->
                val active = language == code
                Row(Modifier.fillMaxWidth().height(56.dp)
                    .background(if (active) Color(0xFFE7F7F3) else Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, if (active) green else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                    .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(code) })
                    .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(flag, fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(title, Modifier.weight(1f), fontFamily = Baloo, fontSize = 16.sp, color = Color(0xFF111827))
                    RadioButton(selected = active, onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = green, unselectedColor = Color(0xFF9CA3AF)))
                }
            }
        }

    }
}
