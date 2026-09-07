package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*

/** Unlock Sticker.svg: a 405-unit sheet, not a full-screen destination. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnlockStickerSheet(onDismiss: () -> Unit, onPremium: () -> Unit) {
    var unavailable by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Column(Modifier.fillMaxWidth().heightIn(min = 405.dp).padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.size(60.dp, 6.dp).background(Color(0xFFADADAD), CircleShape))
                Spacer(Modifier.height(16.dp))
                Text("Unlock Sticker", fontFamily = Baloo, fontSize = 24.sp,
                    lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = StickerInk)
                Text("Watch a short video ad to unlock this sticker For Free.",
                    fontFamily = Baloo, fontSize = 12.sp, lineHeight = 17.sp,
                    textAlign = TextAlign.Center, color = Color(0xFF4B5563))
                Spacer(Modifier.height(16.dp))
                Asset(R.drawable.home_reference_sticker, 160.dp, description = "Sticker preview")
                Spacer(Modifier.height(24.dp))
                Button(onClick = {
                    unavailable = "Quảng cáo thưởng chưa được cấu hình. Sticker chưa được mở khóa."
                }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = CircleShape) {
                    Text("Get It Free", fontFamily = Baloo, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onPremium, modifier = Modifier.fillMaxWidth().height(48.dp), shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StickerGreen)) {
                    Asset(R.drawable.home_material_symbols_crown_rounded, 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Get Premium", fontFamily = Baloo, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
    unavailable?.let { message ->
        AlertDialog(onDismissRequest = { unavailable = null }, title = { Text("Chưa khả dụng") },
            text = { Text(message) }, confirmButton = {
                TextButton(onClick = { unavailable = null }) { Text("Đóng") }
            })
    }
}
