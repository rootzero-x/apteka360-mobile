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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.AiEvent
import uz.apteka360.app.data.AuditEntry
import uz.apteka360.app.data.EmployeeFull
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.str
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.BarLine
import uz.apteka360.app.ui.components.ConfirmDialog
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.Field
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.FormSheet
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.KpiCard
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.PickField
import uz.apteka360.app.ui.components.SearchField
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger
import uz.apteka360.app.ui.theme.Info
import uz.apteka360.app.ui.theme.Warning

// Desktop hr.js bilan bir xil qoidalar
const val MASK = "•• ••• •••"
fun empTone(s: String) = when (s) { "ishda" -> Tone.Accent; "tanaffus" -> Tone.Info; "kech qoldi", "kasal" -> Tone.Warning; "dismissed" -> Tone.Danger; else -> Tone.Neutral }
fun empLabel(s: String) = if (s == "dismissed") "ishdan bo‘shagan" else s
fun canSal(role: String) = role in listOf("OWNER", "FINANCE", "MANAGER", "AUDITOR")
fun canEditEmp(role: String) = !Access.readOnly(role) && role in listOf("OWNER", "SUPER_ADMIN", "HR", "MANAGER")
fun fmtHm(min: Int): String { val m = maxOf(0, min); val hh = m / 60; val mm = m % 60; return if (hh > 0) "$hh s" + (if (mm > 0) " $mm daq" else "") else "$mm daq" }
fun fmtDec(n: Double?): String = if (n == null) "—" else (Math.round(n * 100) / 100.0).toString().replace('.', ',').removeSuffix(",0")
fun shortBranch(n: String) = n.replace(Regex("-filial$", RegexOption.IGNORE_CASE), "").replace(Regex(" filiali$", RegexOption.IGNORE_CASE), "")

/** Kunlik davomat belgisi (web prototip qoidasi — API'da kunlik endpoint yo'q). */
fun attendanceMark(e: EmployeeFull, d: Int): String {
    val sch = e.schedule
    val work = if (sch.startsWith("5/2")) (d + 2) % 7 !in listOf(3, 4) else if (sch.startsWith("1/2")) d % 3 == 1 else d % 4 < 2
    if (d > 8) return if (work) "" else "·"
    var st = if (work) "V" else "·"
    if (work && e.status == "ta’til" && d >= 6) st = "T"
    if (work && e.status == "kasal" && d >= 5) st = "B"
    if (work && st == "V" && e.late > 0 && (d * 7 + e.late) % 5 == 0) st = "K"
    if (work && st == "V" && (e.attendance ?: 100.0) < 90 && d == 2) st = "Y"
    return st
}
fun markColors(mk: String): Pair<Color, Color> = when (mk) {
    "V" -> Color(0x1A1B8A55) to Color(0xFF167346); "K" -> Color(0x2EE2A336) to Warning; "Y" -> Color(0x1AE5484D) to Danger
    "T", "B" -> Color(0x1F4F8CFF) to Info; "·" -> Color(0x14888888) to Color(0xFF8A928D); else -> Color.Transparent to Color(0xFF8A928D)
}
val LEGEND = listOf("V — vaqtida", "K — kechikdi", "Y — kelmagan", "T — ta’til", "B — kasal", "· — dam olish")

// ═════════════ Xodimlar ro'yxati ═════════════
@Composable
fun EmployeesScreen(c: AppContainer, me: Me, initialBranch: String?, go: (String, String?) -> Unit) {
    val loader = rememberLoader { c.branchesCached(); c.repo.employeesFull() }
    var q by rememberSaveable { mutableStateOf("") }
    var br by rememberSaveable { mutableStateOf(initialBranch ?: "") }
    var filter by rememberSaveable { mutableStateOf("all") }
    var addOpen by remember { mutableStateOf(false) }
    val sal = canSal(me.role); val edit = canEditEmp(me.role)
    val multi = c.branches.size > 1 && (me.scope == "all" || me.branchIds.size > 1)
    val all = loader.data ?: emptyList()
    val inBranch = all.filter { br.isBlank() || it.branchId == br }
    val active = inBranch.filter { !it.dismissed }
    val rows = (if (filter == "arxiv") inBranch.filter { it.dismissed } else active).filter { e ->
        when (filter) { "all", "arxiv" -> true; "risk" -> !e.risk.isNullOrBlank(); "ta’til" -> e.status == "ta’til" || e.status == "kasal"; else -> e.status == filter }
    }.filter { e -> q.isBlank() || "${e.name} ${e.position} ${e.fomLogin} ${e.phone}".contains(q, ignoreCase = true) }
    ScreenScaffold("Xodimlar", if (loader.data != null) "${active.size} ta faol xodim · ${if (br.isBlank()) "barcha filiallar" else c.branchName(br)}" else null,
        fab = if (edit) Icons.Default.Add to { addOpen = true } else null) {
        DataBox(loader) { _ ->
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SearchField(q, { q = it }, "Ism, lavozim, FOM login…")
                    if (multi) Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, br, { br = it })
                    Seg(listOf("all" to "Hammasi", "ishda" to "Ishda", "tanaffus" to "Tanaffusda", "smena yo‘q" to "Smena yo‘q", "ta’til" to "Ta’til / kasal", "risk" to "Xavf belgisi", "arxiv" to "Arxiv"), filter, { filter = it })
                }
                if (rows.isEmpty()) EmptyBox("Filtrga mos xodim topilmadi", Modifier.fillMaxSize())
                else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rows, key = { it.id }) { e ->
                        AppCard(padding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Avatar(e.ini.ifBlank { Fmt.initials(e.name) }, 40); Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, style = MaterialTheme.typography.titleSmall)
                                    Text(listOf(e.position, shortBranch(c.branchName(e.branchId))).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Badge(empLabel(e.status), empTone(e.status))
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Stat("Grafik", e.schedule.ifBlank { "—" }); Stat("Davomat", e.attendance?.let { "${it.toInt()}%" } ?: "—")
                                Stat("Idle", if (e.idleLimit > 0) "${e.idleUsed * 100 / e.idleLimit}%" else "—", if (e.idleLimit > 0 && e.idleUsed * 100 / e.idleLimit > 90) Danger else null)
                                Stat("Oylik", if (sal && e.net != null) Fmt.num(e.net) else MASK)
                            }
                            val flag = e.risk ?: (if (e.docs.isNotBlank() && e.docs != "Hammasi amalda") e.docs else null)
                            if (!flag.isNullOrBlank()) Text(flag, color = Danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                            TextButton(onClick = { go("employee", e.id) }, contentPadding = PaddingValues(0.dp)) { Text("Batafsil →") }
                        }
                    }
                    item { Text("${rows.size} / ${inBranch.size} ko‘rsatilmoqda · Oylik ustuni ${if (sal) "ko‘rinadi (maxfiy ruxsat bor)" else "yashirilgan — maxfiy ruxsat yo‘q"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
    if (addOpen) EmployeeForm(c, me, null, onDismiss = { addOpen = false }, onSaved = { id -> addOpen = false; if (id != null) go("employee", id) })
}

@Composable
private fun Stat(label: String, value: String, color: Color? = null) {
    Column { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = color ?: MaterialTheme.colorScheme.onSurface) }
}

// ═════════════ Xodim formasi ═════════════
@Composable
fun EmployeeForm(c: AppContainer, me: Me, e: EmployeeFull?, onDismiss: () -> Unit, onSaved: (String?) -> Unit) {
    val lock = if (me.role == "MANAGER") me.branchId else null
    val brs = c.branches.filter { lock == null || it.id == lock }
    val salaryHidden = e != null && e.oklad == null
    var name by remember { mutableStateOf(e?.name ?: "") }; var position by remember { mutableStateOf(e?.position ?: "Farmatsevt") }
    var branch by remember { mutableStateOf(brs.firstOrNull { it.id == (e?.branchId ?: lock) } ?: brs.firstOrNull()) }
    var phone by remember { mutableStateOf(e?.phone ?: "") }; var oklad by remember { mutableStateOf(e?.oklad?.toString() ?: "") }; var coef by remember { mutableStateOf(e?.coef?.toString() ?: "1") }
    var schedule by remember { mutableStateOf(e?.schedule ?: "2/2 · 08:00–20:00") }; var hired by remember { mutableStateOf(e?.hired ?: "") }; var fom by remember { mutableStateOf(e?.fomLogin ?: "") }; var contract by remember { mutableStateOf(e?.contract ?: "") }
    FormSheet(if (e == null) "Xodim qo‘shish" else "Xodimni tahrirlash", onDismiss = onDismiss, onSubmit = {
        if (name.isBlank()) return@FormSheet "F.I.Sh. majburiy."
        val b = branch ?: return@FormSheet "Filialni tanlang."
        val ok = oklad.replace(" ", "").replace(',', '.').toDoubleOrNull()
        val cf = coef.replace(',', '.').toDoubleOrNull()
        if (!salaryHidden && ok == null) return@FormSheet "Oklad majburiy (son)."
        if (!salaryHidden && coef.isNotBlank() && cf == null) return@FormSheet "Koeffitsient — son kiriting."
        val body = buildJsonObject {
            if (e != null) put("id", e.id)
            put("name", name.trim()); put("position", position.trim()); put("branchId", b.id)
            if (phone.isNotBlank()) put("phone", phone.trim())
            if (!salaryHidden) { put("oklad", ok); if (cf != null) put("coef", cf) }
            if (schedule.isNotBlank()) put("schedule", schedule.trim()); if (hired.isNotBlank()) put("hired", hired.trim()); if (fom.isNotBlank()) put("fomLogin", fom.trim()); if (contract.isNotBlank()) put("contract", contract.trim())
        }
        val r = c.repo.cmd("employees.upsert", body)
        onSaved(r.str("id") ?: e?.id); null
    }) {
        if (salaryHidden) { InfoBar("Oylik maydonlari yashirin.", "Sizning rolingiz oylikni ko‘rmaydi — oklad va koeffitsient o‘zgarmaydi."); Spacer(Modifier.height(8.dp)) }
        Field("F.I.Sh. *", name, { name = it }); Field("Lavozim", position, { position = it })
        PickField("Filial", brs, branch, { it.name }, { branch = it })
        Field("Telefon", phone, { phone = it }, hint = "+998 90 123 45 67")
        if (!salaryHidden) { Field("Oklad *", oklad, { oklad = it }, number = true); Field("Koeffitsient", coef, { coef = it }, number = true) }
        Field("Ish grafigi", schedule, { schedule = it }); Field("Ishga kirgan", hired, { hired = it }, hint = "2026-yil oktabr")
        Field("FOM login", fom, { fom = it }); Field("Shartnoma", contract, { contract = it })
    }
}

// ═════════════ Xodim tafsiloti ═════════════
data class EmpDetailData(val e: EmployeeFull?, val ai: List<AiEvent>?, val audit: List<AuditEntry>?)

@Composable
fun EmployeeDetailScreen(c: AppContainer, me: Me, id: String, go: (String, String?) -> Unit, onBack: () -> Unit) {
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(id, reloadKey) {
        c.branchesCached()
        val e = c.repo.employeesFull().firstOrNull { it.id == id }
        EmpDetailData(e, runCatching { c.repo.aiEvents() }.getOrNull(), runCatching { c.repo.audit() }.getOrNull())
    }
    val tabs = listOf("umumiy" to "Umumiy", "davomat" to "Davomat va idle", "oylik" to "Oylik", "kpi" to "KPI", "jarima" to "Jarimalar va AI", "hujjat" to "Hujjatlar", "audit" to "Audit")
    var tab by rememberSaveable { mutableStateOf("umumiy") }
    var editOpen by remember { mutableStateOf(false) }; var dismissOpen by remember { mutableStateOf(false) }; var restoreOpen by remember { mutableStateOf(false) }
    ScreenScaffold(loader.data?.e?.name ?: "Xodim", loader.data?.e?.let { listOf(it.position, c.branchName(it.branchId), it.schedule).filter { s -> s.isNotBlank() }.joinToString(" · ") }, onBack = onBack) {
        DataBox(loader) { d ->
            val e = d.e
            if (e == null) { EmptyBox("Xodim topilmadi", Modifier.fillMaxSize()); return@DataBox }
            val sal = canSal(me.role); val edit = canEditEmp(me.role)
            val evs = d.ai?.filter { it.empId == e.id } ?: emptyList()
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(e.ini.ifBlank { Fmt.initials(e.name) }, 56); Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Badge(empLabel(e.status), empTone(e.status)); e.risk?.let { Badge(it, Tone.Warning) } }
                                Text("${e.phone.ifBlank { "—" }} · FOM: ${e.fomLogin.ifBlank { "—" }} · Ishga kirgan: ${e.hired.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        if (edit) { Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ editOpen = true }, Modifier.weight(1f)) { Text("Tahrirlash") }
                            if (e.dismissed) Button({ restoreOpen = true }, Modifier.weight(1f)) { Text("Tiklash") } else OutlinedButton({ dismissOpen = true }, Modifier.weight(1f)) { Text("Ishdan bo‘shatish", color = Danger, maxLines = 1) }
                        } }
                    }
                }
                if (e.dismissed) item { InfoBar("Xodim ishdan bo‘shatilgan.", "Ma’lumotlar tarix uchun saqlanadi. «Tiklash» xodimni va uning hisobini qaytaradi.", Tone.Warning) }
                item { ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0), edgePadding = 0.dp) { tabs.forEach { (k, l) -> Tab(selected = tab == k, onClick = { tab = k }, text = { Text(l) }) } } }
                when (tab) {
                    "umumiy" -> {
                        item { AppCard { Text("Shaxsiy", style = MaterialTheme.typography.titleSmall); KeyValueRow("F.I.Sh.", e.name); KeyValueRow("Telefon", e.phone.ifBlank { "—" }); KeyValueRow("Biometriya", "Yuz shabloni · maxfiy") } }
                        item { AppCard { Text("Ish", style = MaterialTheme.typography.titleSmall); KeyValueRow("Lavozim", e.position); KeyValueRow("Filial", c.branchName(e.branchId)); KeyValueRow("Grafik", e.schedule.ifBlank { "—" }); KeyValueRow("Shartnoma", e.contract.ifBlank { "—" }); KeyValueRow("FOM login", if (e.fomLogin.isNotBlank()) "${e.fomLogin} · bog‘langan" else "—") } }
                        item { AppCard { Text("Oy ko‘rsatkichlari", style = MaterialTheme.typography.titleSmall)
                            KeyValueRow("Davomat", e.attendance?.let { "${it.toInt()}%" } ?: "—"); KeyValueRow("Kechikish", "${e.late} marta", if (e.late > 3) Danger else MaterialTheme.colorScheme.onSurface); KeyValueRow("Kelmagan", "${e.absent} kun")
                            val idleBad = e.idleLimit > 0 && e.idleUsed.toDouble() / e.idleLimit > 0.9
                            KeyValueRow("Idle", if (e.idleLimit > 0) "${fmtHm(e.idleUsed)} / ${fmtHm(e.idleLimit)}" else "limit yo‘q", if (idleBad) Danger else MaterialTheme.colorScheme.onSurface)
                            KeyValueRow("AI hodisalar", if (d.ai != null) "${evs.size} ta (${evs.count { it.status == "pending" }} kutilmoqda)" else "ruxsat yo‘q")
                            KeyValueRow("Hujjatlar", e.docs.ifBlank { "—" }, if (e.docs == "Hammasi amalda") MaterialTheme.colorScheme.primary else Danger) } }
                    }
                    "davomat" -> {
                        item { AppCard { Text("Oktabr 2026 · kamera + zaxira check-in", style = MaterialTheme.typography.titleSmall); Spacer(Modifier.height(8.dp)); AttendanceCalendar(e); Spacer(Modifier.height(8.dp)); Text(LEGEND.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        item { val pct = if (e.idleLimit > 0) minOf(100, e.idleUsed * 100 / e.idleLimit) else 0
                            AppCard { Text("Bo‘sh vaqt (idle) limiti · oktabr", style = MaterialTheme.typography.titleSmall)
                                Row(verticalAlignment = Alignment.Bottom) { Text(if (e.idleLimit > 0) fmtHm(e.idleUsed) else "—", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f)); Text("ruxsat: " + (if (e.idleLimit > 0) fmtHm(e.idleLimit) else "belgilanmagan"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Spacer(Modifier.height(6.dp)); BarLine(pct, tone = if (pct > 90) Tone.Danger else Tone.Accent)
                                Text("Qoldi: " + (if (e.idleLimit > 0) fmtHm(maxOf(0, e.idleLimit - e.idleUsed)) else "—") + " · faqat tasdiqlangan hodisalar hisoblanadi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                                KeyValueRow("Telefon ishlatish", fmtHm((e.idleUsed * 0.55).toInt())); KeyValueRow("O‘tirish / yotish", fmtHm((e.idleUsed * 0.3).toInt())); KeyValueRow("Ish zonasidan tashqarida", fmtHm((e.idleUsed * 0.15).toInt())) } }
                        item { Text("Kunlik belgilar web-panel prototipi qoidasi bo‘yicha (API’da kunlik davomat endpointi hali yo‘q).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    "oylik" -> item { SalaryCard(c, e, sal) }
                    "kpi" -> item {
                        val plan = e.kpiPlan ?: 0.0; val fact = e.kpiFact ?: 0.0; val p = if (plan > 0) fact / plan * 100 else 0.0
                        val tier = if (p >= 100) "3 (2,5%)" else if (p >= 80) "2 (2,0%)" else "1 (1,5%)"
                        AppCard { Row { Text("Shaxsiy sotuv KPI · oktabr", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Badge("FOM", Tone.Neutral) }
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 8.dp)) { Text(if (plan > 0) Fmt.money(fact) else "—", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f)); Text("reja: " + (if (plan > 0) Fmt.money(plan) else "—"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            BarLine(minOf(100, p.toInt()), tone = Tone.Accent)
                            Text("Bajarildi: ${String.format("%.1f", p).replace('.', ',')}% · KPI pog‘onasi: $tier · hisoblangan KPI: ${if (sal && e.kpi != null) Fmt.money(e.kpi) else MASK}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
                    }
                    "jarima" -> {
                        if (d.ai == null) item { InfoBar("Ruxsat yo‘q.", "AI hodisalari sizning rolingiz uchun yopiq.") }
                        else if (evs.isEmpty()) item { AppCard { Text("Bu oy AI hodisa va jarima yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        else items(evs, key = { "ai-" + it.id }) { x -> AppCard(padding = PaddingValues(0.dp)) { ListRow(x.label, "${x.date}, ${x.time} · ${x.camera} · ${x.dur}", trailing = { Column(horizontalAlignment = Alignment.End) { Text("${x.conf}%", fontWeight = FontWeight.SemiBold); Badge(aiStatusLabel(x.status), aiStatusTone(x.status)) } }, onClick = if (Access.can("ai", me.role)) { { go("ai", x.id) } } else null) } }
                    }
                    "hujjat" -> item {
                        val exp = e.docs.isNotBlank() && e.docs != "Hammasi amalda"
                        AppCard(padding = PaddingValues(0.dp)) {
                            listOf(Triple("Pasport", "—", "Amalda" to Tone.Accent), Triple("Sanitariya kitobchasi", "—", (if (exp) e.docs else "Amalda") to (if (exp) Tone.Danger else Tone.Accent)), Triple("Farmatsevt sertifikati", "—", "Amalda" to Tone.Accent), Triple("Mehnat shartnomasi", e.contract.ifBlank { "muddatsiz" }, "Imzolangan" to Tone.Accent))
                                .forEachIndexed { i, (n, u, st) -> if (i > 0) HorizontalDivider(); ListRow(n, "Amal qilish: $u", trailing = { Badge(st.first, st.second) }) }
                        }
                        Text("Hujjatlar holati: «${e.docs.ifBlank { "—" }}». Alohida hujjat sanalari API’da hali yo‘q.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                    "audit" -> {
                        val rows = d.audit?.filter { it.`object`.contains(e.name) || it.`object` == e.id }
                        if (rows == null) item { InfoBar("Ruxsat yo‘q.", "Audit jurnali sizning rolingiz uchun yopiq.") }
                        else if (rows.isEmpty()) item { AppCard { Text("Bu xodim bo‘yicha audit yozuvi yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        else items(rows, key = { "au-" + it.id }) { a -> AppCard(padding = PaddingValues(14.dp)) { Text("${a.user} · ${a.action}", style = MaterialTheme.typography.titleSmall); Text("${a.before.ifBlank { "—" }} → ${a.after.ifBlank { "—" }} · Sabab: ${a.reason.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(if (a.at != null) Fmt.dateTime(a.at) else a.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
            if (editOpen) EmployeeForm(c, me, e, onDismiss = { editOpen = false }, onSaved = { editOpen = false; reloadKey++ })
            if (dismissOpen) {
                var date by remember { mutableStateOf(Fmt.isoDay()) }
                FormSheet("Ishdan bo‘shatish", "${e.name} — holati «ishdan bo‘shagan» bo‘ladi. Bog‘langan hisob bloklanadi, sessiyalari va Telegram bog‘lanishi o‘chiriladi. Tarix saqlanadi.", primary = "Ishdan bo‘shatish", danger = true, onDismiss = { dismissOpen = false }, onSubmit = {
                    val reason = dismissReason.trim(); if (reason.length < 3) return@FormSheet "Sabab kamida 3 belgi bo‘lsin."
                    if (date.isBlank()) return@FormSheet "Sanani kiriting."
                    c.repo.cmd("employees.dismiss", buildJsonObject { put("id", e.id); put("date", date); put("reason", reason) }); reloadKey++; null
                }) { Field("Sana (yyyy-mm-dd)", date, { date = it }); Field("Sabab (≥ 3 belgi)", dismissReason, { dismissReason = it }, hint = "Masalan: o‘z xohishiga ko‘ra ariza") }
            }
            if (restoreOpen) ConfirmDialog("Xodimni tiklash", "${e.name} — holati «smena yo‘q» bo‘ladi va bo‘shatishda bloklangan hisob qayta faollashadi.", "Tiklash", reasonLabel = "Sabab (≥ 3 belgi)", minReason = 3, onDismiss = { restoreOpen = false }) { r ->
                c.repo.cmd("employees.restore", buildJsonObject { put("id", e.id); put("reason", r) }); reloadKey++; null
            }
        }
    }
}
private var dismissReason by mutableStateOf("")

fun aiStatusLabel(s: String) = when (s) { "pending" -> "Tekshiruv kutilmoqda"; "confirmed" -> "Tasdiqlangan"; "rejected" -> "Rad etilgan"; "disputed" -> "E’tiroz"; else -> s }
fun aiStatusTone(s: String) = when (s) { "pending" -> Tone.Warning; "confirmed" -> Tone.Accent; "rejected" -> Tone.Danger; "disputed" -> Tone.Info; else -> Tone.Neutral }

@Composable
fun AttendanceCalendar(e: EmployeeFull) {
    LazyVerticalGrid(GridCells.Fixed(7), Modifier.fillMaxWidth().heightIn(max = 300.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), userScrollEnabled = false) {
        items(31) { idx ->
            val d = idx + 1
            val mk = attendanceMark(e, d); val fut = d > 8
            val (bg, fg) = if (fut) Color.Transparent to Color(0xFF8A928D) else markColors(mk)
            Column(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$d", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = fg); Text(mk, style = MaterialTheme.typography.labelMedium, color = fg)
            }
        }
    }
}

@Composable
private fun SalaryCard(c: AppContainer, e: EmployeeFull, sal: Boolean) {
    var shown by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Oktabr · joriy hisob", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Badge("MAXFIY", Tone.Warning) }
        Spacer(Modifier.height(8.dp))
        if (!sal || e.oklad == null) { Text("Ruxsat yo‘q", fontWeight = FontWeight.SemiBold); Text("Oylik ma’lumotini ko‘rish uchun «Oylik — maxfiy» ruxsati kerak. Ruxsatni Egasi yoki Super admin beradi.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall); return@AppCard }
        if (!shown) {
            Text("•• ••• ••• so‘m", style = MaterialTheme.typography.headlineMedium)
            Text("«Ko‘rsatish» bosilsa, harakat audit jurnaliga yoziladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp)); OutlinedButton({ scope.launch { c.repo.auditLog("Maxfiy ma’lumot ko‘rildi", "Oylik · ${e.name}", e.branchId); shown = true } }) { Text("Ko‘rsatish") }
        } else {
            fun n(v: Long?) = if (v == null) MASK else Fmt.num(v)
            KeyValueRow("Oklad", n(e.oklad)); KeyValueRow("Davomat koeffitsienti", "× " + fmtDec(e.coef)); KeyValueRow("Shaxsiy KPI", "+" + n(e.kpi), MaterialTheme.colorScheme.primary); KeyValueRow("Bonus dorilar", "+" + n(e.bonus), MaterialTheme.colorScheme.primary); KeyValueRow("Premiya", "+" + n(e.premium), MaterialTheme.colorScheme.primary)
            KeyValueRow("Jarimalar", if ((e.penalties ?: 0) > 0) "−" + n(e.penalties) else "0", if ((e.penalties ?: 0) > 0) Danger else MaterialTheme.colorScheme.onSurface); KeyValueRow("Soliq (JShDS)", "−" + n(e.tax)); KeyValueRow("Avans", if ((e.advance ?: 0) > 0) "−" + n(e.advance) else "0"); KeyValueRow("Qarz", if ((e.debt ?: 0) > 0) "−" + n(e.debt) else "0")
            HorizontalDivider(Modifier.padding(vertical = 6.dp)); KeyValueRow("Qo‘lga tegadi", e.net?.let { Fmt.money(it) } ?: "$MASK so‘m")
            Text("Ko‘rish audit jurnaliga yozildi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp)); OutlinedButton({ shown = false }) { Text("Yashirish") }
        }
    }
}
