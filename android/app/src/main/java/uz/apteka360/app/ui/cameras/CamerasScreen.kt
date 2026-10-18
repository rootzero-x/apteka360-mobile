package uz.apteka360.app.ui.cameras

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.Camera
import uz.apteka360.app.data.Repository
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.ErrorBox
import uz.apteka360.app.ui.components.LoadingBox
import uz.apteka360.app.ui.components.statusLabel
import uz.apteka360.app.ui.components.statusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CamerasScreen(repo: Repository, onOpen: (Camera) -> Unit) {
    var items by remember { mutableStateOf<List<Camera>?>(null) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() { try { items = repo.cameras(); error = null } catch (e: Exception) { if (items == null) error = e } finally { refreshing = false } }
    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Kameralar") }) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val list = items
            val err = error
            when {
                list == null && err == null -> LoadingBox()
                list == null && err is ApiException && err.code == "module_disabled" -> EmptyBox("Kameralar moduli yoqilmagan. Egasi Tizim → Modullar’da yoqishi kerak.", Modifier.fillMaxSize())
                list == null && err is ApiException && err.status == 403 -> EmptyBox("Bu rol uchun kameralar ko‘rsatilmaydi.", Modifier.fillMaxSize())
                list == null -> ErrorBox(err!!, onRetry = { scope.launch { load() } })
                else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; scope.launch { load() } }) {
                    if (list.isEmpty()) EmptyBox("Kamera yo‘q", Modifier.fillMaxSize())
                    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        items(list, key = { it.id }) { cam ->
                            val status = cam.status ?: if (cam.health == "ok") "online" else "unknown"
                            AppCard(Modifier.clickable(enabled = !cam.privacy) { onOpen(cam) }, padding = PaddingValues(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (cam.privacy) Icons.Outlined.Lock else Icons.Outlined.Videocam, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(cam.name, style = MaterialTheme.typography.titleSmall)
                                        Text(listOf(cam.zones, cam.ai).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (cam.privacy) Text("Maxfiy kamera — jonli ko‘rish yo‘q", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Badge(statusLabel(status), statusTone(status))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
