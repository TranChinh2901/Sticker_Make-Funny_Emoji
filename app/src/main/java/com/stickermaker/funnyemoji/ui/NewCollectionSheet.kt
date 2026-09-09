package com.stickermaker.funnyemoji.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.CollectionRepository
import com.stickermaker.funnyemoji.data.loadCollectionThumbnail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val CollectionGreen = Color(0xFF00B686)
private val CollectionInk = Color(0xFF374151)
private val CollectionPalette = listOf("#FFFFFF", "#00B686", "#FBCFE8", "#CCFBF1", "#FEF9C3", "#E9D5FF")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewCollectionSheet(
    onDismiss: () -> Unit,
    onCreated: () -> Unit = {},
    createCollection: suspend (String, String, ByteArray?) -> Unit = { name, color, image ->
        CollectionRepository.create(name, color, image)
    },
) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("#FDF2F8") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    var thumbnail by remember { mutableStateOf<ByteArray?>(null) }
    var loadingImage by remember { mutableStateOf(false) }
    var customColor by rememberSaveable { mutableStateOf(false) }
    var hex by rememberSaveable { mutableStateOf("") }
    val preview = remember(thumbnail) { thumbnail?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loadingImage = true; error = null
            scope.launch {
                try { thumbnail = loadCollectionThumbnail(resolver, uri) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Couldn't read this image. Choose a PNG, JPEG or WebP under 10 MB." }
                finally { loadingImage = false }
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = Color(0xFFF5FFFD), scrimColor = Color.Black.copy(alpha = .6f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.Hidden || !saving }),
        dragHandle = { Box(Modifier.padding(top = 12.dp).size(40.dp, 6.dp).background(Color(0xFF9CA3AF), CircleShape)) },
    ) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 46.dp, bottom = 32.dp)) {
            CollectionText("New Collection", 20, FontWeight.Bold, Color(0xFF1A1C1D), Modifier.height(30.dp))
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(name, { name = it.take(80); error = null },
                placeholder = { CollectionText("Collection name", 16, color = Color(0xFF9CA3AF)) },
                textStyle = TextStyle(fontFamily = StudioFont, fontSize = 16.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White, disabledContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFD1D5DB), focusedBorderColor = CollectionGreen),
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)
                    .semantics { contentDescription = "Collection name" },
                singleLine = true, enabled = !saving)
            Spacer(Modifier.height(16.dp))
            CollectionText("Collection thumbnail", 14, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp).height(20.dp))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFCE7F3))
                    .border(1.dp, Color(0xFFFBCFE8), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    if (preview != null) Image(preview.asImageBitmap(), "Collection thumbnail preview",
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Asset(R.drawable.collection_choose_image, 24.dp)
                }
                OutlinedButton(enabled = !saving && !loadingImage, modifier = Modifier.weight(1f).height(40.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp), shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CollectionGreen),
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Asset(R.drawable.collection_choose_image, 16.dp)
                    Spacer(Modifier.width(10.dp))
                    CollectionText(if (loadingImage) "Loading image…" else "Choose thumbnail", 16, FontWeight.SemiBold)
                }
            }
            if (thumbnail != null) TextButton(enabled = !saving && !loadingImage, onClick = { thumbnail = null }) { CollectionText("Remove thumbnail", 14) }
            Spacer(Modifier.height(16.dp))
            CollectionText("Collection background", 14, FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp).height(20.dp))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(android.graphics.Color.parseColor(color)))
                .border(1.dp, Color(0xFFFCE7F3), RoundedCornerShape(16.dp))
                .semantics { contentDescription = "Selected background $color" })
            Spacer(Modifier.height(20.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val customSelected = color !in CollectionPalette
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                    .border(if (customSelected) 2.dp else 1.dp, if (customSelected) CollectionGreen else Color(0xFFE5E7EB), CircleShape)
                    .semantics { contentDescription = "Custom background color" }
                    .selectable(customSelected, enabled = !saving, role = Role.RadioButton) { hex = color; customColor = true }
                    .padding(3.dp).background(Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF4ADE80), Color(0xFFF472B6))), CircleShape)
                    .padding(2.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
                    Asset(R.drawable.collection_custom_color, 20.dp)
                }
                CollectionPalette.forEach { value ->
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(value)))
                        .border(if (value == color) 2.dp else 1.dp, if (value == color) CollectionGreen else Color(0xFFE5E7EB), CircleShape)
                        .semantics { contentDescription = "Background $value" }
                        .selectable(value == color, enabled = !saving, role = Role.RadioButton) { color = value })
                }
            }
            Spacer(Modifier.height(24.dp))
            error?.let { Text(it, fontFamily = StudioFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.error); Spacer(Modifier.height(12.dp)) }
            Button(enabled = !saving && !loadingImage && name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CollectionGreen), shape = RoundedCornerShape(12.dp),
                onClick = {
                    saving = true; error = null
                    scope.launch {
                        try {
                            createCollection(name.trim(), color, thumbnail)
                            onCreated()
                            onDismiss()
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = "Couldn't create collection. Please try again. Your choices are still here." }
                        finally { saving = false }
                    }
                }) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                else CollectionText("Create", 18, FontWeight.SemiBold, Color.White)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, CollectionGreen)) {
                CollectionText("Cancel", 18, FontWeight.SemiBold, Color(0xFF4B5563))
            }
        }
    }
    if (customColor) AlertDialog(onDismissRequest = { customColor = false }, title = { CollectionText("Background color", 20, FontWeight.Bold) },
        text = { OutlinedTextField(hex, { hex = it.take(7) }, label = { Text("Hex color") }, placeholder = { Text("#FDF2F8") }, singleLine = true) },
        confirmButton = { TextButton(enabled = Regex("^#[0-9A-Fa-f]{6}$").matches(hex), onClick = { color = hex.uppercase(); customColor = false }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = { customColor = false }) { Text("Cancel") } })
}

@Composable
private fun CollectionText(text: String, size: Int, weight: FontWeight = FontWeight.Normal,
    color: Color = CollectionInk, modifier: Modifier = Modifier) {
    Text(text, modifier, color = color, fontFamily = StudioFont, fontWeight = weight, fontSize = size.sp,
        lineHeight = (size * 1.5f).sp, letterSpacing = 0.sp,
        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
}
