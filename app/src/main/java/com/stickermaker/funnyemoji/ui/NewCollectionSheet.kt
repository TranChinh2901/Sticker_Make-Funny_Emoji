package com.stickermaker.funnyemoji.ui

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
internal fun NewCollectionSheet(onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf("#F8FBFF") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }, containerColor = Color.White,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("New Collection", fontFamily = Baloo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (saved) {
                Text("Đã lưu bộ sưu tập trên Supabase.")
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
            } else {
                OutlinedTextField(name, { if (it.length <= 80) name = it }, label = { Text("Collection name") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving)
                Text("Collection thumbnail", fontFamily = Baloo)
                Text("Ảnh đại diện mặc định. Chọn và tải ảnh lên Storage sẽ được nối ở bước tiếp theo.", style = MaterialTheme.typography.bodySmall)
                Text("Collection background", fontFamily = Baloo)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf("#F8FBFF", "#D5F2ED", "#FFE1E7", "#FFF3CC", "#E9E1FF").forEach { hex ->
                        Box(Modifier.size(44.dp).background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                            .border(if (hex == color) 2.dp else 0.dp, if (hex == color) StickerGreen else Color.Transparent, CircleShape)
                            .semantics { contentDescription = "Background $hex" }
                            .clickable(enabled = !saving, role = Role.RadioButton) { color = hex })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !saving && name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth(), onClick = {
                    saving = true
                    error = null
                    scope.launch {
                        try {
                            CollectionRepository.create(name, color)
                            saved = true
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
                TextButton(onClick = onDismiss, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
