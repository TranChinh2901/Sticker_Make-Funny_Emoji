package com.stickermaker.funnyemoji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.data.SupabaseProvider
import com.stickermaker.funnyemoji.ui.theme.StickerMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StickerMakerTheme {
                StickerHomeScreen(supabaseConfigured = SupabaseProvider.isConfigured)
            }
        }
    }
}

@Composable
private fun StickerHomeScreen(
    supabaseConfigured: Boolean,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "😜", fontSize = 84.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Sticker Maker",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Biến khoảnh khắc vui thành sticker của riêng bạn",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = if (supabaseConfigured) {
                                "✓ Supabase đã được cấu hình"
                            } else {
                                "⚙ Supabase chưa được cấu hình"
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (supabaseConfigured) {
                                "Auth, Database và Storage đã sẵn sàng để tích hợp."
                            } else {
                                "Điền URL và publishable key trong local.properties để kết nối server."
                            },
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Tạo sticker đầu tiên")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StickerHomePreview() {
    StickerMakerTheme {
        StickerHomeScreen(supabaseConfigured = false)
    }
}
