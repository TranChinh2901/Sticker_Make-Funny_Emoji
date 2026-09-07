package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.ui.theme.*

/** Measured against Premium.png (390 × 844). Prices are reference artwork, not Billing offers. */
@Composable
internal fun PremiumScreen(onClose: () -> Unit) {
    var monthly by rememberSaveable { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().navigationBarsPadding()) {
        Image(painterResource(R.drawable.premium_background), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        // Preserve the reference width-based scale; short displays scroll instead of squeezing plans.
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(844.dp)) {
                Image(painterResource(R.drawable.premium_illustration), null,
                    Modifier.offset(y = 84.dp).fillMaxWidth().height(196.dp), contentScale = ContentScale.FillBounds)
                IconButton(onClick = onClose, modifier = Modifier.offset(x = 6.dp, y = 44.dp).size(48.dp)
                    .semantics { contentDescription = "Close Premium" }) {
                    Text("×", fontSize = 32.sp, fontWeight = FontWeight.Medium, color = Color(0xFF4B5563))
                }
                Text("Upgrade Your Premium", Modifier.offset(y = 278.dp).fillMaxWidth().height(42.dp),
                    textAlign = TextAlign.Center, fontFamily = Baloo, fontSize = 28.5.sp,
                    lineHeight = 39.2.sp, fontWeight = FontWeight.Bold, color = StickerGreen)
                val benefits = listOf(
                    R.drawable.premium_benefit_0 to "Access Every Cute Screen Pet",
                    R.drawable.premium_benefit_1 to "Use Mixed Pet Mode Freely",
                    R.drawable.premium_benefit_2 to "Customize Animations Your Way",
                    R.drawable.premium_benefit_3 to "Remove Ads for Smooth Play",
                )
                benefits.forEachIndexed { index, (icon, text) ->
                    Row(Modifier.offset(x = 60.dp, y = (325 + index * 38).dp).width(280.dp).height(30.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Asset(icon, 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(text, fontFamily = Baloo, fontSize = 17.sp, lineHeight = 23.8.sp, color = StickerInk)
                    }
                }
                PremiumPlan(false, !monthly, Modifier.offset(x = 20.dp, y = 498.dp).width(350.dp).height(68.dp)) { monthly = false }
                Box(Modifier.offset(x = 240.dp, y = 484.dp).size(83.dp, 24.dp)
                    .background(StickerGreen, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                    Text("Best offer", Modifier.offset(y = 2.dp), fontFamily = Baloo, fontSize = 12.sp, color = Color.White)
                }
                PremiumPlan(true, monthly, Modifier.offset(x = 20.dp, y = 582.dp).width(350.dp).height(68.dp)) { monthly = true }
                Text("◷ Auto Renewable, Cancel Anytime", Modifier.offset(y = 680.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center, fontFamily = Baloo, fontSize = 12.sp, lineHeight = 20.sp, color = Color(0xFF4B5563))
                Button(onClick = { message = "Giá là mẫu từ thiết kế. Google Play Billing chưa được cấu hình; chưa phát sinh giao dịch hoặc cấp Premium." },
                    modifier = Modifier.offset(x = 20.dp, y = 718.dp).width(350.dp).height(48.dp),
                    shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)) {
                    Text("Upgrade Now", fontFamily = Baloo, fontSize = 18.5.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.offset(y = 766.dp).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    listOf("Terms", "Privacy Policy").forEach { label ->
                        TextButton(onClick = { message = "Chưa có đường dẫn $label được cấu hình." },
                            contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.height(44.dp)) {
                            Text(label, fontFamily = Baloo, fontSize = 10.sp, color = Color(0xFF4B5563))
                        }
                    }
                }
            }
        }
    }
    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Chưa khả dụng") }, text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("Đóng") } })
    }
}

@Composable
private fun PremiumPlan(monthly: Boolean, active: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(12.dp))
        .background(Brush.horizontalGradient(if (active) listOf(Color.White, Color(0xFFD6F5EE)) else listOf(Color.White, Color.White)))
        .border(if (active) 2.dp else 0.dp, if (active) StickerGreen else Color.Transparent, RoundedCornerShape(12.dp))
        .semantics { selected = active }.clickable(role = Role.RadioButton, onClick = onSelect)
        .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (monthly) "Monthly Access" else "Weekly", fontFamily = Baloo, fontSize = if (monthly) 20.sp else 18.sp,
                lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = StickerInk)
            if (monthly) Text("Only $24.99 per month", fontFamily = Baloo, fontSize = 14.sp, lineHeight = 20.sp, color = StickerInk)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(if (monthly) "$6.25" else "$2.99", fontFamily = Baloo, fontSize = 20.sp,
                lineHeight = 28.sp, fontWeight = FontWeight.Bold, color = StickerInk)
            if (monthly) Text("Per week", fontFamily = Baloo, fontSize = 12.sp, lineHeight = 18.sp, color = StickerInk)
        }
        Spacer(Modifier.width(16.dp))
    }
}
