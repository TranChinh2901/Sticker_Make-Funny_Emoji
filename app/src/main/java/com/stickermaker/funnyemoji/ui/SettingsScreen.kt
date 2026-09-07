package com.stickermaker.funnyemoji.ui

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.BuildConfig
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.StudioRepository
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.*
import java.util.UUID

@Composable
internal fun SettingsScreen(onNavigate: (Int) -> Unit, onPremium: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("startup", 0) }
    val scope = rememberCoroutineScope()
    var language by remember { mutableStateOf(preferences.getString("language", "en") ?: "en") }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var draftLanguage by remember { mutableStateOf(language) }
    var feedback by rememberSaveable { mutableStateOf("") }
    var feedbackId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        try {
            StudioRepository.settings()?.let { remote ->
                language = remote.language
                withContext(Dispatchers.IO) { preferences.edit().putString("language", language).commit() }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Keep the device preference while offline. */ }
    }
    Scaffold(containerColor = Color(0xFFF8FAFC), modifier = Modifier.navigationBarsPadding(),
        topBar = { Text("Settings", Modifier.fillMaxWidth().padding(start = 24.dp, top = 48.dp, bottom = 16.dp), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp) },
        bottomBar = { MainNavigation(3, onNavigate) }, snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).background(Color(0xFF34D399), CircleShape), contentAlignment = Alignment.Center) { Asset(R.drawable.settings_container, 18.754.dp, 19.883.dp) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Go Premium", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Unlock premium templates, remove ads, and get more sticker slots", color = Color(0xFFECFDF5), fontSize = 12.sp, lineHeight = 16.5.sp)
                    }
                    Button(onClick = onPremium, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF059669)), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text("Get Premium", fontSize = 12.sp) }
                }
            }
            Text("General", Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 2.dp) {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    SettingsRow("Language Setting", R.drawable.settings_group2, 17.917f, 17.917f, if (language == "vi") "Tiếng Việt" else "English") { draftLanguage = language; error = null; dialog = "Language Setting" }
                    SettingsRow("Feedback", R.drawable.settings_group3, 18.333f, 16.25f) { error = null; dialog = "Feedback" }
                    SettingsRow("Rating", R.drawable.settings_icon_park_outline_star, 20f, 20f) { error = null; dialog = "Rating" }
                    SettingsRow("Share App", R.drawable.settings_group4, 15f, 16.667f) {
                        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                            "I'm creating my own stickers with Sticker Maker & Funny Emoji!")
                        try { context.startActivity(Intent.createChooser(intent, "Share App")) }
                        catch (_: android.content.ActivityNotFoundException) { scope.launch { snackbar.showSnackbar("No sharing app available.") } }
                    }
                    SettingsRow("Privacy Policy", R.drawable.settings_group5, 17.083f, 17.083f) { dialog = "Privacy Policy" }
                    SettingsRow("DMCA", R.drawable.settings_container1, 20f, 18f) { error = null; dialog = "DMCA" }
                }
            }
            Text("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", Modifier.fillMaxWidth().padding(vertical = 16.dp), color = Color(0xFF94A3B8), fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
    dialog?.let { title ->
        val form = title in listOf("Feedback", "DMCA", "Rating")
        AlertDialog(onDismissRequest = { if (!busy) dialog = null }, title = { Text(title) }, text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (title) {
                    "Language Setting" -> {
                        Text("Choose the language for the welcome screens. The editor currently uses English.")
                        listOf("en" to "English", "vi" to "Tiếng Việt").forEach { (code, label) ->
                            Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { draftLanguage = code }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(draftLanguage == code, { draftLanguage = code }, enabled = !busy); Text(label)
                            }
                        }
                    }
                    "Privacy Policy" -> Text("Your photos stay on your device while editing. When you save, the finished PNG and its name are uploaded to your private studio. Collections and language preferences are stored with your guest account. Feedback is sent only when you press Submit. Sharing opens Android's share sheet so you can choose a destination.\n\nThis demo uses a guest account: clearing app data or uninstalling can remove access to that account. A published privacy policy and account recovery are not yet available.")
                    else -> {
                        if (title == "Rating") {
                            Text("Rate your experience with this demo.")
                            Row { (1..5).forEach { value ->
                                IconButton(onClick = { rating = value }, enabled = !busy, modifier = Modifier.size(44.dp)) {
                                    Asset(R.drawable.settings_icon_park_outline_star, 24.dp, description = "$value stars", tint = if (value <= rating) StickerGreen else Color.LightGray)
                                }
                            } }
                        }
                        if (title == "DMCA") Text("Describe the content, your ownership claim and a contact address so the team can review it.")
                        OutlinedTextField(feedback, { feedback = it.take(2000); feedbackId = UUID.randomUUID().toString() }, enabled = !busy,
                            modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text(if (title == "DMCA") "Copyright report" else "Your message") },
                            supportingText = { Text("${feedback.trim().length}/2000 · minimum 10 characters") })
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = {
            TextButton(enabled = !busy && (!form || feedback.trim().length >= 10), onClick = {
                if (title == "Privacy Policy") { dialog = null }
                else {
                    busy = true; error = null
                    scope.launch {
                        try {
                            if (title == "Language Setting") {
                                StudioRepository.language(draftLanguage)
                                check(withContext(Dispatchers.IO) { preferences.edit().putString("language", draftLanguage).commit() })
                                language = draftLanguage
                            } else {
                                val prefix = if (title == "Rating") "Rating $rating/5: " else "$title: "
                                StudioRepository.feedback(feedbackId, (prefix + feedback.trim()).take(2000))
                                feedback = ""; feedbackId = UUID.randomUUID().toString()
                            }
                            dialog = null
                            snackbar.showSnackbar(if (title == "Language Setting") "Language saved" else "Submitted. Thank you!")
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Không thể lưu. Kiểm tra kết nối rồi thử lại." }
                        finally { busy = false }
                    }
                }
            }) { Text(if (title == "Privacy Policy") "Close" else if (form) "Submit" else "Save") }
        }, dismissButton = { if (title != "Privacy Policy") TextButton(enabled = !busy, onClick = { dialog = null }) { Text("Cancel") } })
    }
}

@Composable
private fun SettingsRow(label: String, icon: Int, leafWidth: Float, leafHeight: Float, value: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { Asset(icon, leafWidth.dp, leafHeight.dp) }
        Text(label, Modifier.weight(1f).padding(start = 12.dp), fontSize = 16.sp)
        if (value != null) Text(value, fontSize = 14.sp, color = Color(0xFF969696)) else Asset(R.drawable.settings_arrows_chevron_chevron_right, 16.dp)
    }
    HorizontalDivider(color = Color(0xFFEEEEEE))
}
