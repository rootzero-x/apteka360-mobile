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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.Branch
import uz.apteka360.app.data.Me
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.BarLine
import uz.apteka360.app.ui.components.Bars
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.KpiCard
import uz.apteka360.app.ui.components.KpiMoney
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.nav.Access

/** Filiallar ro'yxati (desktop master) → tafsilot (detail) alohida ekranda. */
@Composable
fun BranchesScreen(c: AppContainer, go: (String, String?) -> Unit) {
    val loader = rememberLoader { c.branchesCached(true) }
    ScreenScaffold("Filiallar", loader.data?.let { "${it.size} ta filial" }) {
        DataBox(loader) { br ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(br, key = { it.id }) { b ->
                    AppCard(padding = PaddingValues(0.dp)) {
                        ListRow(b.label, "${b.manager} · ${b.address}", leading = { HealthDot(b.health) },
                            trailing = { Column(horizontalAlignment = Alignment.End) { Text(Fmt.moneyShort(b.revenueToday).first + " mln", style = MaterialTheme.typography.labelLarge); Text("${b.health}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                            chevron = true, onClick = { go("branch", b.id) })
                    }
                }
                if (br.isEmpty()) item { AppCard { Text("Filial yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
    }
}

@Composable
fun HealthDot(health: Int) {
    val tone = if (health >= 80) Tone.Accent else if (health >= 60) Tone.Warning else Tone.Danger
    Badge("●", tone)
}

@Composable
fun BranchDetailScreen(c: AppContainer, me: Me, id: String, go: (String, String?) -> Unit, onBack: () -> Unit) {
    val loader = rememberLoader(id) { c.branchesCached().firstOrNull { it.id == id } ?: c.branchesCached(true).firstOrNull { it.id == id } ?: throw uz.apteka360.app.data.ApiException(404, "not_found", "Filial topilmadi yoki uni ko‘rishga ruxsat yo‘q") }
    ScreenScaffold(loader.data?.name ?: "Filial", loader.data?.let { "${it.address} · ${it.hours}" }, onBack = onBack) {
        DataBox(loader) { b -> BranchDetail(c, me, b, go) }
    }
}

@Composable
private fun BranchDetail(c: AppContainer, me: Me, b: Branch, go: (String, String?) -> Unit) {
    val dlt = if (b.revenueYesterday != 0L) Math.round((b.revenueToday - b.revenueYesterday).toDouble() / b.revenueYesterday * 1000) / 10.0 else null
    val expPct = if (b.expenseLimit > 0) (b.expensesToday * 100 / b.expenseLimit).toInt() else 0
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge(when (b.closing) { "open" -> "Kassa ochiq"; "closed" -> "Kassa yopilgan"; else -> "Kassa: ${b.closing}" }, when (b.closing) { "open" -> Tone.Info; "closed" -> Tone.Accent; else -> Tone.Warning })
            Badge("FOM · " + (b.fom?.lag ?: b.fom?.status ?: "—"), when (b.fom?.status) { "ok" -> Tone.Accent; "down" -> Tone.Danger; else -> Tone.Warning })
            Badge("Edge · " + (if (b.edge == "online") "onlayn" else "oflayn"), if (b.edge == "online") Tone.Accent else Tone.Danger)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("Bugungi tushum", b.revenueToday, Modifier.weight(1f), delta = dlt, foot = "kecha: " + Fmt.moneyShortText(b.revenueYesterday)); KpiMoney("Yalpi foyda", b.grossToday, Modifier.weight(1f), foot = "marja ${b.margin}%") } }
        item { AppCard(padding = PaddingValues(14.dp)) { Text("Xarajatlar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(Fmt.moneyShortText(b.expensesToday), style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(6.dp)); BarLine(expPct, tone = if (expPct > 100) Tone.Danger else if (expPct > 80) Tone.Warning else Tone.Accent); Text("$expPct% · limit ${Fmt.moneyShort(b.expenseLimit).first} mln", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Smenada", "${b.onShift} / ${b.employees}", "xodim", Modifier.weight(1f), foot = "${b.desks} ta kassa"); KpiCard("Kameralar", "${b.camerasOnline} / ${b.cameras}", "onlayn", Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Tashrif", Fmt.num(b.traffic), "kishi", Modifier.weight(1f)); KpiCard("Konversiya", "${b.conversion}%", "", Modifier.weight(1f)); KpiCard("Navbat", "${b.queue}", "kishi", Modifier.weight(1f)) } }
        sectionTitle("Tushum · 30 kun (mln so‘m)")
        item { AppCard { Bars(b.revenue30) } }
        sectionTitle("Ma’lumotlar")
        item { AppCard { KeyValueRow("Mudir", b.manager); KeyValueRow("Manzil", b.address); KeyValueRow("Ish vaqti", b.hours); KeyValueRow("FOM oxirgi sinx.", b.fom?.let { "${it.last ?: "—"} · ${it.lag ?: ""}" } ?: "—"); KeyValueRow("Bugungi AI hodisalar", b.aiToday?.toString() ?: "—") } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (Access.can("cash", me.role)) OutlinedButton({ go("cash", b.id) }, Modifier.weight(1f)) { Text("Kassa") }
            if (Access.can("cameras", me.role)) OutlinedButton({ go("cameras", b.id) }, Modifier.weight(1f)) { Text("Kameralar") }
            if (Access.can("employees", me.role)) OutlinedButton({ go("employees", b.id) }, Modifier.weight(1f)) { Text("Xodimlar") }
        } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
