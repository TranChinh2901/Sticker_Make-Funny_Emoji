package com.stickermaker.funnyemoji.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.R
import com.stickermaker.funnyemoji.data.*
import com.stickermaker.funnyemoji.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

@Composable
internal fun EditorScreen(collectionId: String? = null, onBack: () -> Unit, onSaved: (SavedSticker) -> Unit,
    saveDraft: ((StickerDocument) -> Unit)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val export = rememberPngExport()
    var doc by remember { mutableStateOf(StickerDocument()) }
    var ready by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var undo by remember { mutableStateOf(listOf<StickerDocument>()) }
    var redo by remember { mutableStateOf(listOf<StickerDocument>()) }
    var selected by remember { mutableStateOf<String?>(null) }
    var tool by rememberSaveable { mutableStateOf("Background") }
    var palette by rememberSaveable { mutableStateOf("Basic") }
    var color by rememberSaveable { mutableLongStateOf(0xFF1F2937) }
    var brushWidth by rememberSaveable { mutableFloatStateOf(8f) }
    var textFont by rememberSaveable { mutableStateOf("Default") }
    var text by rememberSaveable { mutableStateOf("") }
    var layersOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val draftMutex = remember { Mutex() }
    suspend fun persistDraft(snapshot: StickerDocument) = withContext(Dispatchers.IO) {
        draftMutex.withLock {
            ensureActive()
            if (saveDraft != null) saveDraft(snapshot) else EditorDraft.write(context, snapshot)
        }
    }
    var importing by remember { mutableStateOf(false) }
    var saveOpen by remember { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("My sticker") }
    var operationId by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    var gestureStart by remember { mutableStateOf<StickerDocument?>(null) }
    val change: (StickerDocument) -> Unit = { next ->
        if (next != doc && !saving) {
            undo = (undo + doc).takeLast(30); redo = emptyList(); doc = next
            operationId = UUID.randomUUID().toString(); error = null
        }
    }
    LaunchedEffect(Unit) {
        doc = withContext(Dispatchers.IO) { EditorDraft.read(context) }; ready = true
    }
    LaunchedEffect(doc, ready) {
        if (ready) {
            try { persistDraft(doc) }
            catch (_: java.io.IOException) { error = "Không thể lưu bản nháp trên thiết bị." }
            preview = withContext(Dispatchers.Default) { renderSticker(context, doc) }
        }
    }
    val leaveEditor: () -> Unit = {
        if (ready && !saving && !importing && !export.busy) {
            saving = true; error = null
            val snapshot = doc
            scope.launch {
                try { persistDraft(snapshot); onBack() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Couldn't save your draft. Please try Back again." }
                finally { saving = false }
            }
        }
    }
    BackHandler(onBack = leaveEditor)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            importing = true; error = null
            scope.launch {
                try {
                    val bytes = loadCollectionThumbnail(context.contentResolver, uri)
                    val fileName = UUID.randomUUID().toString() + ".png"
                    withContext(Dispatchers.IO) {
                        File(context.filesDir, "imports").mkdirs()
                        File(context.filesDir, "imports/$fileName").writeBytes(bytes)
                    }
                    val layer = StickerLayer(kind = "image", value = fileName)
                    change(doc.copy(layers = doc.layers + layer)); selected = layer.id
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Không thể đọc ảnh. Chọn ảnh PNG/JPG nhỏ hơn 10 MB." }
                finally { importing = false }
            }
        }
    }
    val colors = listOf(0L, 0xFFFFFFFF, 0xFFFF0000, 0xFFFF3399, 0xFF9900CC, 0xFF6633CC, 0xFF0099FF, 0xFF00CCCC)
    val tools = listOf("Background", "Import", "Text", "Sticker", "Outline", "Brush")
    val icons = listOf(R.drawable.editor_hugeicons_background, R.drawable.editor_group,
        R.drawable.editor_solar_text_linear, R.drawable.editor_radix_icons_face, 0, R.drawable.editor_heroicons_paint_brush)
    Column(Modifier.fillMaxSize().background(StickerBackground).navigationBarsPadding().imePadding()) {
        Box(Modifier.fillMaxWidth().padding(top = 44.dp).height(48.dp)) {
            IconButton(onClick = leaveEditor, enabled = ready && !saving && !importing && !export.busy,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Asset(R.drawable.editor_button_svg, 24.dp, description = "Back")
            }
            Text("Create", Modifier.align(Alignment.Center), fontFamily = Baloo, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(63.dp, 48.dp), contentAlignment = Alignment.Center) {
                Button(onClick = { saveOpen = true }, enabled = ready && doc.hasContent && !saving && !importing,
                    modifier = Modifier.size(63.dp, 32.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFF3F4F6)),
                    contentPadding = PaddingValues(0.dp)) { Text("Save", fontFamily = Baloo) }
            }
        }
        if (!saveOpen) error?.let {
            Text(it, Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.error, fontFamily = Baloo)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (tool == "Background") 40.dp else 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(enabled = undo.isNotEmpty() && !saving, onClick = {
                    redo = (redo + doc).takeLast(30); doc = undo.last(); undo = undo.dropLast(1); operationId = UUID.randomUUID().toString()
                }, modifier = Modifier.background(Color.White, CircleShape)) {
                    Asset(R.drawable.editor_svg, 20.dp, description = "Undo", tint = if (undo.isEmpty()) Color.Gray else StickerGreen)
                }
                Spacer(Modifier.width(8.dp))
                IconButton(enabled = redo.isNotEmpty() && !saving, onClick = {
                    undo = (undo + doc).takeLast(30); doc = redo.last(); redo = redo.dropLast(1); operationId = UUID.randomUUID().toString()
                }, modifier = Modifier.background(Color.White, CircleShape)) { Asset(R.drawable.editor_svg1, 20.dp, description = "Redo") }
                Spacer(Modifier.weight(1f))
                AssetButton(R.drawable.editor_fe_layer, "Layers", { layersOpen = true })
            }
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(32.dp))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(32.dp))
                .semantics { contentDescription = "Sticker canvas. Drag to move selected layer or draw with Brush." }
                .pointerInput(tool, selected, saving, color, brushWidth) {
                    detectDragGestures(onDragStart = { point ->
                        if (!saving) {
                            gestureStart = doc
                            if (tool == "Brush" && doc.strokes.size < 200) doc = doc.copy(strokes = doc.strokes + InkStroke(
                                listOf(InkPoint(point.x / size.width, point.y / size.height)), color, brushWidth))
                        }
                    }, onDragEnd = {
                        gestureStart?.let { before ->
                            if (before != doc) { undo = (undo + before).takeLast(30); redo = emptyList(); operationId = UUID.randomUUID().toString() }
                        }; gestureStart = null
                    }, onDragCancel = { gestureStart?.let { doc = it }; gestureStart = null }) { event, delta ->
                        if (!saving && gestureStart != null) {
                            event.consume()
                            if (tool == "Brush" && doc.strokes.size > gestureStart!!.strokes.size) {
                                val last = doc.strokes.last()
                                if (last.points.size < 2000) doc = doc.copy(strokes = doc.strokes.dropLast(1) + last.copy(points = last.points +
                                    InkPoint((event.position.x / size.width).coerceIn(0f, 1f), (event.position.y / size.height).coerceIn(0f, 1f))))
                            } else doc = doc.copy(layers = doc.layers.map {
                                if (it.id == selected) it.copy(x = (it.x + delta.x / size.width).coerceIn(0f, 1f),
                                    y = (it.y + delta.y / size.height).coerceIn(0f, 1f)) else it
                            })
                        }
                    }
                }) {
                Image(painterResource(R.drawable.editor_main_canvas_checkerboard), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                preview?.let { Image(it.asImageBitmap(), "Sticker preview", Modifier.fillMaxSize()) }
            }
            if (importing) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Surface(shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().then(if (tool == "Text") Modifier.height(220.dp) else Modifier.heightIn(max = 230.dp))
                .verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tool == "Text") Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(48.dp).border(1.dp, Color(0xFFBFC9C3), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                        androidx.compose.foundation.text.BasicTextField(text, { text = it.take(40) },
                            modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !saving,
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Baloo, fontSize = 18.sp, color = StickerInk),
                            decorationBox = { field ->
                                if (text.isEmpty()) Text("Hello!", fontFamily = Baloo, fontSize = 18.sp, color = Color(0xFF9CA3AF))
                                field()
                            })
                    }
                    Spacer(Modifier.width(12.dp))
                    IconButton(enabled = text.isNotBlank() && !saving && doc.layers.size < 30, onClick = {
                        val layer = StickerLayer(kind = "text", value = text.trim(), color = color, font = textFont)
                        change(doc.copy(layers = doc.layers + layer)); selected = layer.id; text = ""
                    }, modifier = Modifier.semantics { contentDescription = "Add text" }) {
                        Image(painterResource(R.drawable.editor_text_confirm), null, Modifier.size(20.dp, 16.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    colors.forEach { c ->
                        Box(Modifier.size(32.dp).clip(CircleShape).background(if (c == 0L) Color.White else Color(c))
                            .border(if ((if (tool == "Background") doc.background else color) == c) 2.dp else 1.dp,
                                if ((if (tool == "Background") doc.background else color) == c) StickerGreen else Color(0xFFE5E7EB), CircleShape)
                            .clickable(enabled = !saving) {
                                if (tool == "Background") change(doc.copy(background = c)) else {
                                    color = if (c == 0L) 0xFFFFFFFF else c
                                    if (tool == "Text") change(doc.copy(layers = doc.layers.map { if (it.id == selected) it.copy(color = color) else it }))
                                }
                            }.semantics { contentDescription = if (c == 0L) "Transparent" else "Color ${c.toString(16)}" }) {
                            if (c == 0L) Image(painterResource(R.drawable.editor_border), null, Modifier.fillMaxSize())
                        }
                    }
                }
                when (tool) {
                    "Background" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Basic", "Pastel", "Cute", "Pattern").forEach { label -> Pill(label, palette == label) { palette = label } }
                        }
                        val options = when (palette) {
                            "Pastel" -> listOf("Pink" to 0xFFFBCFE8L, "Lilac" to 0xFFE9D5FFL, "Peach" to 0xFFFFEDD5L)
                            "Cute" -> listOf("Rose" to 0xFFFFE4E6L, "Butter" to 0xFFFEF9C3L, "Sky" to 0xFFDBEAFEL)
                            "Pattern" -> listOf("Dots" to 0xFFFFFFFFL, "Mint dots" to 0xFFE0F7FAL)
                            else -> listOf("Clear" to 0L, "White" to 0xFFFFFFFFL, "Mint" to 0xFFE0F7FAL, "Brand" to 0xFF00B686L)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { (label, c) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(Color(c))
                                        .border(if (doc.background == c) 2.dp else 1.dp, if (doc.background == c) StickerGreen else Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                        .clickable(enabled = !saving) { change(doc.copy(background = c, pattern = palette == "Pattern")) }) {
                                        if (c == 0L) Image(painterResource(R.drawable.editor_border), null, Modifier.fillMaxSize())
                                        if (palette == "Pattern") Text("• • •", Modifier.align(Alignment.Center), color = Color.Gray)
                                    }
                                    Text(label, fontSize = 12.sp, color = StickerGreen)
                                }
                            }
                        }
                    }
                    "Import" -> Button(enabled = !importing && !saving, onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("Choose a photo") }
                    "Text" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Default", "Baloo 2", "Inter", "Poppins").chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { font ->
                                    Surface(Modifier.weight(1f).height(40.dp), shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFECFAF8), border = BorderStroke(if (font == textFont) 2.dp else 0.dp,
                                            if (font == textFont) StickerGreen else Color.Transparent)) {
                                        Box(Modifier.clickable(enabled = !saving) {
                                            textFont = font
                                            change(doc.copy(layers = doc.layers.map {
                                                if (it.id == selected && it.kind == "text") it.copy(font = font) else it
                                            }))
                                        }, contentAlignment = Alignment.Center) {
                                            Text(font, fontFamily = when (font) {
                                                "Baloo 2" -> Baloo
                                                "Inter" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.inter))
                                                "Poppins" -> androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.poppins_regular))
                                                else -> androidx.compose.ui.text.font.FontFamily.Default
                                            }, fontSize = 14.sp)
                                        }
                                    }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    "Sticker" -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("😊", "😍", "😂", "😎", "🥳", "❤️", "✨", "🐱").forEach { emoji ->
                            Text(emoji, Modifier.clickable(enabled = !saving && doc.layers.size < 30) {
                                val layer = StickerLayer(kind = "emoji", value = emoji)
                                change(doc.copy(layers = doc.layers + layer)); selected = layer.id
                            }.padding(4.dp), fontSize = 30.sp)
                        }
                    }
                    "Outline" -> {
                        Text(if (selected == null) "Select a layer first" else "White outline", fontFamily = Baloo)
                        val layer = doc.layers.find { it.id == selected }
                        Slider(layer?.outline ?: 0f, { width -> change(doc.copy(layers = doc.layers.map { if (it.id == selected) it.copy(outline = width) else it })) },
                            valueRange = 0f..16f, enabled = layer != null && !saving)
                    }
                    "Brush" -> {
                        Text("Draw on canvas · ${brushWidth.toInt()} px", fontFamily = Baloo)
                        Slider(brushWidth, { brushWidth = it }, valueRange = 2f..32f)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color.White).height(80.dp).padding(horizontal = 8.dp, vertical = 12.dp)) {
            tools.forEachIndexed { index, label ->
                Column(Modifier.weight(1f).fillMaxHeight().clickable(enabled = !saving) { tool = label },
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    val tint = if (tool == label) StickerGreen else Color(0xFF6B7280)
                    if (icons[index] == 0) Canvas(Modifier.size(24.dp)) {
                        drawCircle(tint, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
                        drawCircle(tint, radius = 6.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    } else Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Asset(icons[index], if (index == 1) 19.5.dp else 24.dp, tint = tint)
                    }
                    Text(label, fontFamily = Baloo, fontSize = 10.sp, color = tint)
                }
            }
        }
    }
    if (layersOpen) AlertDialog(onDismissRequest = { layersOpen = false }, title = { Text("Layers") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            if (doc.layers.isEmpty()) Text("Import a photo, add text or choose a sticker.")
            doc.layers.reversed().forEach { layer ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selected = layer.id }) { Text(if (layer.kind == "image") "Photo" else layer.value) }
                    TextButton(enabled = !saving, onClick = { change(doc.copy(layers = doc.layers.filterNot { it.id == layer.id })); selected = null }) { Text("Delete") }
                    TextButton(enabled = !saving, onClick = { change(doc.copy(layers = doc.layers.filterNot { it.id == layer.id } + layer)) }) { Text("To front") }
                }
                if (selected == layer.id) Slider(layer.scale, { v -> change(doc.copy(layers = doc.layers.map { if (it.id == layer.id) it.copy(scale = v) else it })) }, valueRange = .25f..2f)
            }
            Text("Drag the selected layer on the canvas.")
            TextButton(enabled = doc.hasContent && !saving, onClick = { change(StickerDocument()); selected = null }) { Text("Clear canvas") }
        }
    }, confirmButton = { TextButton(onClick = { layersOpen = false }) { Text("Done") } })
    if (saveOpen) AlertDialog(onDismissRequest = { if (!saving && !export.busy) saveOpen = false }, title = { Text("Save sticker") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it.take(80) }, enabled = !saving, label = { Text("Sticker name") }, singleLine = true)
            Text(if (collectionId == null) "Your sticker will be saved to My Studio." else "Your sticker will be saved to this collection and My Studio.")
            OutlinedButton(enabled = !saving && !export.busy && name.isNotBlank(), onClick = {
                val snapshot = doc
                export.save(name) {
                    withContext(Dispatchers.Default) {
                        renderSticker(context, snapshot).let { bitmap -> try { bitmap.pngBytes() } finally { bitmap.recycle() } }
                    }
                }
            }) { Text("Save PNG to device") }
            if (export.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            export.message?.let { Text(it) }
            if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(enabled = !saving && !export.busy && name.isNotBlank(), onClick = {
        saving = true; error = null
        scope.launch {
            try {
                val png = withContext(Dispatchers.Default) { renderSticker(context, doc).let { bitmap -> try { bitmap.pngBytes() } finally { bitmap.recycle() } } }
                val saved = StudioRepository.save(operationId, name, png)
                collectionId?.let { StudioRepository.add(it, saved.id) }
                persistDraft(StickerDocument())
                saveOpen = false; onSaved(saved)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Không thể lưu sticker. Kiểm tra kết nối rồi thử lại. Bản nháp vẫn được giữ trên máy." }
            finally { saving = false }
        }
    }) { Text(if (saving) "Saving…" else "Save") } }, dismissButton = { TextButton(enabled = !saving && !export.busy, onClick = { saveOpen = false }) { Text("Cancel") } })
}
