package com.stickermaker.funnyemoji.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stickermaker.funnyemoji.data.FavoritesStore
import kotlinx.coroutines.launch

internal val LocalFavorites = staticCompositionLocalOf<FavoritesStore> { error("FavoritesStore missing") }

@Composable
internal fun CatalogCard(id: String, modifier: Modifier, onUnlock: () -> Unit) {
    if (id.startsWith("hug-")) {
        HugCatalogCard(id.substringAfter("-").toInt(), modifier, onUnlock)
        return
    }
    val store = LocalFavorites.current
    val state by store.snapshot.collectAsState()
    val scope = rememberCoroutineScope()
    StickerCard(id, modifier, id in state.ids, { selected ->
        store.choose(id, selected)
        scope.launch { store.sync() }
    }, onUnlock)
}

@Composable
internal fun FavoriteSyncStatus() {
    val store = LocalFavorites.current
    val state by store.snapshot.collectAsState()
    val syncing by store.syncing.collectAsState()
    val error by store.error.collectAsState()
    val scope = rememberCoroutineScope()
    if (state.pending.isNotEmpty() || error != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(if (syncing) "Syncing favorites…" else error ?: "Favorites waiting to sync",
                Modifier.weight(1f), fontSize = 12.sp)
            TextButton(enabled = !syncing, onClick = { scope.launch { store.sync() } }) { Text("Retry") }
        }
    }
}
