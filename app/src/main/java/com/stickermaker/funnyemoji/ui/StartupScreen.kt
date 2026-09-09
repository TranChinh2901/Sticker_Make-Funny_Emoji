package com.stickermaker.funnyemoji.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.StudioRepository
import kotlinx.coroutines.CancellationException
import com.stickermaker.funnyemoji.ui.theme.StickerGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


@Composable
internal fun StartupScreen(onReady: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("startup", 0) }
    var stage by rememberSaveable { mutableIntStateOf(-1) }
    var language by rememberSaveable { mutableStateOf(preferences.getString("language", "fr") ?: "fr") }
    var cloudError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val ready by rememberUpdatedState(onReady)
    LaunchedEffect(Unit) {
        if (stage == -1) {
            delay(1200)
            stage = 0
        }
    }
    LaunchedEffect(stage, retry) {
        if (stage == 4) {
            val stored = withContext(Dispatchers.IO) {
                preferences.edit().putString("language", language).commit()
            }
            if (stored) {
                try {
                    StudioRepository.language(language)
                    ready()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    cloudError = true
                }
            } else stage = 3
        }
    }
    if (cloudError) AlertDialog(
        onDismissRequest = { cloudError = false; stage = 3 },
        title = { Text("Chưa lưu được lên server") },
        text = { Text("Ngôn ngữ đã lưu trên máy nhưng Supabase chưa nhận được. Kiểm tra mạng, Anonymous Sign-Ins và bảng user_settings.") },
        confirmButton = { TextButton(onClick = { cloudError = false; retry++ }) { Text("Thử lại") } },
        dismissButton = { TextButton(onClick = { cloudError = false; ready() }) { Text("Tiếp tục offline") } },
    )
    BackHandler(stage in 0..4) { if (stage in 1..3) stage-- }
    if (stage == -1) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val scale = maxOf(maxWidth / 390.dp, maxHeight / 844.dp)
            val artLeft = (maxWidth - 390.dp * scale) / 2
            val artTop = (maxHeight - 844.dp * scale) / 2
            Image(painterResource(R.drawable.splash_art), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            LinearProgressIndicator(
                modifier = Modifier.offset(x = artLeft + 20.dp * scale, y = artTop + 787.dp * scale)
                    .size(width = 350.dp * scale, height = 10.dp * scale),
                color = StickerGreen,
            )
        }
    } else {
        val vi = language == "vi"
        Box(Modifier.fillMaxSize()) {
            if (stage == 0) {
                LanguageScreen(language, onSelect = { language = it }, onConfirm = { stage = 1 })
            } else {
                val index = (stage - 1).coerceIn(0, 2)
                val artwork = listOf(R.drawable.onboarding_0, R.drawable.onboarding_1, R.drawable.onboarding_2)
                Image(painterResource(artwork[index]), "Onboarding ${index + 1}",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            if (stage > 0) Button(
                onClick = { stage++ },
                enabled = stage < 4,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C08E)),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 12.dp).fillMaxWidth().height(56.dp),
            ) {
                Text(if (stage >= 3) { if (vi) "Bắt đầu" else "Get Started" } else { if (vi) "Tiếp tục" else "Open" })
            }
        }
    }
}
