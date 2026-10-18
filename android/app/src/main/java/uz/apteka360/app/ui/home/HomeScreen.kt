package uz.apteka360.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.apteka360.app.data.AiQueueItem
import uz.apteka360.app.data.BasicReq
import uz.apteka360.app.data.MobileState
import uz.apteka360.app.data.Shift
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.ErrorBox
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.LoadingBox
import uz.apteka360.app.ui.components.SectionTitle
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.som
import uz.apteka360.app.ui.components.statusLabel
import uz.apteka360.app.ui.components.statusTone

sealed interface Sheet {
    data object Advance : Sheet
    data object Leave : Sheet
    data object Swap : Sheet
    data object Appeal : Sheet
    data class Decide(val req: BasicReq) : Sheet
    data class AiDecide(val item: AiQueueItem) : Sheet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel) {
    val ui by vm.ui.collectAsState()
    val snack = remember { SnackbarHostState() }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    LaunchedEffect(ui.toast) { ui.toast?.let { snack.showSnackbar(it); vm.consumeToast() } }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(vm.me.ini.ifBlank { vm.me.name.take(1) }, 36)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(vm.me.name, style = MaterialTheme.typography.titleMedium)
                        Text(vm.me.title.ifBlank { roleLabel(vm.me.role) }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            })
        },
        snackbarHost = { SnackbarHost(snack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val st = ui.state
            when {
                ui.loading -> LoadingBox()
                ui.error != null && st == null -> ErrorBox(ui.error!!, onRetry = { vm.load() })
                st != null -> PullToRefreshBox(isRefreshing = ui.refreshing, onRefresh = { vm.load(refresh = true) }) {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                        if (vm.me.isEmployee) employeeContent(st, vm, onSheet = { sheet = it })
                        if (vm.me.isManager || vm.me.canDecideAi) managerContent(st, vm, onSheet = { sheet = it })
                        if (!vm.me.isEmployee && !vm.me.isManager && !vm.me.canDecideAi) item { EmptyBox("Bu rol uchun mobil ekranlar yo‘q (${roleLabel(vm.me.role)}).") }
                        pushes(st, vm)
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
    sheet?.let { RequestSheets(it, ui, vm, onDismiss = { sheet = null }) }
}

fun roleLabel(role: String) = when (role) {
    "EMPLOYEE" -> "Xodim"; "MANAGER" -> "Filial mudiri"; "OWNER" -> "Egasi"; "OPERATOR" -> "Video operator"
    "AUDITOR" -> "Auditor"; "FINANCE" -> "Moliya"; "HR" -> "HR"; "SUPER_ADMIN" -> "Administrator"; else -> role
}

// ---------------- XODIM ----------------

private fun androidx.compose.foundation.lazy.LazyListScope.employeeContent(st: MobileState, vm: HomeViewModel, onSheet: (Sheet) -> Unit) {
    item { SectionTitle("Haftalik grafik") ; ShiftsRow(st.shifts) }

    item {
        SectionTitle("Tezkor amallar")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Avans", Icons.Outlined.AccountBalanceWallet, Modifier.weight(1f), enabled = st.avReq?.status != "pending") { onSheet(Sheet.Advance) }
            QuickAction("Ta’til", Icons.Outlined.BeachAccess, Modifier.weight(1f), enabled = st.lvReq?.status != "pending") { onSheet(Sheet.Leave) }
            QuickAction("Smena", Icons.Outlined.SwapHoriz, Modifier.weight(1f), enabled = st.swapReq?.status !in setOf("mate", "manager")) { onSheet(Sheet.Swap) }
        }
    }

    item {
        AppCard {
            KeyValueRow("Avans limiti", st.advanceLimit.som())
            KeyValueRow("Ta’til qoldig‘i", "${st.leaveBalance} kun (ishlatilgan ${st.lvUsed})")
        }
    }

    if (st.penalty.status != "none") item {
        SectionTitle("Jarima")
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${st.penalty.ruleCode} · ${st.penalty.amount.som()}", style = MaterialTheme.typography.titleMedium)
                    if (st.penalty.reduced > 0) Text("Kamaytirildi: ${st.penalty.reduced.som()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (st.penalty.note.isNotBlank()) Text(st.penalty.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(statusLabel(st.penalty.status), statusTone(st.penalty.status))
            }
            if (st.penalty.status == "confirmed") {
                Spacer(Modifier.height(10.dp))
                Text("E’tiroz muddati: ${st.penalty.appealDeadline}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onSheet(Sheet.Appeal) }, modifier = Modifier.fillMaxWidth()) { Text("E’tiroz bildirish") }
            }
        }
    }

    val hasReq = st.avReq != null || st.lvReq != null || st.swapReq != null
    if (hasReq) item {
        SectionTitle("Mening so‘rovlarim")
        AppCard(padding = PaddingValues(0.dp)) {
            st.avReq?.let { r -> ReqLine("Avans", r.amount.som() + (if (r.note.isNotBlank()) " · ${r.note}" else ""), r.status) }
            st.lvReq?.let { r -> if (st.avReq != null) HorizontalDivider(); ReqLine("Ta’til · ${r.type}", "${r.label} · ${r.days} kun" + (if (r.cover.isNotBlank()) " · o‘rinbosar: ${r.cover}" else ""), r.status) }
            st.swapReq?.let { r -> if (st.avReq != null || st.lvReq != null) HorizontalDivider(); ReqLine("Smena almashish", "${r.d}-kun → ${r.mateName}" + (if (r.backTitle.isNotBlank()) " · evaz: ${r.backTitle}" else ""), r.status) }
        }
    }

    if (st.mateRequests.isNotEmpty()) {
        item { SectionTitle("Hamkasblar so‘rovi") }
        items(st.mateRequests, key = { "mate-" + it.requesterId }) { m ->
            AppCard {
                Text("${m.requesterName ?: m.requesterId} ${m.d}-kun smenasini almashishni so‘radi", style = MaterialTheme.typography.titleSmall)
                if (m.backTitle.isNotBlank()) Text("Evaz: ${m.backTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.reason.isNotBlank()) Text("Sabab: ${m.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.command("Rozilik yuborildi") { vm.repo.swapMateAccept(m.requesterId) } }, enabled = !vm.ui.value.busy, modifier = Modifier.fillMaxWidth()) { Text("Roziman") }
            }
        }
    }
}

@Composable
private fun ShiftsRow(shifts: List<Shift>) {
    if (shifts.isEmpty()) { EmptyBox("Grafik hali tuzilmagan"); return }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        shifts.take(7).forEach { s ->
            val now = s.status == "now"
            val bg = when {
                now -> MaterialTheme.colorScheme.primary
                s.status == "pending" || s.status == "given" -> Color(0x2EE2A336)
                else -> MaterialTheme.colorScheme.surface
            }
            val fg = if (now) Color.White else MaterialTheme.colorScheme.onSurface
            Column(
                Modifier.weight(1f).background(bg, RoundedCornerShape(12.dp))
                    .then(if (!now) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)) else Modifier)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${s.d}", style = MaterialTheme.typography.titleMedium, color = fg)
                Text(if (s.kind == "night") "Tun" else "Kun", style = MaterialTheme.typography.labelMedium, color = if (now) Color.White.copy(.85f) else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(s.time.replace("–", "\n"), style = MaterialTheme.typography.bodySmall, color = if (now) Color.White.copy(.9f) else MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
            }
        }
    }
}

@Composable
private fun QuickAction(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, enabled = enabled, modifier = modifier.height(72.dp), shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ReqLine(title: String, sub: String, status: String) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Badge(statusLabel(status), statusTone(status))
    }
}

// ---------------- MUDIR / OPERATOR ----------------

private fun androidx.compose.foundation.lazy.LazyListScope.managerContent(st: MobileState, vm: HomeViewModel, onSheet: (Sheet) -> Unit) {
    if (vm.me.isManager) {
        val pending = st.basic.filter { it.status == "pending" }
        item { SectionTitle("Kutayotgan so‘rovlar", trailing = { if (pending.isNotEmpty()) Badge("${pending.size}", Tone.Warning) }) }
        if (pending.isEmpty()) item { AppCard { Text("Hozircha kutayotgan so‘rov yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(pending, key = { "basic-" + it.id }) { r ->
            AppCard(Modifier.clickable { onSheet(Sheet.Decide(r)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.titleSmall)
                        Text(r.sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Badge(r.type.ifBlank { "So‘rov" }, Tone.Info)
                }
                if (r.rows.isNotEmpty()) { Spacer(Modifier.height(8.dp)); r.rows.take(3).forEach { row -> KeyValueRow(row.getOrElse(0) { "" }, row.getOrElse(1) { "" }) } }
                Spacer(Modifier.height(6.dp))
                Text("Ko‘rib chiqish →", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    val ai = st.aiQueue.filter { it.status == "pending" }
    item { SectionTitle("AI hodisalar navbati", trailing = { if (ai.isNotEmpty()) Badge("${ai.size}", Tone.Danger) }) }
    if (ai.isEmpty()) item { AppCard { Text("AI navbati bo‘sh", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    items(ai, key = { "ai-" + it.id }) { e ->
        AppCard(Modifier.clickable { onSheet(Sheet.AiDecide(e)) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.label, style = MaterialTheme.typography.titleSmall)
                    Text("${e.emp} · ${e.cam} · ${e.time}" + (if (e.dur.isNotBlank()) " · ${e.dur}" else ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge("${e.conf}%", if (e.conf >= 80) Tone.Danger else Tone.Warning)
            }
        }
    }
}

// ---------------- PUSH xabarlar ----------------

private fun androidx.compose.foundation.lazy.LazyListScope.pushes(st: MobileState, vm: HomeViewModel) {
    if (st.pushes.isEmpty()) return
    val unread = st.pushes.count { !it.read }
    item {
        SectionTitle("Xabarlar", trailing = {
            if (unread > 0) androidx.compose.material3.TextButton(onClick = {
                vm.command { vm.repo.pushesRead(if (vm.me.isEmployee) "EMPLOYEE" else "MANAGER") }
            }) { Text("O‘qildi deb belgilash") }
        })
    }
    items(st.pushes.take(20), key = { "push-" + it.id }) { p ->
        AppCard(padding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                if (!p.read) Box(Modifier.padding(top = 6.dp, end = 10.dp).size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                Column(Modifier.weight(1f)) {
                    Text(p.text, style = MaterialTheme.typography.bodyMedium, fontWeight = if (p.read) FontWeight.Normal else FontWeight.Medium)
                    Text(p.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
