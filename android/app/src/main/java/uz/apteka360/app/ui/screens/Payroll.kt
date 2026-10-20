package uz.apteka360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.AiEvent
import uz.apteka360.app.data.EmployeeFull
import uz.apteka360.app.data.FormulaToken
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.PayrollRun
import uz.apteka360.app.data.PayrollTemplate
import uz.apteka360.app.data.PayrollTestResult
import uz.apteka360.app.data.json
import uz.apteka360.app.data.str
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.ConfirmDialog
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.Field
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.KpiCard
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.PickField
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger
import uz.apteka360.app.ui.theme.Warning

// ═════════════ Oylik vedomosti (desktop hr.js W18) ═════════════
private val P_STATUS = listOf("Qoralama", "Hisoblangan", "Mudirlar tekshiruvida", "Egasi tasdig‘ida", "Qulflangan", "To‘langan")
private val P_STEPS = listOf("Qoralama", "Hisoblangan", "Mudirlar tekshiruvi", "Egasi tasdig‘i", "Qulflangan", "To‘lov")
data class PayrollData(val run: PayrollRun, val employees: List<EmployeeFull>, val ai: List<AiEvent>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.branchesCached(); PayrollData(c.repo.payroll(), c.repo.employeesFull(false), runCatching { c.repo.aiEvents() }.getOrDefault(emptyList())) }
    var toast by remember { mutableStateOf<String?>(null) }
    var payslip by remember { mutableStateOf<EmployeeFull?>(null) }
    var lockOpen by remember { mutableStateOf(false) }; var paidOpen by remember { mutableStateOf(false) }
    val rl = me.role; val ro = Access.readOnly(rl); val sal = canSal(rl)
    ScreenScaffold("Oylik vedomosti", loader.data?.let { "${it.run.period.ifBlank { "Oktabr 2026" }} · soliq: JShDS (sozlanadi)" }) {
        DataBox(loader) { d ->
            val pe = d.employees; val reviewed = d.run.reviewed
            val stage = if (d.run.stage == 2 && reviewed.values.all { it }) 3 else d.run.stage
            val scopeBranch = if (rl == "MANAGER") me.branchId else null
            val brs = c.branches.filter { scopeBranch == null || it.id == scopeBranch }
            val tg = pe.sumOf { it.gross ?: 0 }; val tn = pe.sumOf { it.net ?: 0 }; val tded = pe.sumOf { (it.penalties ?: 0) + (it.tax ?: 0) + (it.advance ?: 0) + (it.debt ?: 0) }
            fun money(n: Long) = if (sal) Fmt.money(n) else MASK
            fun run(cmd: String, body: kotlinx.serialization.json.JsonObject = buildJsonObject {}, ok: String) { scope.launch { try { c.repo.cmd(cmd, body); toast = ok; reloadKey++ } catch (x: Exception) { toast = x.userMessage() } } }
            data class Act(val title: String, val text: String, val btn: String, val disabled: Boolean = false, val go: () -> Unit)
            val act: Act? = if (ro) null else when {
                rl == "FINANCE" && stage <= 3 -> Act("Vedomostni qayta hisoblash", "Davomat, KPI (FOM), bonus dorilar, tasdiqlangan jarimalar, avans va qarzlar qayta olinadi. Mudirlar tekshiruvi qaytadan boshlanadi.", "Qayta hisoblash") { run("payroll.recalc", ok = "Vedomost qayta hisoblandi. Filial mudirlari tekshiruvi qayta boshlandi.") }
                rl == "FINANCE" && stage == 4 -> Act("To‘lovni belgilash", "Vedomost qulflangan. To‘lov bank orqali o‘tkazilgandan so‘ng belgilang — xodimlarga payslip yuboriladi.", "To‘landi deb belgilash") { paidOpen = true }
                rl == "MANAGER" && scopeBranch != null && reviewed[scopeBranch] != true && stage <= 3 -> Act("${c.branchName(scopeBranch)} vedomostini tekshiring", "${pe.size} xodim · jami ${money(tn)}. Qatorni bosib har bir xodim payslipini ko‘ring.", "Tekshirildi — Egasiga yuborish") { run("payroll.review", buildJsonObject { put("branchId", scopeBranch) }, "${c.branchName(scopeBranch)} tekshirildi.") }
                rl == "OWNER" && stage <= 1 -> Act("Vedomostni qayta hisoblash", "Davomat, KPI, bonus, jarima, avans va qarzlar qayta olinadi.", "Qayta hisoblash") { run("payroll.recalc", ok = "Vedomost qayta hisoblandi.") }
                rl == "OWNER" && stage == 2 -> Act("Egasi tasdig‘i hali ochilmagan", "Filial mudirlari tekshiruvi kutilmoqda.", "Tasdiqlash va qulflash", true) {}
                rl == "OWNER" && stage == 3 -> Act("Vedomost tasdiqingizni kutmoqda", "Barcha filial mudirlari tekshirdi. Tasdiqlangandan so‘ng vedomost qulflanadi.", "Tasdiqlash va qulflash") { lockOpen = true }
                rl == "OWNER" && stage == 4 -> Act("To‘lovni belgilash", "Vedomost qulflangan. To‘lov o‘tkazilgandan so‘ng belgilang.", "To‘landi deb belgilash") { run("payroll.markPaid", ok = "Vedomost to‘langan deb belgilandi.") }
                else -> null
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Badge(P_STATUS.getOrElse(stage) { "$stage" }, if (stage >= 5) Tone.Accent else if (stage == 4) Tone.Info else Tone.Warning) }
                item { AppCard(padding = PaddingValues(12.dp)) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) { P_STEPS.forEachIndexed { i, n ->
                    val done = i < stage || stage == 5; val cur = i == stage && stage != 5
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
                        Box(Modifier.size(26.dp).background(if (done) MaterialTheme.colorScheme.primary else if (cur) Color(0x2EE2A336) else MaterialTheme.colorScheme.surfaceVariant, CircleShape), contentAlignment = Alignment.Center) { Text(if (done) "✓" else "${i + 1}", color = if (done) Color.White else if (cur) Warning else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
                        Text(n, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    } } } } }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Hisoblangan (gross)", money(tg), modifier = Modifier.weight(1f)); KpiCard("Ushlanmalar", if (sal) "−" + Fmt.money(tded) else MASK, modifier = Modifier.weight(1f)) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("To‘lanadi (net)", money(tn), modifier = Modifier.weight(1f)); KpiCard("Xodimlar", "${pe.size}", "ta", Modifier.weight(1f)) } }
                act?.let { a -> item { AppCard { Text(a.title, style = MaterialTheme.typography.titleSmall); Text(a.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp)); Button(onClick = a.go, enabled = !a.disabled, modifier = Modifier.fillMaxWidth()) { Text(a.btn) } } } }
                item { AppCard(padding = PaddingValues(0.dp)) { brs.forEachIndexed { i, b -> if (i > 0) HorizontalDivider(); val bs = pe.filter { it.branchId == b.id }.sumOf { it.net ?: 0 }; ListRow(b.name, b.manager, trailing = { Column(horizontalAlignment = Alignment.End) { Text(money(bs), style = MaterialTheme.typography.bodySmall); if (reviewed[b.id] == true) Badge("Tekshirildi", Tone.Accent) else Badge("Kutilmoqda", Tone.Warning) } }) } } }
                if (stage >= 4) item { InfoBar("Vedomost qulflangan.", "O‘zgartirish faqat «Tuzatish vedomosti» orqali, Egasi tasdig‘i bilan amalga oshiriladi.", Tone.Accent) }
                if (!sal) item { InfoBar("Oylik summalari yashirilgan.", "Sizning rolingizda «Oylik — maxfiy» ruxsati yo‘q.") }
                sectionTitle("Xodimlar")
                items(pe, key = { it.id }) { e ->
                    fun m(v: Long?) = if (v == null) MASK else Fmt.num(v)
                    AppCard(Modifier.clickable { payslip = e }, padding = PaddingValues(12.dp)) {
                        Row { Column(Modifier.weight(1f)) { Text(e.name, style = MaterialTheme.typography.titleSmall); Text(shortBranch(c.branchName(e.branchId)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(m(e.net), fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Oklad" to m(e.oklad), "Koef." to fmtDec(e.coef), "KPI" to m(e.kpi), "Jarima" to (if (e.penalties == null) MASK else if (e.penalties > 0) "−" + Fmt.num(e.penalties) else "0"), "Soliq" to (if (e.tax == null) MASK else "−" + Fmt.num(e.tax))).forEach { (l, v) -> Column { Text(l, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v, style = MaterialTheme.typography.bodySmall, color = if (l == "Jarima" && (e.penalties ?: 0) > 0) Danger else MaterialTheme.colorScheme.onSurface) } }
                        }
                    }
                }
                if (pe.isEmpty()) item { AppCard { Text("Hali xodim yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                item { Text("Gross: ${if (sal) Fmt.num(tg) else MASK} · Qo‘lga jami: ${money(tn)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
                item { Spacer(Modifier.height(24.dp)) }
            }
            payslip?.let { e ->
                val ev = d.ai.firstOrNull { it.empId == e.id && it.status == "confirmed" } ?: d.ai.firstOrNull { it.empId == e.id }
                ModalBottomSheet(onDismissRequest = { payslip = null }) {
                    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
                        Text("Payslip · ${e.name}", style = MaterialTheme.typography.titleLarge); Text("${e.position} · ${c.branchName(e.branchId)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = 10.dp)) { Text("Qo‘lga tegadi", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(e.net?.let { Fmt.money(it) } ?: "$MASK so‘m", style = MaterialTheme.typography.headlineMedium) }
                        fun n(v: Long?, sign: String = "") = if (v == null) MASK else if (v == 0L) "0" else sign + Fmt.num(v)
                        listOf(
                            Triple("Oklad × davomat koef.", if (e.oklad == null) MASK else Fmt.num(Math.round(e.oklad * (e.coef ?: 0.0))), if (e.oklad == null) "" else "${Fmt.num(e.oklad)} × ${fmtDec(e.coef)}"),
                            Triple("Shaxsiy KPI", n(e.kpi, "+"), "FOM sotuvlari asosida"), Triple("Bonus dorilar", n(e.bonus, "+"), "FOM bonus ro‘yxati"), Triple("Premiya", n(e.premium, "+"), "mudir taklifi"),
                            Triple("Jarimalar", n(e.penalties, "−"), if ((e.penalties ?: 0) > 0) (ev?.let { "${it.label} · ${it.conf}% · ${it.date}, ${it.time}" } ?: "qo‘lda kiritilgan") else "yo‘q"),
                            Triple("Soliq (JShDS)", n(e.tax, "−"), "gross − jarima"), Triple("Avans", n(e.advance, "−"), ""), Triple("Qarz", n(e.debt, "−"), if ((e.debt ?: 0) > 0) "tovar qarzi" else "—"),
                        ).forEach { (l, v, s) -> HorizontalDivider(); Row(Modifier.padding(vertical = 8.dp)) { Column(Modifier.weight(1f)) { Text(l); if (s.isNotBlank()) Text(s, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(v, fontWeight = FontWeight.SemiBold, color = if (l == "Jarimalar" && (e.penalties ?: 0) > 0) Danger else MaterialTheme.colorScheme.onSurface) } }
                        Text("Xodim shu payslipni mobil ilovada ko‘radi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                        OutlinedButton({ payslip = null; go("employee", e.id) }, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Xodim kartasi") }
                    }
                }
            }
            if (lockOpen) ConfirmDialog("Vedomostni tasdiqlash", "Jami to‘lov: ${money(tn)} · ${pe.size} xodim · ${brs.size} filial · e’tirozlar: ${d.ai.count { it.status == "disputed" }} ta. Tasdiqlangandan so‘ng o‘zgartirish qulflanadi.", "Tasdiqlash va qulflash", onDismiss = { lockOpen = false }) { c.repo.cmd("payroll.lock", buildJsonObject {}); toast = "Vedomost tasdiqlandi va qulflandi. Moliya admin to‘lovni belgilaydi."; reloadKey++; null }
            if (paidOpen) ConfirmDialog("To‘lovni belgilash", "Vedomost to‘langan deb belgilanadi va ${pe.size} xodimga payslip yuboriladi.", "To‘landi", onDismiss = { paidOpen = false }) { c.repo.cmd("payroll.markPaid", buildJsonObject {}); toast = "Vedomost to‘langan deb belgilandi. ${pe.size} xodimga payslip yuborildi."; reloadKey++; null }
        }
    }
}

// ═════════════ Oylik shablonlari va formula (desktop hr.js W16) ═════════════
private val VARS = listOf("oklad" to "Oklad", "coef" to "Koeffitsient", "kpi" to "KPI", "bonus" to "Bonus", "premium" to "Premiya", "penalties" to "Jarimalar", "advance" to "Avans", "debt" to "Qarz")
private val OPS = listOf("+" to "+", "-" to "−", "*" to "×", "/" to "÷", "(" to "(", ")" to ")")
private fun shown(t: FormulaToken) = if (t.kind == "op" || t.kind == "paren") OPS.firstOrNull { it.first == t.value }?.second ?: t.value else if (t.kind == "var") VARS.firstOrNull { it.first == t.value }?.second ?: t.value else t.value

data class RulesData(val tpls: List<PayrollTemplate>, val employees: List<EmployeeFull>)

@Composable
fun PayrollRulesScreen(c: AppContainer, me: Me) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { RulesData(c.repo.payrollRules().templates, runCatching { c.repo.employeesFull(false) }.getOrDefault(emptyList())) }
    var tplId by rememberSaveable { mutableStateOf<String?>(null) }
    var toks by remember { mutableStateOf<List<FormulaToken>?>(null) }
    var test by remember { mutableStateOf<PayrollTestResult?>(null) }
    var empId by rememberSaveable { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }; var reason by remember { mutableStateOf("") }; var newName by remember { mutableStateOf("") }; var numIn by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf<String?>(null) }
    val rl = me.role; val ro = Access.readOnly(rl)
    val canWrite = !ro && (rl == "OWNER" || rl == "FINANCE"); val canTest = !ro && rl in listOf("OWNER", "FINANCE", "HR"); val isOwner = !ro && rl == "OWNER"
    ScreenScaffold("Oylik shablonlari va formula", "Formula serverda tekshiriladi; faol versiya keyingi qayta hisobdan qo‘llanadi.") {
        DataBox(loader) { d ->
            if (d.tpls.isEmpty()) { EmptyBox("Shablon yo‘q", Modifier.fillMaxSize()); return@DataBox }
            val tpl = d.tpls.firstOrNull { it.id == tplId } ?: d.tpls.firstOrNull { it.active } ?: d.tpls.first()
            val base = (tpl.versions.firstOrNull { it.active } ?: tpl.versions.lastOrNull())?.formula ?: emptyList()
            val f = toks ?: base; val dirty = toks != null
            val te = d.employees.firstOrNull { it.id == empId } ?: d.employees.firstOrNull()
            fun set(n: List<FormulaToken>) { toks = n; test = null }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Seg(d.tpls.map { it.id to (it.name + (if (it.active) " · faol" else "")) }, tpl.id, { tplId = it; toks = null; test = null }) }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                item { AppCard {
                    Text("Formula · ${tpl.name}", style = MaterialTheme.typography.titleSmall); Text("token ustiga bosib o‘chiring · pastdan o‘zgaruvchi qo‘shing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (f.isEmpty()) Text("Formula bo‘sh", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        f.forEachIndexed { i, tk -> AssistChip(onClick = { if (canTest) set(f.filterIndexed { j, _ -> j != i }) }, label = { Text(shown(tk)) }, colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(containerColor = if (tk.kind == "var") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) }
                    }
                    if (canTest) {
                        val last = f.lastOrNull(); val needOp = last != null && (last.kind == "var" || last.kind == "num" || (last.kind == "paren" && last.value == ")"))
                        fun add(x: FormulaToken) = set(if (needOp && (x.kind == "var" || x.kind == "num" || (x.kind == "paren" && x.value == "("))) f + FormulaToken("op", "+") + x else f + x)
                        Text("Amallar:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { OPS.forEach { (v, l) -> OutlinedButton({ set(f + FormulaToken(if (v == "(" || v == ")") "paren" else "op", v)) }) { Text(l) } } }
                        Text("O‘zgaruvchilar:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { VARS.forEach { (v, l) -> OutlinedButton({ add(FormulaToken("var", v)) }) { Text("+ $l") } } }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            androidx.compose.material3.OutlinedTextField(numIn, { numIn = it }, label = { Text("Son") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedButton({ val v = numIn.replace(',', '.').trim(); if (!Regex("^\\d{1,12}(\\.\\d{1,6})?$").matches(v)) toast = "Son noto‘g‘ri" else { add(FormulaToken("num", v)); numIn = "" } }) { Text("Qo‘shish") }
                        }
                        if (dirty) androidx.compose.material3.TextButton({ toks = null; test = null }) { Text("Joriy versiyaga qaytarish") }
                    }
                } }
                item { AppCard {
                    Text("Formula test", style = MaterialTheme.typography.titleSmall)
                    if (d.employees.isNotEmpty()) PickField("Xodim", d.employees, te, { it.name }, { empId = it.id; test = null })
                    test?.let { t -> if (t.ok) { Text("Natija", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary); Text(Fmt.money(t.gross), style = MaterialTheme.typography.titleLarge); Text("Soliq ${Fmt.num(t.tax)} · qo‘lga ${Fmt.num(t.net)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); te?.net?.let { Text("Joriy vedomost: ${Fmt.money(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } else Text("Formula xato: ${t.error}", color = Danger) }
                    OutlinedButton(enabled = te != null && canTest, onClick = { scope.launch { try { val r = c.repo.cmd("payrollRules.test", buildJsonObject { put("formula", json.encodeToJsonElement(kotlinx.serialization.serializer<List<FormulaToken>>(), f)); put("employeeId", te!!.id) }); test = json.decodeFromJsonElement(PayrollTestResult.serializer(), r) } catch (x: Exception) { test = PayrollTestResult(false, error = x.userMessage().removePrefix("Formula xato: ")) } } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Testni ishga tushirish") }
                    if (canWrite) {
                        Field("Versiya izohi", note, { note = it }, hint = "Masalan: KPI ulushi oshirildi")
                        Button(enabled = dirty && test?.ok == true, onClick = { scope.launch { try { c.repo.cmd("payrollRules.saveVersion", buildJsonObject { put("templateId", tpl.id); put("formula", json.encodeToJsonElement(kotlinx.serialization.serializer<List<FormulaToken>>(), f)); put("note", note.trim()) }); toast = "Yangi versiya saqlandi. Faollashtirilgach keyingi qayta hisobdan amal qiladi."; toks = null; test = null; reloadKey++ } catch (x: Exception) { toast = x.userMessage() } } }, modifier = Modifier.fillMaxWidth()) { Text("Yangi versiya saqlash") }
                        if (test?.ok != true) Text("Saqlashdan oldin formulani xodimda server orqali sinab ko‘ring.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (!canTest) Text("Faqat ko‘rish rejimi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                item { AppCard { Text("KPI pog‘onalari (shaxsiy sotuvdan %)", style = MaterialTheme.typography.titleSmall); tpl.kpiTiers.forEach { x -> KeyValueRow(if (x.maxPct == null) "≥ ${fmtDec(x.minPct)}%" else "${fmtDec(x.minPct)}–${fmtDec(x.maxPct)}%", "${fmtDec(x.ratePct)}%") } } }
                item { AppCard {
                    Text("Versiyalar tarixi", style = MaterialTheme.typography.titleSmall)
                    tpl.versions.reversed().forEach { v -> HorizontalDivider(Modifier.padding(vertical = 6.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text("v${v.version} · ${v.note ?: "—"} · ${v.createdBy ?: ""}", style = MaterialTheme.typography.bodyMedium); Text(v.formula.joinToString(" ") { shown(it) } + (v.createdAt?.let { " · " + Fmt.date(it) } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (v.active && tpl.active) Badge("faol", Tone.Accent) else if (isOwner) OutlinedButton({ if (reason.trim().length < 3) toast = "Faollashtirish sababini yozing (≥ 3 belgi)" else scope.launch { try { c.repo.cmd("payrollRules.activate", buildJsonObject { put("templateId", tpl.id); put("version", v.version); put("reason", reason.trim()) }); toast = "Versiya faollashtirildi — keyingi qayta hisobdan qo‘llanadi."; reloadKey++ } catch (x: Exception) { toast = x.userMessage() } } }) { Text("Faollashtirish") } else Badge("nofaol", Tone.Neutral)
                    } }
                    if (isOwner) Field("Faollashtirish sababi (≥ 3 belgi)", reason, { reason = it })
                    Text("Faqat Egasi faollashtiradi; o‘zgarish audit jurnaliga maxfiy yozuv sifatida tushadi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                if (canWrite) item { AppCard { Field("Yangi shablon (joriy shablondan nusxa)", newName, { newName = it }); OutlinedButton(enabled = newName.isNotBlank(), onClick = { scope.launch { try { val r = c.repo.cmd("payrollRules.createTemplate", buildJsonObject { put("name", newName.trim()); put("baseTemplateId", tpl.id) }); toast = "«${newName.trim()}» shabloni yaratildi"; tplId = r.str("id"); newName = ""; reloadKey++ } catch (x: Exception) { toast = x.userMessage() } } }, modifier = Modifier.fillMaxWidth()) { Text("Yangi shablon") } } }
                sectionTitle("Jarima qoidalari")
                item { AppCard(padding = PaddingValues(0.dp)) { tpl.penaltyRules.forEachIndexed { i, x -> if (i > 0) HorizontalDivider(); ListRow("${x.code} · ${x.name}", listOf(x.basis ?: x.amount?.let { Fmt.money(it) } ?: "—", x.source ?: "").filter { it.isNotBlank() }.joinToString(" · ")) }; if (tpl.penaltyRules.isEmpty()) Text("Qoida yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) } }
                item { Text("AI hodisalari faqat inson tasdig‘idan keyin qo‘llanadi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
