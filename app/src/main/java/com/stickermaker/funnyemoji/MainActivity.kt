package com.stickermaker.funnyemoji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stickermaker.funnyemoji.ui.StickerApp
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StickerMakerTheme { StickerApp() } }
    }
}
