package uz.apteka360.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.apteka360.app.AppContainer
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.SearchField
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader

@Composable
fun AuditScreen(c: AppContainer) {
    val loader = rememberLoader { c.branchesCached(); c.repo.audit() }
    var q by rememberSaveable { mutableStateOf("") }
    var onlySens by rememberSaveable { mutableStateOf(false) }
    val shown = loader.data?.filter { x -> (!onlySens || x.sensitive) && (q.isBlank() || listOf(x.user, x.action, x.`object`, x.device, x.reason).joinToString(" ").contains(q, ignoreCase = true)) }
    ScreenScaffold("Audit jurnali", shown?.let { "${it.size} ta yozuv" }) {
        DataBox(loader) { _ ->
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    SearchField(q, { q = it }, "Foydalanuvchi, amal, obyekt")
                    Row(Modifier.padding(top = 8.dp)) { FilterChip(selected = onlySens, onClick = { onlySens = !onlySens }, label = { Text("Faqat sezgir") }) }
                }
                val list = shown ?: emptyList()
                if (list.isEmpty()) EmptyBox("Yozuv topilmadi", Modifier.fillMaxSize())
                else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { InfoBar("O‘zgarmas jurnal.", "Audit yozuvlarini o‘chirib yoki tahrirlab bo‘lmaydi — har bir moliyaviy va xavfsizlik amali shu yerda qayd etiladi.") }
                    items(list, key = { it.id }) { x ->
                        AppCard(padding = PaddingValues(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(Fmt.initials(x.user), 32); Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) { Text(x.user, style = MaterialTheme.typography.titleSmall); Text(if (x.at != null) Fmt.dayMonthTime(x.at) else x.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                if (x.sensitive) Badge("sezgir", Tone.Warning)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(x.action, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(listOf(x.`object`, c.branchName(x.branchId)).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val change = if ((x.before.isNotBlank() && x.before != "—") || (x.after.isNotBlank() && x.after != "—")) "${x.before} → ${x.after}" else null
                            if (change != null) Text(change, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                            if (x.reason.isNotBlank() && x.reason != "—") Text("Sabab: ${x.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (x.device.isNotBlank()) Text(x.device, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f), modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
