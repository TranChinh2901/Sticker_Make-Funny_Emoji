package com.stickermaker.funnyemoji.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.stickermaker.funnyemoji.data.loadCollectionThumbnail
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stickermaker.funnyemoji.data.CollectionRepository
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewCollectionSheet(onDismiss: () -> Unit, onCreated: () -> Unit = {}) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("#F8FBFF") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    var thumbnail by remember { mutableStateOf<ByteArray?>(null) }
    var loadingImage by remember { mutableStateOf(false) }
    val preview = remember(thumbnail) { thumbnail?.let { BitmapFactory.decodeByteArray(it, 0, it.size) } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            loadingImage = true
            error = null
            scope.launch {
                try { thumbnail = loadCollectionThumbnail(resolver, uri) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Không thể đọc ảnh. Chọn ảnh PNG/JPEG/WebP dưới 10 MB." }
                finally { loadingImage = false }
            }
        }
    }
    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }, containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 16.dp).size(40.dp, 6.dp)
                .background(Color(0xFF9CA3AF), CircleShape))
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("New Collection", Modifier.align(Alignment.CenterHorizontally), fontFamily = Baloo,
                fontSize = 24.sp, fontWeight = FontWeight.Bold)
            if (saved) {
                Text("Đã lưu bộ sưu tập trên Supabase.")
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
            } else {
                Text("Collection name", fontFamily = Baloo, fontSize = 16.sp)
                OutlinedTextField(name, { if (it.length <= 80) name = it },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), singleLine = true, enabled = !saving)
                Text("Collection thumbnail", fontFamily = Baloo)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFCE7F3)).border(1.dp, Color(0xFFFBCFE8), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center) {
                        preview?.let { bitmap ->
                            Image(bitmap.asImageBitmap(), "Collection thumbnail preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } ?: Text("+", fontSize = 28.sp, color = StickerGreen)
                    }
                    OutlinedButton(enabled = !saving && !loadingImage, modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, StickerGreen),
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text(if (loadingImage) "Đang xử lý ảnh…" else "Choose thumbnail", fontFamily = Baloo)
                    }
                }
                if (thumbnail != null) TextButton(enabled = !saving, onClick = { thumbnail = null }) { Text("Remove thumbnail") }
                Text("Collection background", fontFamily = Baloo)
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                    .background(Color(android.graphics.Color.parseColor(color)))
                    .border(1.dp, Color(0xFFFCE7F3), RoundedCornerShape(16.dp))
                    .semantics { contentDescription = "Selected background $color" })
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("#F8FBFF", "#FFFFFF", "#00B686", "#FBCFE8", "#CCFBF1", "#FEF9C3", "#E9D5FF").forEach { hex ->
                        Box(Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                            .border(if (hex == color) 2.dp else 0.dp, if (hex == color) StickerGreen else Color.Transparent, CircleShape)
                            .semantics { contentDescription = "Background $hex" }
                            .clickable(enabled = !saving, role = Role.RadioButton) { color = hex })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !saving && !loadingImage && name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            CollectionRepository.create(name, color, thumbnail)
                            saved = true
                            onCreated()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: IllegalStateException) {
                            error = failure.message ?: "Không thể lưu bộ sưu tập."
                        } catch (_: Exception) {
                            error = "Không thể lưu lên Supabase. Kiểm tra kết nối, migration sticker_collections và quyền truy cập."
                        } finally { saving = false }
                    }
                }) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Create Collection")
                }
                OutlinedButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, StickerGreen)) { Text("Cancel") }
            }
        }
    }
}
