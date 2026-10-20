package uz.apteka360.app.ui.screens

import uz.apteka360.app.ui.components.findFragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.Branch
import uz.apteka360.app.data.Expense
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.ReportJob
import uz.apteka360.app.data.Settings
import uz.apteka360.app.data.arr
import uz.apteka360.app.data.bool
import uz.apteka360.app.data.child
import uz.apteka360.app.data.dbl
import uz.apteka360.app.data.list
import uz.apteka360.app.data.long
import uz.apteka360.app.data.str
import uz.apteka360.app.data.text
import uz.apteka360.app.data.txt
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.BarLine
import uz.apteka360.app.ui.components.ConfirmDialog
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.Field
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.FormSheet
import uz.apteka360.app.ui.components.HBars
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.KpiCard
import uz.apteka360.app.ui.components.KpiMoney
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.MoneyField
import uz.apteka360.app.ui.components.PickField
import uz.apteka360.app.ui.components.SearchField
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.Sparkline
import uz.apteka360.app.ui.components.SwitchRow
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger
import uz.apteka360.app.ui.theme.Warning

private fun isNet(role: String) = role in listOf("OWNER", "SUPER_ADMIN", "FINANCE", "AUDITOR")
fun mln(n: Long?): String = if (n == null) "—" else (if (n < 0) "−" else "") + String.format(java.util.Locale.US, "%.1f", kotlin.math.abs(n) / 1e6).replace('.', ',') + " mln"
fun mln(n: Double?): String = mln(n?.let { Math.round(it) })
private fun qs(vararg p: Pair<String, String?>): String { val l = p.filter { !it.second.isNullOrBlank() }; return if (l.isEmpty()) "" else "?" + l.joinToString("&") { "${it.first}=${java.net.URLEncoder.encode(it.second, "UTF-8")}" } }
private fun shortDate(s: String?): String { val d = Fmt.parse(s) ?: return s ?: "—"; val c = java.util.Calendar.getInstance().apply { time = d }; val M = listOf("yan", "fev", "mar", "apr", "may", "iyn", "iyl", "avg", "sen", "okt", "noy", "dek"); return "${c.get(java.util.Calendar.DAY_OF_MONTH)} ${M[c.get(java.util.Calendar.MONTH)]}" }

@Composable
fun BranchFilter(c: AppContainer, role: String, value: String, onChange: (String) -> Unit) { if (isNet(role)) Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, value, onChange) }

@Composable
fun NotConnected(text: String = "Tushum, tannarx va prognoz FOM sinxronlash agenti ulangandan keyin to‘ldiriladi.") { InfoBar("FOM ulanmagan.", text) }

@Composable
fun DateRange(from: String, to: String, onChange: (String, String) -> Unit) {
    var f by remember(from) { mutableStateOf(from) }; var t by remember(to) { mutableStateOf(to) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.OutlinedTextField(f, { f = it }, label = { Text("Dan (yyyy-mm-dd)") }, singleLine = true, modifier = Modifier.weight(1f))
        androidx.compose.material3.OutlinedTextField(t, { t = it }, label = { Text("Gacha") }, singleLine = true, modifier = Modifier.weight(1f))
        TextButton(onClick = { if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(f) && Regex("\\d{4}-\\d{2}-\\d{2}").matches(t)) onChange(f, t) }) { Text("OK") }
    }
}

// ═════════════ Harajatlar ═════════════
private val SOURCES = listOf("Kassa (naqd)", "Bank hisobi", "Egasi kassasi")
private val CATS = listOf("Xodimlar" to "tushlik, forma, o‘qitish", "Kommunal" to "elektr, suv, gaz, ijara", "IT" to "internet, uskunalar, dastur", "Logistika" to "yetkazish, taksi, yoqilg‘i", "Davlat / bank" to "soliq, komissiya, litsenziya", "Tijorat" to "reklama, vitrina, yorliq", "Vrachlar" to "kelishuv to‘lovlari · maxfiy", "Boshqa" to "izoh majburiy")
private fun expSt(s: String) = when (s) { "approved" -> "Tasdiqlangan" to Tone.Accent; "pending" -> "Mudir tasdig‘i" to Tone.Warning; "finance" -> "Moliya tasdig‘i" to Tone.Warning; "owner" -> "Egasi tasdig‘i" to Tone.Warning; "rejected" -> "Rad etilgan" to Tone.Danger; "cancelled" -> "Bekor qilingan" to Tone.Neutral; else -> s to Tone.Neutral }
private fun canWriteExpenses(role: String) = role in listOf("OWNER", "SUPER_ADMIN", "FINANCE", "MANAGER") && !Access.readOnly(role)
private fun approvalLevel(amt: Long, remain: Long, st: Settings): String {
    val x = st.approvalX.takeIf { it > 0 } ?: 2_000_000; val y = st.approvalY.takeIf { it > 0 } ?: 10_000_000
    return when { amt > remain -> "Egasi — kunlik limitdan oshadi"; amt > y -> "Egasi (> 10 mln)"; amt > x -> "Moliya admin (2–10 mln)"; else -> "Filial mudiri (≤ 2 mln)" }
}
data class ExpData(val list: List<Expense>, val branches: List<Branch>, val settings: Settings)

@Composable
fun ExpensesScreen(c: AppContainer, me: Me, initialBranch: String?) {
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { ExpData(c.repo.expenses(), c.branchesCached(true), runCatching { c.repo.settings() }.getOrDefault(Settings())) }
    var filter by rememberSaveable { mutableStateOf("all") }; var bf by rememberSaveable { mutableStateOf(initialBranch?.takeIf { it != "new" } ?: "") }; var q by rememberSaveable { mutableStateOf("") }
    var addOpen by rememberSaveable { mutableStateOf(initialBranch == "new" && canWriteExpenses(me.role)) }; var detail by remember { mutableStateOf<Expense?>(null) }
    val own = if (me.role == "MANAGER") me.branchId else null
    val scopeIds = own?.let { listOf(it) } ?: (if (me.branchIds.isNotEmpty() && !isNet(me.role)) me.branchIds else null)
    fun inScope(id: String) = scopeIds == null || id in scopeIds
    ScreenScaffold("Harajatlar", fab = if (canWriteExpenses(me.role)) Icons.Default.Add to { addOpen = true } else null) {
        DataBox(loader) { d ->
            val myBranches = d.branches.filter { inScope(it.id) }
            val rows = d.list.filter { inScope(it.branchId) }.filter { bf.isBlank() || it.branchId == bf }
                .filter { when (filter) { "all" -> true; "wait" -> it.status in listOf("pending", "finance", "owner"); else -> it.status == filter } }
                .filter { q.isBlank() || listOf(it.title, it.who, it.cat, it.source).joinToString(" ").contains(q, ignoreCase = true) }
            val sum = rows.filter { it.status != "cancelled" && it.status != "rejected" }.sumOf { it.amount }
            val st = d.settings
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SearchField(q, { q = it }, "Tavsif, kiritgan, kategoriya") }
                item { BranchFilter(c, me.role, bf) { bf = it } }
                item { Seg(listOf("all" to "Hammasi", "wait" to "Tasdiq kutilmoqda", "approved" to "Tasdiqlangan", "rejected" to "Rad etilgan", "cancelled" to "Bekor qilingan"), filter, { filter = it }) }
                item { Text("${rows.size} ta yozuv · ko‘rsatilgan jami: ${Fmt.money(sum)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(myBranches, key = { "lim-" + it.id }) { b -> val p = if (b.expenseLimit > 0) b.expensesToday.toDouble() / b.expenseLimit else 0.0
                    AppCard(padding = PaddingValues(12.dp)) { Text("${b.label} · bugungi limit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${Fmt.num(b.expensesToday)} / ${Fmt.num(b.expenseLimit)}", fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(6.dp)); BarLine((p * 100).toInt(), tone = if (p > 1) Tone.Danger else if (p > 0.8) Tone.Warning else Tone.Accent) } }
                if (rows.isEmpty()) item { AppCard { Text("Bu filtrda harajat yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(rows, key = { it.id }) { x ->
                    val (l, t) = expSt(x.status); val cancelled = x.status == "cancelled"
                    AppCard(Modifier.clickable { detail = x }, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(x.title.ifBlank { "—" }, style = MaterialTheme.typography.titleSmall, textDecoration = if (cancelled) TextDecoration.LineThrough else null); Text("${x.date} · ${c.branchName(x.branchId)} · ${x.cat}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Column(horizontalAlignment = Alignment.End) { Text(Fmt.num(x.amount), fontWeight = FontWeight.SemiBold, textDecoration = if (cancelled) TextDecoration.LineThrough else null); Badge(l, t) }
                        }
                        Text("${x.who} · ${x.source} · ${if (x.receipt) "chek ✓" else "cheksiz"}", style = MaterialTheme.typography.bodySmall, color = if (x.receipt) MaterialTheme.colorScheme.onSurfaceVariant else Warning, modifier = Modifier.padding(top = 4.dp))
                        if (x.anomaly.isNotBlank()) Text(x.anomaly, style = MaterialTheme.typography.bodySmall, color = Warning)
                    }
                }
                item { Text("Tasdiqlash: ≤ ${Fmt.moneyShort(st.approvalX.takeIf { it > 0 } ?: 2_000_000).first} mln — mudir, ${Fmt.moneyShort(st.approvalX.takeIf { it > 0 } ?: 2_000_000).first}–${Fmt.moneyShort(st.approvalY.takeIf { it > 0 } ?: 10_000_000).first} mln — moliya, undan yuqori yoki limitdan oshsa — Egasi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { Spacer(Modifier.height(72.dp)) }
            }
            if (addOpen) NewExpenseSheet(c, myBranches, st, bf.ifBlank { own ?: myBranches.firstOrNull()?.id ?: "" }, onDismiss = { addOpen = false }, onSaved = { addOpen = false; reloadKey++ })
            detail?.let { x -> ExpenseDetailSheet(c, me, x, onDismiss = { detail = null }, onChanged = { detail = null; reloadKey++ }) }
        }
    }
}

@Composable
private fun NewExpenseSheet(c: AppContainer, branches: List<Branch>, st: Settings, defBranch: String, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val act = androidx.compose.ui.platform.LocalContext.current.findFragmentActivity()
    var cat by remember { mutableStateOf<String?>(null) }; var amount by remember { mutableStateOf("") }; var source by remember { mutableStateOf(SOURCES[0]) }
    var branch by remember { mutableStateOf(branches.firstOrNull { it.id == defBranch } ?: branches.firstOrNull()) }
    var receipt by remember { mutableStateOf(false) }; var recurring by remember { mutableStateOf(false) }; var note by remember { mutableStateOf("") }
    val a = amount.toLongOrNull() ?: 0; val b = branch; val remain = (b?.expenseLimit ?: 0) - (b?.expensesToday ?: 0)
    FormSheet("Yangi harajat", primary = "Saqlash va yuborish", onDismiss = onDismiss, onSubmit = {
        val ct = cat ?: return@FormSheet "Kategoriyani tanlang."
        if (a <= 0) return@FormSheet "Summani kiriting."
        val bb = b ?: return@FormSheet "Filial yo‘q."
        if (ct == "Boshqa" && note.isBlank()) return@FormSheet "«Boshqa» kategoriyasi uchun izoh majburiy"
        if (a > uz.apteka360.app.ui.components.BIOMETRIC_THRESHOLD) uz.apteka360.app.ui.components.Biometric.confirm(act, "Katta harajatni tasdiqlang", Fmt.money(a), required = true)?.let { return@FormSheet it }
        c.repo.cmd("expenses.create", buildJsonObject { put("branchId", bb.id); put("cat", ct); put("amount", a); put("source", source); put("receipt", receipt); put("note", note.trim()); put("recurring", recurring) }); onSaved(); null
    }) {
        Text("Kategoriyani tanlang", style = MaterialTheme.typography.labelLarge)
        CATS.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 3.dp)) { row.forEach { (k, d) -> FilterChip(selected = cat == k, onClick = { cat = k }, label = { Column { Text(k); Text(d, style = MaterialTheme.typography.labelMedium) } }, modifier = Modifier.weight(1f)) } } }
        Spacer(Modifier.height(8.dp))
        MoneyField("Summa *", amount, { amount = it })
        if (branches.size > 1) PickField("Filial", branches, branch, { it.name }, { branch = it })
        Text("Bugungi limit: ${Fmt.num(b?.expensesToday ?: 0)} / ${Fmt.num(b?.expenseLimit ?: 0)} · qoldi ${Fmt.num(maxOf(0, remain))} so‘m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (a > 0 && a > remain) InfoBar("Kunlik limitdan oshadi", "Kunlik limitdan ${Fmt.num(a - remain)} so‘m oshadi. Harajat saqlanadi, lekin to‘lov Egasi tasdig‘idan keyin amalga oshiriladi.", Tone.Warning)
        Spacer(Modifier.height(8.dp)); Text("Pul manbasi", style = MaterialTheme.typography.labelLarge); Seg(SOURCES.map { it to it }, source, { source = it })
        Spacer(Modifier.height(8.dp)); Field("Izoh", note, { note = it }, minLines = 2, hint = "Tavsif: nima uchun, kimga")
        SwitchRow("Chek ilova qilingan", receipt, { receipt = it }); SwitchRow("Har oy takrorlanadigan harajat", recurring, { recurring = it })
        Text("Tasdiqlovchi: ${approvalLevel(a, remain, st)}" + (cat?.let { " · Kategoriya: $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExpenseDetailSheet(c: AppContainer, me: Me, x: Expense, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val (l, t) = expSt(x.status)
    val canCancel = canWriteExpenses(me.role) && x.status != "cancelled" && x.status != "rejected"
    var reason by remember { mutableStateOf("") }
    FormSheet(x.title.ifBlank { "Harajat" }, primary = if (canCancel) "Bekor qilish" else "Yopish", danger = canCancel, onDismiss = onDismiss, onSubmit = {
        if (!canCancel) { onDismiss(); return@FormSheet null }
        if (reason.isBlank()) return@FormSheet "Bekor qilish sababini yozing."
        c.repo.cmd("expenses.cancel", buildJsonObject { put("id", x.id); put("reason", reason.trim()) }); onChanged(); null
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(Fmt.money(x.amount), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f)); Badge(l, t) }
        if (x.anomaly.isNotBlank()) InfoBar("Anomaliya (AI taxmini)", "${x.anomaly} — shu kategoriyadagi oxirgi 90 kunga nisbatan.", Tone.Warning)
        KeyValueRow("Sana", x.createdAt?.let { Fmt.dateTime(it) } ?: x.date); KeyValueRow("Filial", c.branchName(x.branchId)); KeyValueRow("Kategoriya", x.cat); KeyValueRow("Manba", x.source); KeyValueRow("Kiritdi", x.who)
        KeyValueRow("Chek", if (x.receipt) "Rasm ilova qilingan · OCR mos" else "Ilova qilinmagan"); if (x.recurring) KeyValueRow("Takrorlanadi", "Har oy")
        if (x.note.isNotBlank()) Text((if (x.status == "cancelled") "Bekor qilish sababi: " else "Izoh: ") + x.note, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp))
        if (canCancel) { HorizontalDivider(Modifier.padding(vertical = 8.dp)); Field("Bekor qilish sababi *", reason, { reason = it }, hint = "Moliyaviy yozuv o‘chirilmaydi — «Bekor qilingan» holatida audit bilan saqlanadi.") }
    }
}

// ═════════════ Pul oqimi ═════════════
@Composable
fun CashflowScreen(c: AppContainer, me: Me) {
    var hz by rememberSaveable { mutableStateOf(30) }; var bf by rememberSaveable { mutableStateOf("") }
    val loader = rememberLoader(bf) { c.branchesCached(); c.api.getJson("/v1/finance/cashflow" + qs("branchId" to bf)) }
    ScreenScaffold("Pul oqimi", loader.data?.let { d -> "Joriy qoldiq: " + Fmt.money(d.list("accounts").sumOf { it.long("balance") ?: 0 }) }) {
        DataBox(loader) { d ->
            val acc = d.list("accounts"); val exp = d.list("expected"); val rw = d.child("runway"); val total = acc.sumOf { it.long("balance") ?: 0 }
            val v = when (hz) { 90 -> rw.long("d90"); 60 -> rw.long("d60"); else -> rw.long("d30") } ?: 0
            val risk = d.child("risk")
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Seg(listOf("30" to "30 kun", "60" to "60 kun", "90" to "90 kun"), "$hz", { hz = it.toInt() }) }
                item { BranchFilter(c, me.role, bf) { bf = it } }
                if (d.bool("connected") != true) item { NotConnected("Kassa va terminal qoldig‘i hamda tushum prognozi FOM agenti ulangandan keyin to‘liq bo‘ladi.") }
                items(acc.chunked(2)) { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { a -> KpiMoney(a.str("label") ?: "", a.long("balance"), Modifier.weight(1f), foot = a.str("asOf")?.let { "holat: " + Fmt.dateTime(it) }) }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
                sectionTitle("Kassa runway — prognoz qilingan qoldiq")
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("30 kundan keyin", rw.long("d30"), Modifier.weight(1f)); if (hz >= 60) KpiMoney("60 kundan keyin", rw.long("d60"), Modifier.weight(1f)) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { if (hz >= 90) KpiMoney("90 kundan keyin", rw.long("d90"), Modifier.weight(1f)); rw.child("lowest")?.let { KpiMoney("Eng past nuqta", it.long("value"), Modifier.weight(1f), foot = Fmt.date(it.str("date"))) } } }
                item { val pts = listOfNotNull(total, rw.long("d30"), if (hz >= 60) rw.long("d60") else null, if (hz >= 90) rw.long("d90") else null); AppCard { Text("Qoldiq dinamikasi · $hz kun", style = MaterialTheme.typography.titleSmall); Sparkline(pts, Modifier.fillMaxWidth().height(80.dp)) } }
                item { if (risk != null) InfoBar("Prognoz · ogohlantirish", risk.str("text") ?: "", if (risk.str("level") == "crit") Tone.Danger else Tone.Warning) else if (v < 0) InfoBar("Prognoz · ogohlantirish", "$hz kun ichida kassa uzilishi xavfi: ${Fmt.money(v)}.", Tone.Danger) else InfoBar("Prognoz", "$hz kun ichida kassa uzilishi xavfi yo‘q.", Tone.Accent) }
                val limit = Fmt.isoDay(-hz); val rows = exp.filter { (it.str("date") ?: "") <= limit }
                val inSum = rows.filter { it.str("kind") == "in" }.sumOf { it.long("amount") ?: 0 }; val outSum = rows.filter { it.str("kind") != "in" }.sumOf { it.long("amount") ?: 0 }
                sectionTitle("Kutilayotgan kirim va chiqimlar")
                item { Text("kirim +${Fmt.moneyShortText(inSum)} · chiqim −${Fmt.moneyShortText(outSum)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (rows.isEmpty()) item { AppCard { Text("Kutilayotgan to‘lovlar yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(rows) { x -> val inn = x.str("kind") == "in"; AppCard(padding = PaddingValues(0.dp)) { ListRow(x.str("title") ?: "", "${shortDate(x.str("date"))} · ${if (inn) "Kirim" else "Chiqim"}" + (x.str("source")?.let { " · $it" } ?: ""), trailing = { Text((if (inn) "+" else "−") + Fmt.num(x.long("amount")), fontWeight = FontWeight.SemiBold, color = if (inn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface) }) } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═════════════ P&L ═════════════
@Composable
fun PnlScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit) {
    var from by rememberSaveable { mutableStateOf(Fmt.isoDay().take(8) + "01") }; var to by rememberSaveable { mutableStateOf(Fmt.isoDay()) }
    var gb by rememberSaveable { mutableStateOf("branch") }; var bf by rememberSaveable { mutableStateOf("") }
    val loader = rememberLoader(from, to, gb, bf) { c.branchesCached(); c.api.getJson("/v1/finance/pnl" + qs("from" to from, "to" to to, "groupBy" to gb, "branchId" to bf)) }
    ScreenScaffold("Foyda va zarar (P&L)", loader.data?.let { "${if (bf.isBlank()) "Barcha filiallar" else c.branchName(bf)} · ${Fmt.date(it.str("from"))} – ${Fmt.date(it.str("to"))}" }) {
        DataBox(loader) { d ->
            val cols = d.list("columns").map { it.text }; val rows = d.list("rows"); val connected = d.bool("connected") == true
            val rev = rows.firstOrNull { it.str("key") == "revenue" }
            val FOMDEP = setOf("revenue", "cogs", "gross", "net")
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { DateRange(from, to) { f, t -> from = f; to = t } }
                item { Seg(listOf("branch" to "Filiallar", "month" to "Oylar"), gb, { gb = it }) }
                item { BranchFilter(c, me.role, bf) { bf = it } }
                if (!connected) item { NotConnected("Tushum va tannarx FOM agenti ulangach to‘ladi; harajat va oylik fondi — Apteka 360 hisobidan.") }
                items(rows) { r ->
                    val kind = r.str("kind"); val strong = kind == "sub" || kind == "total"; val noFom = !connected && r.str("key") in FOMDEP
                    val vals = r.list("values").map { it.long("") ?: runCatching { (it as kotlinx.serialization.json.JsonPrimitive).content.toDouble().toLong() }.getOrNull() ?: 0L }
                    val tot = vals.lastOrNull() ?: 0; val rt = rev?.list("values")?.lastOrNull()?.let { runCatching { (it as kotlinx.serialization.json.JsonPrimitive).content.toDouble().toLong() }.getOrNull() } ?: 0
                    AppCard(padding = PaddingValues(12.dp)) {
                        Row { Text(r.str("label") ?: "", fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f)); Text(if (noFom) "—" else mln(tot), fontWeight = FontWeight.SemiBold, color = if (kind == "total") (if (tot < 0) Danger else MaterialTheme.colorScheme.primary) else MaterialTheme.colorScheme.onSurface); if (strong && rt != 0L) Text("  ${Math.round(tot.toDouble() / rt * 1000) / 10.0}%".replace('.', ','), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (cols.size > 1 && !noFom) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { cols.dropLast(1).forEachIndexed { i, cn -> Column { Text(cn, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1); Text(mln(vals.getOrNull(i)), style = MaterialTheme.typography.bodySmall) } } }
                    }
                }
                val net = rows.firstOrNull { it.str("key") == "net" }
                if (net != null && cols.size > 1 && connected) { sectionTitle("Sof foyda · ${if (gb == "month") "oylar" else "filiallar"} kesimida"); item { AppCard { HBars(cols.dropLast(1).mapIndexed { i, cn -> cn to (net.list("values").getOrNull(i)?.let { runCatching { (it as kotlinx.serialization.json.JsonPrimitive).content.toDouble().toLong() }.getOrNull() } ?: 0L) }, ::mln) } } }
                item { OutlinedButton({ go("reports", "pnl") }, Modifier.fillMaxWidth()) { Text("Eksport (Hisobotlar)") } }
                item { Text("Oylik fondi davr kunlariga mutanosib (fond × kun / 30). Harajatlar — rad etilgan va bekor qilinganlardan tashqari.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

private fun JsonElement?.numAny(): Long? = (this as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()?.let { Math.round(it) }

// ═════════════ Prognoz ═════════════
@Composable
fun ForecastScreen(c: AppContainer, me: Me) {
    var hz by rememberSaveable { mutableStateOf(14) }; var bf by rememberSaveable { mutableStateOf("") }
    val loader = rememberLoader(hz, bf) { c.branchesCached(); c.api.getJson("/v1/finance/forecast" + qs("horizon" to "$hz", "branchId" to bf)) }
    ScreenScaffold("Prognozlar", "So‘nggi 28 kunlik FOM tushumi · ishonch oralig‘i 80%") {
        DataBox(loader) { d ->
            val s = d.list("series"); val connected = d.bool("connected") == true; val sum = s.sumOf { it.long("value") ?: 0 }
            val RISK = mapOf("ok" to ("yetarli" to Tone.Accent), "overstock" to ("ortiqcha zaxira" to Tone.Warning), "unknown_stock" to ("qoldiq noma’lum" to Tone.Neutral))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Seg(listOf("7" to "7 kun", "14" to "14 kun", "30" to "30 kun"), "$hz", { hz = it.toInt() }) }
                item { BranchFilter(c, me.role, bf) { bf = it } }
                if (!connected) item { NotConnected() } else d.list("notes").forEach { n -> item { InfoBar("Izoh", n.text) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("Kutilgan tushum · $hz kun", sum, Modifier.weight(1f)); KpiMoney("Kunlik o‘rtacha", if (s.isNotEmpty()) sum / s.size else 0, Modifier.weight(1f)) } }
                item { KpiMoney("Pessimistik (80%)", s.sumOf { it.long("low80") ?: 0 }) }
                item { AppCard { Text("Tushum prognozi · $hz kun", style = MaterialTheme.typography.titleSmall); if (s.isEmpty()) Text(if (connected) "Prognoz uchun ma’lumot yetarli emas" else "FOM ulanmagan — prognoz uchun ma’lumot yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant) else Sparkline(s.map { it.long("value") ?: 0 }, Modifier.fillMaxWidth().height(90.dp)) } }
                if (s.isNotEmpty()) { sectionTitle("Kunlar bo‘yicha"); items(s) { x -> AppCard(padding = PaddingValues(0.dp)) { ListRow(Fmt.date(x.str("date")), "80%: ${Fmt.num(x.long("low80"))} – ${Fmt.num(x.long("high80"))}", trailing = { Text(Fmt.num(x.long("value")), fontWeight = FontWeight.SemiBold) }) } } }
                sectionTitle("Talab prognozi · $hz kun")
                val dem = d.list("demand")
                if (dem.isEmpty()) item { AppCard { Text("Talab prognozi FOM sotuvlari ulangach ko‘rinadi", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(dem) { x -> val k = RISK[x.str("risk")] ?: ((x.str("risk") ?: "") to Tone.Neutral); AppCard(padding = PaddingValues(0.dp)) { ListRow(x.str("name") ?: "", "${x.str("sku") ?: ""} · ${c.branchName(x.str("branchId"))} · kutilgan ${Fmt.num(x.long("expected"))} · qoldiq ${Fmt.num(x.long("stock"))}", trailing = { Badge(k.first, k.second) }) } }
                item { InfoBar("AI tavsiyasi", "Prognoz — tavsiya. Buyurtma va to‘lov bo‘yicha qarorni mas’ul xodim qabul qiladi.") }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═════════════ Analitika ═════════════
private val AI_CODES = mapOf("IDLE_PHONE" to "Telefon ishlatish", "OUT_OF_ZONE" to "Ish zonasidan tashqarida", "SITTING" to "O‘tirish", "LYING" to "Yotish", "ABSENT_POST" to "Postda yo‘q", "CASH_NO_RECEIPT" to "Chek urilmagan savdo", "RUDE_SPEECH" to "Qo‘pol ibora (audio)", "QUEUE_LONG" to "Uzun navbat")

@Composable
fun AnalyticsScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit) {
    var tab by rememberSaveable { mutableStateOf("filial") }; var bf by rememberSaveable { mutableStateOf("") }
    var from by rememberSaveable { mutableStateOf(Fmt.isoDay(30)) }; var to by rememberSaveable { mutableStateOf(Fmt.isoDay()) }
    val path = mapOf("filial" to "/v1/analytics/branches", "xodim" to "/v1/analytics/staff", "mijoz" to "/v1/analytics/traffic", "ai" to "/v1/analytics/ai")[tab]!!
    val loader = rememberLoader(tab, bf, from, to) { c.branchesCached(); c.api.getJson(path + qs("from" to from, "to" to to, "branchId" to bf)) }
    ScreenScaffold("Analitika", loader.data?.let { "${Fmt.date(it.str("from"))} – ${Fmt.date(it.str("to"))}" }) {
        DataBox(loader) { d ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Seg(listOf("filial" to "Filiallar", "xodim" to "Xodimlar", "mijoz" to "Mijoz oqimi", "ai" to "AI analitika"), tab, { tab = it }) }
                item { DateRange(from, to) { f, t -> from = f; to = t } }
                item { BranchFilter(c, me.role, bf) { bf = it } }
                when (tab) {
                    "filial" -> {
                        val bs = d.list("branches")
                        if (d.bool("connected") != true) item { NotConnected("Tushum, cheklar va marja FOM agenti ulangach to‘ladi.") }
                        items(bs) { x -> val con = x.bool("connected") == true; val h = x.long("health") ?: 0
                            AppCard(Modifier.clickable { x.str("branchId")?.let { go("branch", it) } }, padding = PaddingValues(12.dp)) {
                                Row { Text(x.str("name") ?: "", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Text("$h", fontWeight = FontWeight.Bold, color = if (h >= 80) MaterialTheme.colorScheme.primary else if (h >= 60) Warning else Danger) }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) { listOf("Tushum" to (if (con) mln(x.long("revenue")) else "—"), "Cheklar" to (if (con) Fmt.num(x.long("checks")) else "—"), "O‘rt. chek" to (if (con) Fmt.num(x.long("avgCheck")) else "—"), "Marja" to (if (con) "${x.txt("marginPct")}%" else "—"), "Tashrif" to Fmt.num(x.long("traffic")), "Konv." to (x.dbl("conversion")?.let { "$it%" } ?: "—")).forEach { (l, v) -> Column { Text(l, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v, style = MaterialTheme.typography.bodySmall) } } }
                            } }
                        if (bs.any { it.bool("connected") == true }) { sectionTitle("Tushum taqqoslash"); item { AppCard { HBars(bs.map { (it.str("name") ?: "") to (it.long("revenue") ?: 0) }, ::mln) } } }
                    }
                    "xodim" -> {
                        val st = d.list("staff").sortedByDescending { it.long("kpi") ?: 0 }; val mx = (st.maxOfOrNull { it.long("kpi") ?: 0 } ?: 1).coerceAtLeast(1)
                        if (d.bool("salaryMasked") == true) item { InfoBar("Oylik ma’lumotlari yashirilgan.", "Rolingiz uchun KPI summalari ko‘rsatilmaydi.") }
                        if (st.isEmpty()) item { AppCard { Text("Xodimlar yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        items(st.withIndex().toList()) { (i, e) -> AppCard(padding = PaddingValues(12.dp)) {
                            Row { Text("${i + 1}. ${e.str("name")}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Text(e.long("kpi")?.let { Fmt.num(it) } ?: "—", fontWeight = FontWeight.SemiBold) }
                            Text("${e.str("position") ?: ""} · ${c.branchName(e.str("branchId"))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            e.long("kpi")?.let { BarLine((it * 100 / mx).toInt(), Modifier.fillMaxWidth().padding(vertical = 6.dp), Tone.Accent) }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { listOf("Sotuv" to (e.long("sales")?.let(::mln) ?: "—"), "Davomat" to (e.dbl("attendance")?.let { "${it.toInt()}%" } ?: "—"), "Idle" to (e.dbl("idlePct")?.let { "$it%" } ?: "—"), "AI" to "${e.long("aiEvents") ?: 0}" + (e.long("aiConfirmed")?.takeIf { it > 0 }?.let { " ($it tasdiq)" } ?: ""), "Jarima" to (e.long("penalties")?.takeIf { it > 0 }?.let(Fmt::num) ?: "—")).forEach { (l, v) -> Column { Text(l, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v, style = MaterialTheme.typography.bodySmall) } } }
                        } }
                        item { Text("AI bahosi taxmin hisoblanadi va jarimaga bevosita ta’sir qilmaydi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    "mijoz" -> {
                        d.list("notes").forEach { n -> item { InfoBar("Izoh", n.text) } }
                        val hm = d.list("heatmap")
                        item { AppCard { Text("Mijoz oqimi: hafta kuni × soat", style = MaterialTheme.typography.titleSmall)
                            if (hm.isEmpty()) Text("Soatbay tashrif Edge sanash ulanganda ko‘rinadi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else { val days = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya"); val mx = hm.flatMap { it.arr }.maxOfOrNull { it.long("") ?: it.numAny() ?: 0 }?.coerceAtLeast(1) ?: 1
                                hm.forEachIndexed { di, row -> Row(verticalAlignment = Alignment.CenterVertically) { Text(days.getOrElse(di) { "" }, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 4.dp)); row.arr.forEach { v -> val n = v.numAny() ?: 0; androidx.compose.foundation.layout.Box(Modifier.weight(1f).height(14.dp).padding(horizontal = 0.5.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = (0.08f + n.toFloat() / mx * 0.85f)))) } } } } } }
                        item { AppCard { Text("Tashrif va konversiya", style = MaterialTheme.typography.titleSmall); d.list("conversion").forEach { x -> KeyValueRow(c.branchName(x.str("branchId")), "${Fmt.num(x.long("traffic"))} · ${x.txt("conversion")}%" + (x.str("source")?.let { " · $it" } ?: "")) }; if (d.list("conversion").isEmpty()) Text("Ma’lumot yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        item { AppCard { Row { Text("Mijoz kayfiyati", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Badge("AI · AUDIO + VIDEO", Tone.Neutral) }; val mood = d.child("mood"); if (mood is kotlinx.serialization.json.JsonObject) mood.entries.forEach { (k, v) -> KeyValueRow(k, v.text) } else Text("Kayfiyat tahlili audio/video AI ulanganda ko‘rinadi", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        item { AppCard { Row { Text("Yo‘qotilgan talab", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Badge("AUDIO AI", Tone.Neutral) }; val ld = d.list("lostDemand"); if (ld.isEmpty()) Text("Ma’lumot yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant); ld.forEach { x -> KeyValueRow(x.str("name") ?: "", "${x.long("count") ?: 0} so‘rov · ${x.str("branchId")?.let { c.branchName(it) } ?: "—"}") } } }
                    }
                    else -> {
                        val t = d.child("total")
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Jami hodisalar", Fmt.num(t.long("total") ?: 0), modifier = Modifier.weight(1f)); KpiCard("Tasdiqlangan", Fmt.num(t.long("confirmed") ?: 0), modifier = Modifier.weight(1f), foot = "rad etilgan: ${t.long("rejected") ?: 0}") } }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Kutilmoqda", Fmt.num(t.long("pending") ?: 0), modifier = Modifier.weight(1f), foot = t.long("disputed")?.takeIf { it > 0 }?.let { "bahsli: $it" }); KpiCard("Aniqlik", t.dbl("precision")?.let { "$it" } ?: "—", if (t.dbl("precision") != null) "%" else "", Modifier.weight(1f), foot = t.long("avgConf")?.let { "o‘rtacha ishonch: $it%" }) } }
                        sectionTitle("AI hodisalar · inson tasdiqlagan ulush")
                        val codes = d.list("codes")
                        if (codes.isEmpty()) item { AppCard { Text("Bu davrda AI hodisalar yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        items(codes) { x -> val p = x.dbl("precision"); AppCard(padding = PaddingValues(12.dp)) {
                            Text(AI_CODES[x.str("code")] ?: x.str("code") ?: "", style = MaterialTheme.typography.titleSmall); Text(x.str("code") ?: "", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) { listOf("Jami" to x.txt("total"), "Tasdiq" to x.txt("confirmed"), "Rad" to x.txt("rejected"), "Kutilmoqda" to x.txt("pending"), "O‘rt. ishonch" to (x.long("avgConf")?.let { "$it%" } ?: "—")).forEach { (l, v) -> Column { Text(l, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v, style = MaterialTheme.typography.bodySmall) } } }
                            if (p == null) Text("baholanmagan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) { BarLine(p.toInt(), Modifier.weight(1f), if (p >= 70) Tone.Accent else if (p >= 50) Tone.Warning else Tone.Danger); Text("  ${p.toInt()}%", fontWeight = FontWeight.SemiBold) }
                        } }
                        item { Text("Tasdiqlash ulushi past bo‘lsa, model ko‘p xato beradi. Bunday turlarda ishonch chegarasi oshiriladi yoki model qayta o‘qitiladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
private fun JsonElement?.text(key: String): String = this.child(key).text

// ═════════════ Hisobotlar ═════════════
private data class Kind(val id: String, val label: String, val desc: String, val roles: List<String>)
private val KINDS = listOf(
    Kind("daily", "Kunlik hisobot", "Tushum, kassa, harajat, AI", listOf("OWNER", "FINANCE", "AUDITOR", "MANAGER")), Kind("cash", "Kassa yopilishlari", "Filial × kun, FOM bilan farqlar", listOf("OWNER", "FINANCE", "AUDITOR", "MANAGER")),
    Kind("pnl", "P&L oylik", "Filiallar kesimida", listOf("OWNER", "FINANCE", "AUDITOR", "MANAGER")), Kind("reconciliation", "Solishtirma dalolatnoma", "Firma bo‘yicha ledger, imzo uchun", listOf("OWNER", "FINANCE", "AUDITOR")),
    Kind("payroll", "Oylik vedomost", "Xodim × komponent, qulflangan davr", listOf("OWNER", "FINANCE", "AUDITOR", "MANAGER")), Kind("attendance", "Davomat tabeli", "Xodim × kun, T-13 shakliga yaqin", listOf("OWNER", "FINANCE", "HR", "AUDITOR", "MANAGER")),
    Kind("ai", "AI hodisalar", "Tasdiq/rad statistikasi, aniqlik", listOf("OWNER", "MANAGER", "OPERATOR", "AUDITOR")),
)
private fun kindLabel(k: String) = KINDS.firstOrNull { it.id == k }?.label ?: k
private fun repSt(s: String) = when (s) { "queued" -> "Navbatda" to Tone.Info; "running" -> "Tayyorlanmoqda…" to Tone.Info; "ready" -> "Tayyor" to Tone.Accent; "failed" -> "Xato" to Tone.Danger; else -> s to Tone.Neutral }

@Composable
fun ReportsScreen(c: AppContainer, me: Me, initialKind: String?) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.branchesCached(); c.api.getAs<List<ReportJob>>("/v1/reports?limit=50") }
    var create by remember { mutableStateOf<Kind?>(KINDS.firstOrNull { it.id == initialKind }) }
    var toast by remember { mutableStateOf<String?>(null) }
    val allowed = KINDS.filter { me.role == "SUPER_ADMIN" || me.role in it.roles }
    LaunchedEffect(loader.data) { if (loader.data?.any { it.status == "queued" || it.status == "running" } == true) { delay(2500); reloadKey++ } }
    fun open(id: String) = scope.launch { try { val j = c.api.getAs<ReportJob>("/v1/reports/$id"); if (j.status != "ready" || j.url == null) toast = "Hali tayyor emas: ${repSt(j.status).first}" else { val u = if (j.url.startsWith("http")) j.url else c.api.baseUrl.trimEnd('/') + j.url; ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u))) } } catch (e: Exception) { toast = e.userMessage() } }
    ScreenScaffold("Hisobotlar", "PDF yoki XLSX fayl fonda tayyorlanadi va 7 kun saqlanadi") {
        DataBox(loader) { list ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                toast?.let { item { InfoBar(it) } }
                if (allowed.isEmpty()) item { AppCard { Text("Rolingiz uchun hisobot turlari yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(allowed, key = { "k-" + it.id }) { k -> AppCard(padding = PaddingValues(0.dp)) { ListRow(k.label, k.desc, trailing = { TextButton({ create = k }) { Text("Tayyorlash") } }) } }
                sectionTitle("So‘nggi hisobotlar · 7 kun")
                if (list.isEmpty()) item { AppCard { Text("Hali hisobot tayyorlanmagan", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(list, key = { it.id }) { x -> val (l, t) = repSt(x.status)
                    AppCard(padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(kindLabel(x.kind), style = MaterialTheme.typography.titleSmall); Text("${Fmt.date(x.from)} – ${Fmt.date(x.to)} · ${x.branchId?.let { c.branchName(it) } ?: "Barcha filiallar"} · ${x.format.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("${Fmt.dateTime(x.createdAt)} · ${x.createdBy}" + (if (x.size > 0) " · ${x.size / 1024} KB" else ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Badge(l, t) }
                        if (x.status == "ready") TextButton({ open(x.id) }, contentPadding = PaddingValues(0.dp)) { Text("Yuklab olish") }
                        if (x.status == "failed" && !x.error.isNullOrBlank()) Text(x.error, color = Danger, style = MaterialTheme.typography.bodySmall)
                    } }
                item { Spacer(Modifier.height(24.dp)) }
            }
            create?.let { k ->
                var from by remember { mutableStateOf(Fmt.isoDay().take(8) + "01") }; var to by remember { mutableStateOf(Fmt.isoDay()) }; var fmt by remember { mutableStateOf("pdf") }; var br by remember { mutableStateOf("") }
                FormSheet(k.label, "Davr ≤ 366 kun. Fayl 7 kun saqlanadi.", primary = "Tayyorlash", onDismiss = { create = null }, onSubmit = {
                    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(from) || !Regex("\\d{4}-\\d{2}-\\d{2}").matches(to) || from > to) return@FormSheet "Davrni to‘g‘ri tanlang."
                    c.api.request("POST", "/v1/reports", buildJsonObject { put("kind", k.id); put("from", from); put("to", to); put("format", fmt); if (br.isNotBlank()) put("branchId", br) }); toast = "«${k.label}» navbatga qo‘yildi."; reloadKey++; null
                }) {
                    Field("Boshlanish (yyyy-mm-dd)", from, { from = it }); Field("Tugash (yyyy-mm-dd)", to, { to = it })
                    if (isNet(me.role)) Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, br, { br = it }) else Text("Filial: o‘z filialingiz", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp)); Seg(listOf("pdf" to "PDF", "xlsx" to "XLSX"), fmt, { fmt = it })
                }
            }
        }
    }
}

// ═════════════ Soliq ═════════════
@Composable
fun TaxScreen(c: AppContainer, me: Me) {
    val scope = rememberCoroutineScope()
    var year by rememberSaveable { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var refresh by remember { mutableStateOf(0) }
    val st = rememberLoader(refresh) { c.api.getJson("/v1/soliq/status") }
    var summary by remember { mutableStateOf<JsonElement?>(null) }; var sumErr by remember { mutableStateOf<String?>(null) }
    var connect by remember { mutableStateOf(false) }; var disconnect by remember { mutableStateOf(false) }; var toast by remember { mutableStateOf<String?>(null) }
    val admin = (me.role == "OWNER" || me.role == "SUPER_ADMIN") && !Access.readOnly(me.role)
    val on = st.data?.bool("connected") == true
    LaunchedEffect(on, year, refresh) { if (on) { summary = null; sumErr = null; try { summary = c.api.getJson("/v1/soliq/summary?year=$year" + (if (refresh > 0) "&refresh=1" else "")) } catch (e: Exception) { sumErr = e.userMessage() } } }
    ScreenScaffold("Soliq", "my.soliq.uz — faqat o‘qish: kassa (OFD), e-faktura, soliq yuki va qarzi") {
        DataBox(st) { s ->
            val co = s.child("company")
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                toast?.let { item { InfoBar(it) } }
                item { AppCard { Row(verticalAlignment = Alignment.CenterVertically) { Text("my.soliq.uz", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Badge(if (on) "Ulangan" else "Ulanmagan", if (on) Tone.Accent else Tone.Neutral) }
                    if (on) Text("Korxona: ${co.str("name") ?: "—"} · STIR ${co.str("tin") ?: "—"}" + (co.str("director")?.let { " · $it" } ?: "") + " · Token: ${s.long("tokenMinutesLeft") ?: "—"} daq · yangilash tokeni: ${s.long("refreshMinutesLeft") ?: "—"} daq", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else if (!admin) Text("Soliq kabinetini faqat egasi yoki super-admin ulay oladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (admin) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { Button({ connect = true }, Modifier.weight(1f)) { Text("Kabinetni ulash") }; if (on) OutlinedButton({ disconnect = true }, Modifier.weight(1f)) { Text("Uzish") } } } }
                if (on) {
                    val ys = (0 until 4).map { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - it }
                    item { Row(verticalAlignment = Alignment.CenterVertically) { Seg(ys.map { "$it" to "$it" }, "$year", { year = it.toInt() }, Modifier.weight(1f)); TextButton({ refresh++ }) { Text("Yangilash") } } }
                    val sm = summary
                    if (sm == null && sumErr == null) item { Text("Soliq portalidan olinmoqda… (40 soniyagacha)", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    sumErr?.let { item { InfoBar("Olinmadi", it, Tone.Danger) } }
                    if (sm != null) {
                        fun ok(x: JsonElement?) = x != null && x !is kotlinx.serialization.json.JsonNull && x.str("error") == null
                        val k = sm.child("kassa").takeIf { ok(it) }; val p = sm.child("purchases").takeIf { ok(it) }; val tax = sm.child("tax").takeIf { ok(it) }; val ge = sm.child("grossEstimate")
                        item { Text("Hisoblangan: ${Fmt.dateTime(sm.str("computedAt"))} · kassa ma’lumoti 1 kun kechikadi" + (k?.str("lastDay")?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("Kassa savdosi (OFD)", k?.long("total"), Modifier.weight(1f), foot = k?.let { "naqd ${mln(it.long("cash"))} · karta ${mln(it.long("card"))}" }); KpiMoney("Xaridlar (e-faktura)", p?.long("total"), Modifier.weight(1f), foot = p?.let { "${it.long("documents")} hujjat · ${it.long("parties")} firma" }) } }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("Taxminiy yalpi marja", ge?.long("value"), Modifier.weight(1f), foot = ge?.dbl("pctOfSales")?.let { "savdodan $it%" }); KpiMoney("Hisoblangan soliq", tax?.long("accrued"), Modifier.weight(1f), foot = tax?.dbl("burdenPct")?.let { "Soliq yuki: $it%" }) } }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("To‘langan", tax?.long("paid"), Modifier.weight(1f)); KpiMoney("Soliq qarzi", tax?.long("debt"), Modifier.weight(1f), foot = tax?.long("overpaid")?.takeIf { it > 0 }?.let { "ortiqcha to‘langan: ${mln(it)}" }) } }
                        sectionTitle("Xavflar va ogohlantirishlar")
                        val risks = sm.list("risks")
                        if (risks.isEmpty()) item { AppCard { Text("Ogohlantirish yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        items(risks) { r -> AppCard(padding = PaddingValues(0.dp)) { ListRow(r.str("topic") ?: "", r.str("detail"), trailing = { Badge(when (r.str("level")) { "high" -> "Yuqori"; "medium" -> "O‘rta"; "low" -> "Past"; else -> r.str("level") ?: "" }, when (r.str("level")) { "high" -> Tone.Danger; "medium" -> Tone.Warning; else -> Tone.Neutral }) }) } }
                        k?.let { kk -> item { AppCard { Text("Oylik kassa savdosi · $year", style = MaterialTheme.typography.titleSmall); HBars(kk.list("months").map { (it.str("month") ?: "").drop(5) to (it.long("total") ?: 0) }, ::mln) } } }
                        sm.child("terminals").takeIf { ok(it) }?.let { tr -> item { AppCard { Text("Terminallar (filiallar) bo‘yicha", style = MaterialTheme.typography.titleSmall); HBars(tr.list("terminals").map { (it.str("address") ?: it.str("salePoint") ?: it.str("terminalId") ?: "") to (it.long("total") ?: 0) }, ::mln); val idle = tr.list("idle"); if (idle.isNotEmpty()) Text("Savdosiz terminallar: " + idle.joinToString(", ") { it.str("address") ?: it.str("terminalId") ?: "" }, color = Warning, style = MaterialTheme.typography.bodySmall) } } }
                        p?.let { pp -> item { AppCard { Row { Text("Eng katta ta’minotchilar (e-faktura)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); pp.dbl("top5Share")?.let { Text("Top-5: $it%", style = MaterialTheme.typography.bodySmall) } }; HBars(pp.list("top").map { (it.str("name") ?: "") to (it.long("sum") ?: 0) }, ::mln); Text("Savdo − xarid; qoldiq o‘zgarishi hisobga olinmagan. Aniq marja — FOM tannarxidan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                        tax?.let { tx -> sectionTitle("Soliq turlari (dalolatnoma)"); items(tx.list("taxes")) { x -> AppCard(padding = PaddingValues(0.dp)) { ListRow(x.str("name") ?: "", "hisoblangan ${mln(x.long("accrued"))} · to‘langan ${mln(x.long("paid"))}", trailing = { val dbt = x.long("debt") ?: 0; Text(if (dbt > 0) mln(dbt) else "—", color = if (dbt > 0) Danger else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold) }) } } }
                        item { TerminalsByPeriod(c) }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
            if (connect) {
                var tok by remember { mutableStateOf("") }; var ref by remember { mutableStateOf("") }
                FormSheet("Soliq kabinetini ulash", "my.soliq.uz ga ERI kalit bilan kiring va sessiyani oling. Tokenlar serverda shifrlanib saqlanadi va qaytarilmaydi.", primary = "Ulash", onDismiss = { connect = false }, onSubmit = {
                    var t = tok.trim(); var r = ref.trim()
                    runCatching { val j = uz.apteka360.app.data.json.parseToJsonElement(t); j.str("token")?.let { tt -> t = tt; r = j.str("refresh_token") ?: r } }
                    if (t.isBlank() || r.isBlank()) return@FormSheet "Ikkala tokenni kiriting."
                    c.api.request("PUT", "/v1/soliq/session", buildJsonObject { put("token", t); put("refresh_token", r) }); toast = "Soliq kabineti ulandi"; refresh++; null
                }) { Field("Access token (yoki JSON)", tok, { tok = it }, password = true); Field("Refresh token", ref, { ref = it }, password = true) }
            }
            if (disconnect) ConfirmDialog("Uzish", "Soliq sessiyasi serverdan o‘chiriladi. Davom etasizmi?", "Uzish", danger = true, onDismiss = { disconnect = false }) { c.api.request("DELETE", "/v1/soliq/session"); toast = "Soliq sessiyasi o‘chirildi"; refresh++; null }
        }
    }
}

@Composable
private fun TerminalsByPeriod(c: AppContainer) {
    var from by remember { mutableStateOf(Fmt.isoDay(30)) }; var to by remember { mutableStateOf(Fmt.isoDay(1)) }
    var key by remember { mutableStateOf(0) }
    val loader = rememberLoader(key) { c.api.getJson("/v1/soliq/kassa" + qs("from" to from, "to" to to)) }
    Column {
        Text("Terminal savdosi · davr bo‘yicha", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        DateRange(from, to) { f, t -> from = f; to = t; key++ }
        val d = loader.data; val err = loader.error.value
        when {
            d == null && err == null -> Text("Olinmoqda…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            d == null -> InfoBar("Bu bo‘lim olinmadi", err!!.userMessage(), Tone.Danger)
            else -> AppCard { Text("Jami: ${Fmt.money(d.long("total"))}", style = MaterialTheme.typography.titleSmall); val ts = d.list("terminals"); if (ts.isEmpty()) Text("Bu davrda savdo yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant); ts.forEach { x -> KeyValueRow(x.str("address") ?: x.str("salePoint") ?: x.str("terminalId") ?: "—", "${Fmt.num(x.long("total"))}" + (x.dbl("share")?.let { " · $it%" } ?: "")) }; val idle = d.list("idle"); if (idle.isNotEmpty()) InfoBar("Savdosiz terminallar", idle.joinToString(", ") { it.str("address") ?: it.str("salePoint") ?: it.str("terminalId") ?: "" }, Tone.Warning) }
        }
    }
}
