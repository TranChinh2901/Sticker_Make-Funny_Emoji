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
import com.stickermaker.funnyemoji.ui.theme.StickerGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Intro artwork is exported; surrounding onboarding controls await full screen exports. */
@Composable
internal fun StartupScreen(onReady: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("startup", 0) }
    var stage by rememberSaveable { mutableIntStateOf(-1) }
    var language by rememberSaveable { mutableStateOf(preferences.getString("language", "fr") ?: "fr") }
    val ready by rememberUpdatedState(onReady)
    LaunchedEffect(Unit) {
        if (stage == -1) {
            delay(1200) // Branding transition, not network/ad progress.
            // Demo: replay the full introduction on each fresh app launch.
            stage = 0
        }
    }
    LaunchedEffect(stage) {
        if (stage == 4) {
            val stored = withContext(Dispatchers.IO) {
                preferences.edit().putString("language", language).commit()
            }
            if (stored) ready() else stage = 3
        }
    }
    BackHandler(stage in 0..4) { if (stage in 1..3) stage-- }
    if (stage == -1) {
        Box(Modifier.fillMaxSize()) {
            // Cover different device aspect ratios without stretching the artwork.
            Image(painterResource(R.drawable.splash_art), null,
                Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            LinearProgressIndicator(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp).fillMaxWidth().height(10.dp),
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
                // Draw controls above the art, not after a full-height image in a scrolling column.
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
                Text(if (stage >= 3) { if (vi) "Bắt đầu" else "Get Started" } else { if (vi) "Tiếp tục" else "Continue" })
            }
        }
    }
}
