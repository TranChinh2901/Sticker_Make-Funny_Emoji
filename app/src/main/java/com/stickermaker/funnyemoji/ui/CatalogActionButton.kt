package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.Baloo
import com.stickermaker.funnyemoji.ui.theme.StickerGreen

@Composable
internal fun CatalogActionButton(locked: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Row(Modifier.width(80.dp).height(20.dp).clip(CircleShape).background(StickerGreen)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically) {
        if (locked) Asset(R.drawable.home_tabler_lock_filled, 10.dp)
        Text(if (locked) "Unlock" else "Use", fontFamily = Baloo, fontSize = 10.sp, lineHeight = 10.sp,
            color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
