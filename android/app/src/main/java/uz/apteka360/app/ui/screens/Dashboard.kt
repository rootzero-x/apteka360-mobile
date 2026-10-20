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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.AiEvent
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.Approval
import uz.apteka360.app.data.Branch
import uz.apteka360.app.data.Dashboard
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.Supplier
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.BarLine
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.DeltaBadge
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.KpiCard
import uz.apteka360.app.ui.components.KpiMoney
import uz.apteka360.app.ui.components.LevelIcon
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.Sparkline
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.nav.Access

data class DashData(val d: Dashboard?, val forbidden: Boolean, val branches: List<Branch>, val approvals: List<Approval>, val suppliers: List<Supplier>, val fomCash: Long, val ai: List<AiEvent>)

/** Desktop `A.screens.dashboard` — KPI, ogohlantirishlar, filiallar bugun, tasdiqlar, AI, pul zaxirasi. */
@Composable
fun DashboardScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit) {
    val role = me.role
    val loader = rememberLoader {
        var forbidden = false
        val d = try { c.repo.dashboard() } catch (e: ApiException) { if (e.status == 403) { forbidden = true; null } else throw e }
        val br = c.branchesCached(true)
        val ap = runCatching { c.repo.approvals() }.getOrDefault(emptyList()).filter { it.status == "pending" }
        val sup = if (Access.can("suppliers", role) || Access.can("payables", role)) runCatching { c.repo.suppliers() }.getOrDefault(emptyList()) else emptyList()
        val fom = runCatching { c.repo.fomCashToday() }.getOrDefault(emptyMap()).values.sumOf { it.naqd }
        val ai = if (Access.can("ai", role)) runCatching { c.repo.aiEvents() }.getOrDefault(emptyList()) else emptyList()
        DashData(d, forbidden, br, ap, sup, fom, ai)
    }
    ScreenScaffold(title = Fmt.greeting(me.name), sub = Fmt.longDate() + " · so‘nggi 30 kun") {
        DataBox(loader) { x ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (x.forbidden || x.d == null) {
                    item { InfoBar("Tarmoq ko‘rsatkichlari sizning rolingiz uchun yopiq.", "Quyidagi bo‘limlardan foydalanishingiz mumkin.") }
                    item { QuickLinks(role, go) }
                    return@LazyColumn
                }
                val d = x.d
                if (!d.connected) item { InfoBar("FOM ulanmagan.", "Tushum, foyda va kassa qoldig‘i FOM sinxronlash agenti ulangandan keyin ko‘rinadi.") }
                val K = mapOf("revenue" to "Tushum", "grossProfit" to "Yalpi foyda", "netProfit" to "Sof foyda", "cash" to "Kassadagi pul", "payroll" to "Oylik fondi", "expenses" to "Harajatlar", "supplierDebt" to "Firmalarga qarz", "payments7d" to "7 kunda to‘lanadi")
                val supDebt = x.suppliers.sumOf { it.debt }; val supDue = x.suppliers.sumOf { it.dueSoon + it.overdue }; val supOver = x.suppliers.sumOf { it.overdue }
                val kpis = d.kpis.map { k ->
                    var v: Long? = k.value; var src = k.source; var foot = if (k.prev != null && k.prev != 0L) "oldingi davr: " + Fmt.moneyShortText(k.prev) else ""
                    when {
                        k.value == 0L && k.key == "supplierDebt" && supDebt > 0 -> { v = supDebt; src = "FIRMALAR"; foot = "muddati o‘tgan: " + Fmt.moneyShortText(supOver) }
                        k.value == 0L && k.key == "payments7d" && supDue > 0 -> { v = supDue; src = "FIRMALAR"; foot = "muddati o‘tgan + yaqin to‘lovlar" }
                        k.key == "cash" && (k.value == 0L || !d.connected) && x.fomCash > 0 -> { v = x.fomCash; src = "KASSALAR"; foot = "bugungi naqd (kassa yopilishi)" }
                    }
                    val noData = !d.connected && (src == "FOM" || k.key == "netProfit")
                    Triple(K[k.key] ?: k.key, if (noData) null else v, Triple(if (noData) null else k.deltaPct, if (noData) "FOM ulanmagan" else foot, src to (if (noData) emptyList() else k.spark)))
                }
                items(kpis.chunked(2)) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { (label, v, meta) -> KpiMoney(label, v, Modifier.weight(1f), delta = meta.first, foot = meta.second, src = meta.third.first, spark = meta.third.second) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                sectionTitle("Tushum dinamikasi · 30 kun")
                item {
                    val series = d.revenueSeries.map { it.actual ?: 0L }
                    AppCard { if (series.all { it == 0L }) Text(if (d.connected) "Bu davr uchun tushum yo‘q" else "FOM ulangach tushum grafigi shu yerda ko‘rinadi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else { Sparkline(series, Modifier.fillMaxWidth().height(90.dp)); Spacer(Modifier.height(6.dp)); Row { Text(Fmt.date(d.revenueSeries.first().date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(Fmt.date(d.revenueSeries.last().date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }
                }
                sectionTitle("Diqqat talab qiladi")
                if (d.alerts.isEmpty()) item { AppCard { Text("Ogohlantirishlar yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                else item { AppCard(padding = PaddingValues(0.dp)) { d.alerts.forEachIndexed { i, a -> if (i > 0) HorizontalDivider(); ListRow(a.title, a.text, leading = { LevelIcon(a.level) }, chevron = a.link != null, onClick = a.link?.let { l -> { linkTo(l, go) } }) } } }
                if (x.branches.isNotEmpty()) {
                    sectionTitle("Filiallar bugun")
                    items(x.branches, key = { "b-" + it.id }) { b -> BranchRowCard(b) { go("branches", b.id) } }
                }
                if (Access.can("approvals", role)) {
                    item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Tasdiqlar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TextButton({ go("approvals", null) }) { Text("Hammasi") } } }
                    if (x.approvals.isEmpty()) item { AppCard { Text("Kutilayotgan tasdiq yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    else item { AppCard(padding = PaddingValues(0.dp)) { x.approvals.take(5).forEachIndexed { i, a -> if (i > 0) HorizontalDivider(); ListRow(a.title, "${a.requester} · ${c.branchName(a.branchId)}", trailing = { if (a.amount != null) Text(Fmt.moneyShortText(a.amount), style = MaterialTheme.typography.labelLarge) }, onClick = { go("approvals", a.id) }) } } }
                }
                if (Access.can("ai", role)) {
                    val open = x.ai.filter { it.status == "pending" || it.status == "new" || it.status == "review" }
                    item { Row(verticalAlignment = Alignment.CenterVertically) { Text("AI hodisalar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); TextButton({ go("ai", null) }) { Text("Review navbati") } } }
                    if (open.isEmpty()) item { AppCard { Text("Ko‘rib chiqiladigan AI hodisa yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    else item { AppCard(padding = PaddingValues(0.dp)) { open.take(5).forEachIndexed { i, e -> if (i > 0) HorizontalDivider(); ListRow(e.label.ifBlank { e.code.ifBlank { "AI hodisa" } }, listOf(e.camera, c.branchName(e.branchId), "${e.date} ${e.time}").filter { it.isNotBlank() }.joinToString(" · "), trailing = { Badge("${e.conf}%", if (e.priority == "yuqori") Tone.Warning else Tone.Info) }, onClick = { go("ai", e.id) }) } } }
                }
                d.runway?.let { r ->
                    sectionTitle("Pul zaxirasi prognozi")
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMoney("30 kundan keyin", if (d.connected) r.d30 else null, Modifier.weight(1f), foot = if (d.connected) null else "FOM ulanmagan")
                        KpiMoney("60 kundan keyin", if (d.connected) r.d60 else null, Modifier.weight(1f), foot = if (d.connected) null else "FOM ulanmagan")
                    } }
                    item { KpiMoney("Eng past nuqta", if (d.connected) r.lowest?.value else null, foot = if (d.connected) r.lowest?.date else "FOM ulanmagan") }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun BranchRowCard(b: Branch, onClick: () -> Unit) {
    val dlt = if (b.revenueYesterday != 0L) Math.round((b.revenueToday - b.revenueYesterday).toDouble() / b.revenueYesterday * 1000) / 10.0 else null
    AppCard(Modifier.then(Modifier), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().let { m -> m }) {
            Column(Modifier.weight(1f)) {
                Text(b.label, style = MaterialTheme.typography.titleSmall)
                Text(b.manager, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) { Text(Fmt.money(b.revenueToday), style = MaterialTheme.typography.labelLarge); DeltaBadge(dlt) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BarLine(b.health, Modifier.weight(1f)); Text("${b.health}%", style = MaterialTheme.typography.bodySmall)
            Badge("FOM · " + (b.fom?.lag ?: b.fom?.status ?: "—"), when (b.fom?.status) { "ok" -> Tone.Accent; "down" -> Tone.Danger; else -> Tone.Warning })
            Text("${b.camerasOnline}/${b.cameras} kam.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onClick, contentPadding = PaddingValues(0.dp)) { Text("Batafsil →") }
    }
}

@Composable
fun QuickLinks(role: String, go: (String, String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("cash", "cameras", "approvals", "notifications", "employees", "audit", "ai", "schedule", "attendance", "profile").filter { Access.can(it, role) }.forEach { id ->
            val it = Access.byId[id] ?: return@forEach
            AppCard(padding = PaddingValues(0.dp)) { ListRow(it.label, it.group, leading = { androidx.compose.material3.Icon(it.icon, null, tint = MaterialTheme.colorScheme.primary) }, chevron = true, onClick = { go(id, null) }) }
        }
    }
}

/** Desktop `linkTo`: web yo'lni ekran id'siga. */
fun linkTo(path: String, go: (String, String?) -> Unit) {
    Regex("^/branches/([^/]+)").find(path)?.let { go("branches", it.groupValues[1]); return }
    val map = mapOf("/dashboard" to "dashboard", "/branches" to "branches", "/sroklar" to "expiry", "/cash" to "cash", "/expenses" to "expenses", "/cashflow" to "cashflow", "/employees" to "employees", "/schedule" to "schedule", "/attendance" to "attendance", "/live" to "live", "/payroll" to "payroll", "/payroll/rules" to "payrollRules", "/suppliers" to "suppliers", "/tolovlar" to "payables", "/doctors" to "doctors", "/ai" to "ai", "/videowall" to "videowall", "/cameras" to "cameras", "/pnl" to "pnl", "/analytics" to "analytics", "/forecast" to "forecast", "/reports" to "reports", "/soliq" to "tax", "/approvals" to "approvals", "/notifications" to "notifications", "/audit" to "audit", "/fom" to "fom", "/health" to "health", "/users" to "users", "/roles" to "roles", "/modules" to "modules", "/ai-settings" to "aiSettings", "/settings" to "settings")
    map[path.substringBefore('?')]?.let { go(it, null) }
}
