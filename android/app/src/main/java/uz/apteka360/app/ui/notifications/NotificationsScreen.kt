package uz.apteka360.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.apteka360.app.data.Notification
import uz.apteka360.app.data.Repository
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.ErrorBox
import uz.apteka360.app.ui.components.LoadingBox
import uz.apteka360.app.ui.components.statusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(repo: Repository) {
    var items by remember { mutableStateOf<List<Notification>?>(null) }
    var error by remember { mutableStateOf<Throwable?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    suspend fun load() { try { items = repo.notifications(); error = null } catch (e: Exception) { if (items == null) error = e } finally { refreshing = false } }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bildirishnomalar") }, actions = {
            if (items?.any { it.unread } == true) TextButton(onClick = { scope.launch { runCatching { repo.markAllRead() }; load() } }) { Text("Hammasini o‘qish") }
        }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val list = items
            when {
                list == null && error == null -> LoadingBox()
                list == null -> ErrorBox(error!!, onRetry = { scope.launch { load() } })
                else -> PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; scope.launch { load() } }) {
                    if (list.isEmpty()) EmptyBox("Bildirishnoma yo‘q", Modifier.fillMaxSize())
                    else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                        items(list, key = { it.id }) { n ->
                            AppCard(Modifier.clickable(enabled = n.unread) { scope.launch { runCatching { repo.markRead(n.id) }; items = list.map { if (it.id == n.id) it.copy(readByMe = true, read = true) else it } } }, padding = PaddingValues(14.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    if (n.unread) Box(Modifier.padding(top = 7.dp, end = 10.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                                    Column(Modifier.weight(1f)) {
                                        Text(n.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (n.unread) FontWeight.SemiBold else FontWeight.Normal)
                                        Spacer(Modifier.height(4.dp))
                                        Text(listOf(n.cat, n.source, n.time).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (n.action.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(n.action, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                                    }
                                    Badge(n.pr, statusTone(n.pr))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
