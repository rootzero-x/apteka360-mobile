package uz.apteka360.app.ui.screens

import uz.apteka360.app.ui.components.findFragmentActivity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.Doctor
import uz.apteka360.app.data.DoctorSummary
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.Settings
import uz.apteka360.app.data.Supplier
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
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.MoneyField
import uz.apteka360.app.ui.components.SearchField
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.SwitchRow
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger
import uz.apteka360.app.ui.theme.Warning

private fun ms(n: Long?) = Fmt.moneyShortText(n)
private fun ms(n: Double?) = Fmt.moneyShortText(n?.let { Math.round(it) })
private fun dm(d: String?) = if (d.isNullOrBlank() || d.length < 10) "—" else "${d.substring(8, 10)}.${d.substring(5, 7)}"
private fun qty(n: Double?) = Fmt.num(Math.round((n ?: 0.0) * 100) / 100.0)
private fun pct2(r: Double?) = (Math.round((r ?: 0.0) * 10000) / 100.0).toString().replace('.', ',').removeSuffix(",0")
private fun dec(v: String): Double? = v.replace(" ", "").replace(',', '.').trim().toDoubleOrNull()

// ═════════════ Firmalar va to‘lovlar ═════════════
data class SupData(val list: List<Supplier>, val settings: Settings)
private fun supStatus(x: Supplier) = when { x.overdue > 0 -> "Muddati o‘tgan" to Tone.Danger; x.dueSoon > 0 -> "7 kunda to‘lov" to Tone.Warning; else -> "Muddatida" to Tone.Accent }
private data class Calc(val overdueCovered: Long, val discount: Long, val volumeBonus: Long, val penaltyAvoided: Long, val benefit: Long, val remainingDebt: Long)
private fun supplierCalc(s: Supplier, amount: Long): Calc {
    val oc = minOf(amount, s.overdue); val ee = minOf(maxOf(0, amount - oc), s.earlyBase)
    val disc = Math.round(ee * s.earlyRate); val vb = if (s.volumeRate > 0 && amount >= s.volumeFrom) Math.round(amount * s.volumeRate) else 0L
    val pa = Math.round(oc * s.penaltyDaily * 30)
    return Calc(oc, disc, vb, pa, disc + vb + pa, maxOf(0, s.debt - amount - disc))
}

@Composable
fun SuppliersScreen(c: AppContainer, me: Me, initialId: String?, go: (String, String?) -> Unit) {
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { SupData(c.repo.suppliers(), runCatching { c.repo.settings() }.getOrDefault(Settings())) }
    var q by rememberSaveable { mutableStateOf("") }; var sel by rememberSaveable { mutableStateOf(initialId) }; var form by remember { mutableStateOf<Supplier?>(null) }; var formOpen by remember { mutableStateOf(false) }
    val canEdit = !Access.readOnly(me.role) && me.role in listOf("OWNER", "SUPER_ADMIN", "FINANCE")
    ScreenScaffold("Firmalar va to‘lovlar", loader.data?.let { "${it.list.size} ta firma" }, fab = if (canEdit) Icons.Default.Add to { form = null; formOpen = true } else null) {
        DataBox(loader) { d ->
            val list = d.list
            if (list.isEmpty()) { EmptyBox("Firmalar yo‘q", Modifier.fillMaxSize()); return@DataBox }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Jami qarz", ms(list.sumOf { it.debt }), modifier = Modifier.weight(1f)); KpiCard("Muddati o‘tgan", ms(list.sumOf { it.overdue }), modifier = Modifier.weight(1f)) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("7 kunda to‘lanadi", ms(list.sumOf { it.dueSoon }), modifier = Modifier.weight(1f)); KpiCard("Kirim · 30 kun (FOM)", ms(list.sumOf { it.purchases30 }), modifier = Modifier.weight(1f)) } }
                if (Access.can("payables", me.role)) item { OutlinedButton({ go("payables", null) }, Modifier.fillMaxWidth()) { Text("To‘lov kalendari") } }
                item { SearchField(q, { q = it }, "Firma, STIR") }
                items(list.filter { q.isBlank() || "${it.name} ${it.inn}".contains(q, ignoreCase = true) }, key = { it.id }) { x ->
                    val (l, t) = supStatus(x); val use = if (x.creditLimit > 0) x.debt.toDouble() / x.creditLimit else 0.0
                    AppCard(Modifier.clickable { sel = x.id }, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(x.name, style = MaterialTheme.typography.titleSmall); Text("Qarz ${ms(x.debt)}" + (if (x.overdue > 0) " · o‘tgan ${ms(x.overdue)}" else ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Badge(l, t) }
                        Spacer(Modifier.height(6.dp)); BarLine((use * 100).toInt(), tone = if (use > 0.7) Tone.Warning else Tone.Accent)
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
            list.firstOrNull { it.id == sel }?.let { s -> SupplierDetail(c, me, s, d.settings, canEdit, onDismiss = { sel = null }, onEdit = { form = s; formOpen = true }) }
            if (formOpen) SupplierForm(c, form, onDismiss = { formOpen = false }, onSaved = { formOpen = false; reloadKey++ })
        }
    }
}

@Composable
private fun SupplierForm(c: AppContainer, s: Supplier?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val F = listOf("name" to "Firma nomi *", "inn" to "STIR *", "manager" to "Menejer", "phone" to "Telefon", "creditLimit" to "Kredit limiti", "terms" to "To‘lov sharti", "earlyRate" to "Erta to‘lov chegirmasi, %", "earlyBase" to "Erta to‘lov bazasi", "volumeRate" to "Hajm bonusi, %", "volumeFrom" to "Hajm bonusi chegarasi", "penaltyDaily" to "Kunlik penya, %")
    val nums = setOf("creditLimit", "earlyRate", "earlyBase", "volumeRate", "volumeFrom", "penaltyDaily")
    val init = mapOf("name" to (s?.name ?: ""), "inn" to (s?.inn ?: ""), "manager" to (s?.manager ?: ""), "phone" to (s?.phone ?: ""), "creditLimit" to (s?.creditLimit?.toString() ?: ""), "terms" to (s?.terms ?: ""), "earlyRate" to (s?.earlyRate?.toString() ?: ""), "earlyBase" to (s?.earlyBase?.toString() ?: ""), "volumeRate" to (s?.volumeRate?.toString() ?: ""), "volumeFrom" to (s?.volumeFrom?.toString() ?: ""), "penaltyDaily" to (s?.penaltyDaily?.toString() ?: ""))
    val vals = remember { androidx.compose.runtime.mutableStateMapOf<String, String>().apply { putAll(init) } }
    FormSheet(if (s == null) "Firma qo‘shish" else "Firmani tahrirlash", onDismiss = onDismiss, onSubmit = {
        val body = buildJsonObject {
            if (s != null) put("id", s.id)
            for ((k, l) in F) { val raw = (vals[k] ?: "").trim(); if (l.endsWith("*") && raw.isBlank()) return@FormSheet "Maydonni to‘ldiring: ${l.removeSuffix(" *")}"; if (raw.isBlank()) continue
                if (k in nums) { val n = dec(raw) ?: return@FormSheet "Maydonni to‘g‘ri to‘ldiring: $l"; put(k, n) } else put(k, raw) }
        }
        c.repo.cmd("suppliers.upsert", body); onSaved(); null
    }) { F.forEach { (k, l) -> Field(l, vals[k] ?: "", { vals[k] = it }, number = k in nums) } }
}

@Composable
private fun SupplierDetail(c: AppContainer, me: Me, s: Supplier, st: Settings, canEdit: Boolean, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val scope = rememberCoroutineScope()
    val canPay = !Access.readOnly(me.role) && me.role in listOf("OWNER", "FINANCE")
    val apX = st.approvalX.takeIf { it > 0 } ?: 2_000_000; val apY = st.approvalY.takeIf { it > 0 } ?: 10_000_000
    var amount by remember { mutableStateOf("50000000") }; var confirm by remember { mutableStateOf(false) }; var toast by remember { mutableStateOf<String?>(null) }
    val P = amount.toLongOrNull() ?: 0; val calc = supplierCalc(s, P); val toOwner = P > apY
    val (l, t) = supStatus(s)
    FormSheet(s.name, "STIR ${s.inn.ifBlank { "—" }} · ${s.manager.ifBlank { "—" }} · ${s.phone.ifBlank { "—" }}", primary = "Yopish", onDismiss = onDismiss, onSubmit = { onDismiss(); null }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Badge(l, t); Spacer(Modifier.weight(1f)); if (canEdit) TextButton(onEdit) { Text("Tahrirlash") } }
        KeyValueRow("Qarz", Fmt.money(s.debt)); KeyValueRow("Muddati o‘tgan", Fmt.money(s.overdue), Danger); KeyValueRow("Kredit limiti", Fmt.money(s.creditLimit))
        if (s.terms.isNotBlank()) InfoBar("Shartnoma shartlari", s.terms)
        Text("Qarz yoshi (aging)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        val agT = s.aging.sum().coerceAtLeast(1); val agC = listOf(MaterialTheme.colorScheme.primary, Color(0xFFD97706), Color(0xFFDC2626), Color(0xFF7D1A1F)); val agL = listOf("0–30 kun", "31–60 kun", "61–90 kun", "90+ kun")
        Row(Modifier.fillMaxWidth().height(10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(5.dp))) { s.aging.forEachIndexed { i, v -> if (v > 0) Box(Modifier.weight(v.toFloat() / agT).fillMaxSize().background(agC.getOrElse(i) { Color.Gray })) } }
        s.aging.forEachIndexed { i, v -> KeyValueRow(agL.getOrElse(i) { "" }, ms(v)) }
        toast?.let { InfoBar(it, tone = Tone.Accent) }
        HorizontalDivider(Modifier.padding(vertical = 10.dp)); Text("To‘lov kalkulyatori", style = MaterialTheme.typography.titleSmall)
        MoneyField("To‘lov summasi", amount, { amount = it })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("O‘tganini yopish" to s.overdue, "Chegirma maks." to (s.overdue + s.earlyBase), "To‘liq qarz" to s.debt).filter { it.second > 0 }.forEach { (lbl, v) -> androidx.compose.material3.AssistChip(onClick = { amount = "$v" }, label = { Text(lbl) }) } }
        InfoBar(if (P > 0) "Bugun ${Fmt.money(P)} to‘lasangiz" else "To‘lov summasini kiriting", if (P > 0) (if (calc.benefit > 0) "jami ${Fmt.money(calc.benefit)} foyda olasiz." else "qo‘shimcha foyda yo‘q.") else null, if (P > 0) Tone.Accent else Tone.Info)
        KeyValueRow("Muddati o‘tgan qarz yopiladi", Fmt.money(calc.overdueCovered)); KeyValueRow("Erta to‘lov chegirmasi (${pct2(s.earlyRate)}%)", if (calc.discount > 0) "+" + Fmt.money(calc.discount) else "0", MaterialTheme.colorScheme.primary)
        KeyValueRow("Hajm bonusi", if (calc.volumeBonus > 0) "+" + Fmt.money(calc.volumeBonus) else "0", MaterialTheme.colorScheme.primary); KeyValueRow("Oldi olingan penya (30 kun)", if (calc.penaltyAvoided > 0) "+" + Fmt.money(calc.penaltyAvoided) else "0", MaterialTheme.colorScheme.primary)
        KeyValueRow("Qolgan qarz", Fmt.money(calc.remainingDebt)); KeyValueRow("Yakuniy foyda", Fmt.money(calc.benefit), MaterialTheme.colorScheme.primary)
        Text("Tasdiqlovchi: " + (if (toOwner) "Egasi tasdig‘i (> ${ms(apY)})" else if (P > apX) "Moliya admin" else "Filial mudiri"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(enabled = canPay && P > 0, onClick = { confirm = true }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(if (canPay) "To‘lov yaratish" else "Faqat Egasi yoki Moliya") }
        HorizontalDivider(Modifier.padding(vertical = 10.dp)); Text("Ledger (kirim — FOM, to‘lov — qo‘lda)", style = MaterialTheme.typography.titleSmall)
        val ledger = s.ledger.ifEmpty { listOf(uz.apteka360.app.data.LedgerRow("—", "Nakladnoy (FOM)", "Kirim (FOM)", s.purchases30 / 2, 0)) }
        ledger.forEach { r -> KeyValueRow("${r.date} · ${r.doc}", (if (r.debit > 0) "D ${Fmt.num(r.debit)}" else "") + (if (r.credit > 0) " K ${Fmt.num(r.credit)}" else "")) }
    }
    val act = androidx.compose.ui.platform.LocalContext.current.findFragmentActivity()
    if (confirm) ConfirmDialog("To‘lov so‘rovini yaratish", "Firma: ${s.name} · Summa: ${Fmt.money(P)} · Manba: Bank hisobi · Foyda: ${Fmt.money(calc.benefit)} · Tasdiqlovchi: ${if (toOwner) "Egasi" else "Moliya admin"}. To‘lov tasdiqlangandan keyin bank orqali qo‘lda o‘tkaziladi.", "Tasdiqqa yuborish", onDismiss = { confirm = false }) {
        if (P > uz.apteka360.app.ui.components.BIOMETRIC_THRESHOLD) uz.apteka360.app.ui.components.Biometric.confirm(act, "To‘lovni biometrik tasdiqlang", "${s.name} · ${Fmt.money(P)}", required = true)?.let { return@ConfirmDialog it }
        c.repo.cmd("suppliers.createPayment", buildJsonObject { put("supplierId", s.id); put("amount", P); put("benefit", calc.benefit) }); toast = "To‘lov so‘rovi yaratildi va tasdiqqa yuborildi."; null
    }
}

// ═════════════ To‘lov kalendari ═════════════
@Composable
fun PayablesScreen(c: AppContainer, me: Me, initialVendor: String?, go: (String, String?) -> Unit) {
    var days by rememberSaveable { mutableStateOf(30) }; var tab by rememberSaveable { mutableStateOf("calendar") }; var all by rememberSaveable { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(days, reloadKey) { c.branchesCached(); c.api.getJson("/v1/payables/summary?days=$days") }
    var vendor by rememberSaveable { mutableStateOf(initialVendor) }; var settingsOpen by remember { mutableStateOf(false) }
    val canEdit = !Access.readOnly(me.role) && me.role in listOf("OWNER", "SUPER_ADMIN", "FINANCE")
    ScreenScaffold("To‘lov kalendari", "Qaysi firmaga qachon va qancha to‘lash kerak") {
        val err = loader.error.value
        if (loader.data == null && err is ApiException && (err.status == 503 || err.status == 404 || err.code == "module_disabled")) { Column(Modifier.padding(16.dp)) { InfoBar("To‘lov kalendari mavjud emas.", err.message, Tone.Warning) }; return@ScreenScaffold }
        DataBox(loader) { s ->
            if (s.bool("connected") != true) { Column(Modifier.padding(16.dp)) { InfoBar("FOM ulanmagan.", "FOM agenti ulanmagan yoki kirim nakladnoylari hali kelmagan. Tizim → FOM integratsiya sahifasida agentni ulang (agent v1.2+ kerak)."); if (Access.can("fom", me.role)) OutlinedButton({ go("fom", null) }, Modifier.padding(top = 8.dp)) { Text("FOM integratsiya") } }; return@DataBox }
            val T = s.child("totals"); val inflow = s.child("inflow"); val sets = s.child("settings")
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Firmalarga qarz", ms(T.dbl("debt")), modifier = Modifier.weight(1f), foot = "${T.long("vendors") ?: 0} ta firma"); KpiCard("Muddati o‘tgan", ms(T.dbl("overdue")), modifier = Modifier.weight(1f), foot = T.dbl("penaltyAccrued")?.takeIf { it > 0 }?.let { "penya ${ms(it)}" }) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("7 kunda to‘lanadi", ms(T.dbl("due7")), modifier = Modifier.weight(1f), foot = "30 kunda: ${ms(T.dbl("due30"))}"); KpiCard("Chegirma imkoni", ms(T.dbl("earlySaving")), modifier = Modifier.weight(1f), foot = "muddatidan oldin to‘lansa") } }
                item { KpiCard("Kunlik to‘lov imkoni", ms(inflow.dbl("daily")), foot = "tushumning ${inflow.txt("sharePct")}% · ${inflow.txt("basisDays")} kunlik o‘rtacha") }
                T.str("firstGap")?.let { fg -> item { InfoBar("${dm(fg)} dan pul yetmay qoladi.", "Muddatlar bo‘yicha to‘lansa, eng past nuqtada ${ms(-(T.dbl("minBalance") ?: 0.0))} yetishmaydi. «Tavsiya reja»da taqsimlash ko‘rsatilgan.", Tone.Danger) } }
                (T.long("unconfigured") ?: 0).takeIf { it > 0 }?.let { n -> item { InfoBar("$n ta firma: to‘lov shartlari kiritilmagan", "Ular uchun standart ${sets.txt("defaultTermDays")} kun olindi. Firmani bosib muddat, chegirma va penyani kiriting.", Tone.Warning) } }
                item { Row(verticalAlignment = Alignment.CenterVertically) { Seg(listOf("calendar" to "Kalendar", "vendors" to "Firmalar", "plan" to "Tavsiya reja"), tab, { tab = it }, Modifier.weight(1f)); if (canEdit) TextButton({ settingsOpen = true }) { Text("Sozlamalar") } } }
                item { Seg(listOf(14, 30, 60, 90).map { "$it" to "$it kun" }, "$days", { days = it.toInt() }) }
                when (tab) {
                    "calendar" -> { val cal = s.list("calendar"); val bal = s.list("projection").associate { (it.str("date") ?: "") to (it.dbl("balance") ?: 0.0) }
                        if (cal.isEmpty()) item { AppCard { Text("Bu davrda to‘lov yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        items(cal) { d -> val tot = (d.dbl("due") ?: 0.0) + (d.dbl("overdue") ?: 0.0); val b = bal[d.str("date")] ?: 0.0
                            if (tot > 0) AppCard(padding = PaddingValues(12.dp)) {
                                Row { Text(dm(d.str("date")), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Text(ms(tot), fontWeight = FontWeight.SemiBold, color = if ((d.dbl("overdue") ?: 0.0) > 0) Danger else MaterialTheme.colorScheme.onSurface) }
                                Text("prognoz qoldiq: ${ms(b)}" + (if ((d.dbl("overdue") ?: 0.0) > 0) " · muddati o‘tgan: ${ms(d.dbl("overdue"))}" else ""), style = MaterialTheme.typography.bodySmall, color = if (b < 0) Warning else MaterialTheme.colorScheme.onSurfaceVariant)
                                d.list("items").forEach { i -> HorizontalDivider(Modifier.padding(vertical = 4.dp)); Row(Modifier.clickable { vendor = i.str("vendorKey") }) { Column(Modifier.weight(1f)) { Text(i.str("vendor") ?: ""); Text("${i.long("invoices") ?: 0} ta nakladnoy" + ((i.dbl("early") ?: 0.0).takeIf { it > 0 }?.let { " · erta to‘lasa −${ms(it)}" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(ms(i.dbl("amount")), fontWeight = FontWeight.SemiBold) } }
                            } }
                        item { Text("Qizil — muddati o‘tgan, sariq qoldiq — prognozda pul yetmaydi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    "vendors" -> { item { SwitchRow("Qarzi yo‘qlarini ham ko‘rsatish", all, { all = it }) }
                        val rows = s.list("vendors").filter { all || (it.dbl("debt") ?: 0.0) > 0 }
                        if (rows.isEmpty()) item { AppCard { Text("Qarz yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        items(rows) { v -> val od = v.dbl("overdue") ?: 0.0; val d7 = v.dbl("due7") ?: 0.0
                            AppCard(Modifier.clickable { vendor = v.str("key") }, padding = PaddingValues(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Badge("●", if (od > 0) Tone.Danger else if (d7 > 0) Tone.Warning else Tone.Accent); Spacer(Modifier.padding(4.dp)); Text(v.str("name") ?: "", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Text(ms(v.dbl("debt")), fontWeight = FontWeight.SemiBold) }
                                Text(listOfNotNull(if (od > 0) "o‘tgan ${ms(od)} (${v.long("maxOverdueDays")} kun)" else null, if (d7 > 0) "7 kunda ${ms(d7)}" else null, v.str("nextDue")?.let { "keyingi ${dm(it)}" }, (v.dbl("earlySaving") ?: 0.0).takeIf { it > 0 }?.let { "chegirma ${ms(it)}" }).joinToString(" · ").ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (v.bool("configured") == true) "${v.long("termDays")} kun" + ((v.dbl("earlyRate") ?: 0.0).takeIf { it > 0 }?.let { " · ${pct2(it)}%/${v.long("earlyDays")}" } ?: "") + ((v.dbl("penaltyDaily") ?: 0.0).takeIf { it > 0 }?.let { " · penya ${pct2(it)}%" } ?: "") else "shart kiritilmagan", style = MaterialTheme.typography.bodySmall, color = if (v.bool("configured") == true) MaterialTheme.colorScheme.onSurfaceVariant else Warning)
                            } }
                    }
                    else -> { val p = s.child("plan")
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Rejada to‘lanadi", ms(p.dbl("paid")), modifier = Modifier.weight(1f)); KpiCard("Chegirmadan tejash", ms(p.dbl("saving")), modifier = Modifier.weight(1f)) } }
                        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Oldi olingan penya", ms(p.dbl("penaltyAvoided")), modifier = Modifier.weight(1f)); KpiCard("Muddatida to‘lanmay qoladi", ms(p.dbl("unpaidDue")), modifier = Modifier.weight(1f)) } }
                        item { Text("Har kuni kunlik to‘lov imkoni (${ms(inflow.dbl("daily"))}) qo‘shiladi, boshlang‘ich pul ${ms(sets.dbl("startCash"))}. Tartib: avval chegirma muddati tugayotganlar, keyin penyali muddati o‘tganlar, keyin boshqalar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        val rows = p.list("rows"); val REASON = mapOf("early" to ("Chegirma uchun" to Tone.Accent), "penalty" to ("Penya to‘xtatish" to Tone.Danger), "overdue" to ("Muddati o‘tgan" to Tone.Warning), "due" to ("Muddati keldi" to Tone.Neutral))
                        if (rows.isEmpty()) item { AppCard { Text("Bu davrda to‘lov rejasi yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                        items(rows) { r -> val k = REASON[r.str("reason")] ?: ((r.str("reason") ?: "") to Tone.Neutral); AppCard(Modifier.clickable { vendor = r.str("vendorKey") }, padding = PaddingValues(0.dp)) { ListRow("${dm(r.str("date"))} · ${r.str("vendor")}", "${r.str("number") ?: "—"} · ${dm(r.str("invoiceDate"))}" + ((r.dbl("saving") ?: 0.0).takeIf { it > 0 }?.let { " · tejaladi ${ms(it)}" } ?: ""), trailing = { Column(horizontalAlignment = Alignment.End) { Text(ms(r.dbl("amount")), fontWeight = FontWeight.SemiBold); Badge(k.first, k.second) } }) } }
                    }
                }
                item { Text("Qarz = FOM kirim nakladnoylari − kiritilgan to‘lovlar; to‘lov eng eski nakladnoyni yopadi. Oxirgi nakladnoy: ${s.str("lastIncome") ?: "—"}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { Spacer(Modifier.height(24.dp)) }
            }
            vendor?.let { k -> VendorSheet(c, me, k, onDismiss = { vendor = null }, onChanged = { reloadKey++ }) }
            if (settingsOpen) {
                var term by remember { mutableStateOf(sets.txt("defaultTermDays")) }; var cash by remember { mutableStateOf((sets.dbl("startCash")?.let { Math.round(kotlin.math.abs(it)) } ?: 0).toString()) }
                var auto by remember { mutableStateOf(sets.child("paySharePct") == null || sets.child("paySharePct") is kotlinx.serialization.json.JsonNull) }; var share by remember { mutableStateOf(sets.dbl("paySharePct")?.toString() ?: inflow.txt("costSharePct")) }
                FormSheet("To‘lov kalendari sozlamalari", "Prognoz va reja shu qiymatlar bilan hisoblanadi.", onDismiss = { settingsOpen = false }, onSubmit = {
                    val t = term.trim().toIntOrNull() ?: return@FormSheet "Muddatni kiriting."
                    c.api.request("PUT", "/v1/payables/settings", buildJsonObject { put("defaultTermDays", t); put("startCash", cash.toLongOrNull() ?: 0); if (auto) put("paySharePct", null as String?) else put("paySharePct", (dec(share) ?: 0.0).coerceIn(0.0, 100.0)) }); reloadKey++; null
                }) { Field("Standart to‘lov muddati, kun", term, { term = it }, number = true); MoneyField("Hozirgi bo‘sh pul (bank + kassa)", cash, { cash = it }); SwitchRow("Kunlik imkon = tushumning tannarx ulushi (avto, hozir ${inflow.txt("costSharePct")}%)", auto, { auto = it }); if (!auto) Field("Tushumning necha foizi firmalarga, %", share, { share = it }, number = true) }
            }
        }
    }
}

@Composable
private fun VendorSheet(c: AppContainer, me: Me, key: String, onDismiss: () -> Unit, onChanged: () -> Unit) {
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(key, reloadKey) { c.api.getJson("/v1/payables/vendors/${java.net.URLEncoder.encode(key, "UTF-8")}") }
    val ro = Access.readOnly(me.role); val canEdit = !ro && me.role in listOf("OWNER", "SUPER_ADMIN", "FINANCE"); val canPay = !ro && me.role in listOf("OWNER", "FINANCE")
    var amt by remember { mutableStateOf("") }; var date by remember { mutableStateOf(Fmt.isoDay()) }; var note by remember { mutableStateOf("") }; var toast by remember { mutableStateOf<String?>(null) }
    var del by remember { mutableStateOf<JsonElement?>(null) }; var termsOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val v = loader.data
    FormSheet(v?.str("name") ?: "Firma", v?.child("summary")?.let { sm -> "qarz ${ms(sm.dbl("debt"))} · 30 kunda olindi ${ms(sm.dbl("purchases30"))} · to‘landi ${ms(sm.dbl("paid30"))}" }, primary = "Yopish", onDismiss = onDismiss, onSubmit = { onDismiss(); null }) {
        if (v == null) { Text(loader.error.value?.userMessage() ?: "Yuklanmoqda…", color = MaterialTheme.colorScheme.onSurfaceVariant); return@FormSheet }
        toast?.let { InfoBar(it, tone = Tone.Accent) }
        if (canPay) { Text("To‘lov kiritish", style = MaterialTheme.typography.titleSmall); MoneyField("Summa", amt, { amt = it }); Field("Sana (yyyy-mm-dd)", date, { date = it }); Field("Izoh", note, { note = it }, hint = "to‘lov raqami, bank…")
            Button({ val n = amt.toLongOrNull() ?: 0; if (n <= 0 || date.isBlank()) toast = "Summa va sanani kiriting." else scope.launch { try { c.api.request("POST", "/v1/payables/payments", buildJsonObject { put("vendorKey", key); put("date", date); put("amount", n); put("note", note.trim()) }); toast = "To‘lov kiritildi"; amt = ""; note = ""; reloadKey++; onChanged() } catch (e: Exception) { toast = e.userMessage() } } }, Modifier.fillMaxWidth()) { Text("Saqlash") } }
        val t = v.child("terms")
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) { Text(if (v.bool("configured") == true) "To‘lov shartlari: ${t.long("termDays")} kun" else "shart kiritilmagan", color = if (v.bool("configured") == true) MaterialTheme.colorScheme.onSurface else Warning, modifier = Modifier.weight(1f)); if (canEdit) TextButton({ termsOpen = true }) { Text("Shartlar") } }
        val open = v.list("open")
        Text("Ochiq nakladnoylar (${open.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        if (open.isEmpty()) Text("Ochiq nakladnoy yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        open.forEach { i -> val dd = i.long("daysToDue") ?: 0; KeyValueRow((if (i.bool("synthetic") == true) "Boshlang‘ich qoldiq" else i.str("number") ?: "—") + " · ${dm(i.str("date"))} · " + (if (dd < 0) "${-dd} kun kechikkan" else "muddat ${dm(i.str("due"))} ($dd kun)"), ms(i.dbl("remaining")), if (dd < 0) Danger else MaterialTheme.colorScheme.onSurface) }
        val pays = v.list("payments")
        Text("To‘lovlar (${pays.size})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        if (pays.isEmpty()) Text("To‘lov kiritilmagan.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        pays.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(dm(p.str("date")) + (p.str("note")?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")); Text(p.str("by") ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(ms(p.dbl("amount")), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold); if (canPay) TextButton({ del = p }) { Text("O‘chirish") } } }
        Text("Oxirgi kirimlar (FOM)", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        val rc = v.list("receipts"); if (rc.isEmpty()) Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        rc.take(20).forEach { r -> KeyValueRow("${dm(r.str("date"))} · ${r.str("snumber") ?: r.str("number") ?: ""}", ms(r.dbl("sum"))) }
    }
    del?.let { p -> ConfirmDialog("To‘lovni o‘chirish", "${dm(p.str("date"))} · ${Fmt.money(p.dbl("amount"))}" + (p.str("note")?.let { " · $it" } ?: ""), "O‘chirish", danger = true, onDismiss = { del = null }) { c.api.request("DELETE", "/v1/payables/payments/${java.net.URLEncoder.encode(p.str("id") ?: "", "UTF-8")}"); toast = "To‘lov o‘chirildi"; reloadKey++; onChanged(); null } }
    if (termsOpen && v != null) {
        val t = v.child("terms")
        var termDays by remember { mutableStateOf(t.txt("termDays")) }; var cl by remember { mutableStateOf((t.dbl("creditLimit")?.let { Math.round(it) } ?: 0).toString()) }; var ep by remember { mutableStateOf(t.dbl("earlyRate")?.takeIf { it > 0 }?.let { pct2(it) } ?: "") }; var ed by remember { mutableStateOf(t.long("earlyDays")?.takeIf { it > 0 }?.toString() ?: "") }
        var pp by remember { mutableStateOf(t.dbl("penaltyDaily")?.takeIf { it > 0 }?.let { pct2(it) } ?: "") }; var od by remember { mutableStateOf(t.str("openingDate") ?: "") }; var op by remember { mutableStateOf((t.dbl("opening")?.let { Math.round(kotlin.math.abs(it)) } ?: 0).toString()) }
        var adv by remember { mutableStateOf((t.dbl("opening") ?: 0.0) < 0) }; var exc by remember { mutableStateOf(t.bool("excluded") == true) }
        FormSheet("To‘lov shartlari", v.str("name"), primary = "Shartlarni saqlash", onDismiss = { termsOpen = false }, onSubmit = {
            val td = termDays.trim().toIntOrNull() ?: return@FormSheet "To‘lov muddatini kiriting."
            c.api.request("PUT", "/v1/payables/vendors/${java.net.URLEncoder.encode(key, "UTF-8")}", buildJsonObject { put("termDays", td); put("earlyDays", ed.toIntOrNull() ?: 0); put("earlyRate", (dec(ep) ?: 0.0) / 100); put("penaltyDaily", (dec(pp) ?: 0.0) / 100); put("creditLimit", cl.toLongOrNull() ?: 0); put("opening", (op.toLongOrNull() ?: 0) * (if (adv) -1 else 1)); put("openingDate", od.ifBlank { null }); put("excluded", exc) })
            toast = "Shartlar saqlandi"; reloadKey++; onChanged(); null
        }) { Field("To‘lov muddati, kun", termDays, { termDays = it }, number = true); MoneyField("Kredit limiti", cl, { cl = it }); Field("Erta to‘lov chegirmasi, %", ep, { ep = it }, number = true); Field("Chegirma muddati, kun", ed, { ed = it }, number = true); Field("Penya, kuniga %", pp, { pp = it }, number = true); Field("Boshlang‘ich qoldiq sanasi (yyyy-mm-dd)", od, { od = it }, hint = "Bu sanadan oldingi nakladnoylar hisobga olinmaydi"); MoneyField("Boshlang‘ich qoldiq", op, { op = it }); SwitchRow("Bu avans (firma bizga qarzdor)", adv, { adv = it }); SwitchRow("Hisobdan chiqarish (ta’minotchi emas)", exc, { exc = it }) }
    }
}

// ═════════════ Vrachlar va ROI ═════════════
private fun roiTone(r: Long) = if (r >= 100) Tone.Accent else if (r >= 0) Tone.Warning else Tone.Danger
private fun fmtRoi(r: Long) = (if (r > 0) "+" else if (r < 0) "−" else "") + kotlin.math.abs(r) + "%"
data class DocData(val docs: List<Doctor>, val sums: List<DoctorSummary>)

@Composable
fun DoctorsScreen(c: AppContainer, me: Me, initialId: String?) {
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { DocData(c.api.getAs("/v1/doctors"), runCatching { c.api.getAs<List<DoctorSummary>>("/v1/doctorSummaries") }.getOrDefault(emptyList())) }
    var sel by rememberSaveable { mutableStateOf(initialId) }; var formOpen by remember { mutableStateOf(false) }; var formDoc by remember { mutableStateOf<Doctor?>(null) }; var closeOpen by remember { mutableStateOf(false) }
    val canEdit = !Access.readOnly(me.role) && me.role in listOf("OWNER", "FINANCE")
    val viewed = remember { mutableSetOf<String>() }
    val scope = rememberCoroutineScope()
    ScreenScaffold("Vrachlar bilan kelishuvlar", "MAXFIY · har ko‘rish auditga yoziladi", fab = if (canEdit) Icons.Default.Add to { formDoc = null; formOpen = true } else null) {
        DataBox(loader) { d ->
            val docs = d.docs
            if (docs.isEmpty()) { EmptyBox("Kelishuvlar yo‘q", Modifier.fillMaxSize()); return@DataBox }
            val act = docs.filter { it.status != "closed" }; val tc = act.sumOf { it.cost }; val tp = act.sumOf { it.profit }
            val review = d.sums.count { it.negMonths >= 2 }.takeIf { it > 0 } ?: act.count { it.months.size >= 2 && it.months.takeLast(2).all { m -> m < 0 } }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Faol kelishuvlar", "${act.size}", modifier = Modifier.weight(1f), foot = (docs.size - act.size).takeIf { it > 0 }?.let { "$it ta yopilgan" }); KpiCard("Vrach xarajati / oy", ms(tc), modifier = Modifier.weight(1f)) } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Qo‘shimcha yalpi foyda", ms(tp), modifier = Modifier.weight(1f)); KpiCard("O‘rtacha ROI", if (tc > 0) fmtRoi(Math.round((tp - tc).toDouble() / tc * 100)) else "—", modifier = Modifier.weight(1f), foot = if (review > 0) "$review ta qayta ko‘rish kerak" else null) } }
                items(docs, key = { it.id }) { x -> AppCard(Modifier.clickable { sel = x.id; if (viewed.add(x.id)) scope.launch { c.repo.auditLog("Vrach kelishuvi ko‘rildi", x.name, null) } }, padding = PaddingValues(0.dp)) { ListRow(x.name, "${x.clinic} · ${x.spec} · ${x.owner} · ${x.model}", trailing = { Column(horizontalAlignment = Alignment.End) { Text(Fmt.num(x.profit), fontWeight = FontWeight.SemiBold); if (x.status == "closed") Badge("Yopilgan", Tone.Neutral) else Badge(fmtRoi(x.roi), roiTone(x.roi)) } }) } }
                item { Spacer(Modifier.height(72.dp)) }
            }
            docs.firstOrNull { it.id == sel }?.let { dd ->
                val net = dd.profit - dd.cost; val closed = dd.status == "closed"; val rec = dd.months.size >= 2 && dd.months.takeLast(2).all { it < 0 }; val pf = if (dd.plan > 0) (dd.fact * 100 / dd.plan).toInt() else 0
                FormSheet(dd.name, listOf(dd.clinic, dd.spec, dd.region).filter { it.isNotBlank() }.joinToString(" · "), primary = "Yopish", onDismiss = { sel = null }, onSubmit = { sel = null; null }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { if (closed) Badge("Yopilgan", Tone.Neutral) else Badge("ROI " + fmtRoi(dd.roi), roiTone(dd.roi)); Spacer(Modifier.weight(1f)); if (canEdit && !closed) { TextButton({ formDoc = dd; formOpen = true }) { Text("Tahrirlash") }; TextButton({ closeOpen = true }) { Text("Yopish", color = Danger) } } }
                    if (closed) InfoBar("Kelishuv yopilgan.", (dd.closedAt?.let { Fmt.date(it) + " · " } ?: "") + (dd.closeReason ?: ""))
                    KeyValueRow("Kelishuv davri", if (dd.startDate.isNotBlank()) Fmt.date(dd.startDate) + " dan" else "—"); KeyValueRow("To‘lov modeli", dd.model); KeyValueRow("Dorilar", dd.meds); KeyValueRow("Atributsiya", dd.method); KeyValueRow("Mas’ul · filial", "${dd.owner.ifBlank { "—" }} · ${dd.branches.ifBlank { "—" }}")
                    Text("Reja / haqiqiy (attributsiya qilingan sotuv)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)); BarLine(minOf(100, pf), tone = Tone.Accent); Text("${Fmt.money(dd.fact)} / ${Fmt.money(dd.plan)} · $pf%", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp)); KeyValueRow("Qo‘shimcha yalpi foyda", Fmt.money(dd.profit)); KeyValueRow("Vrach xarajati", Fmt.money(dd.cost)); KeyValueRow("Sof natija", Fmt.money(net), if (net >= 0) MaterialTheme.colorScheme.primary else Danger)
                    if (dd.months.isNotEmpty()) { Text("ROI · oxirgi oylar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)); val ml = listOf("Avgust", "Sentyabr", "Oktabr").takeLast(dd.months.size); dd.months.forEachIndexed { i, v -> KeyValueRow(ml.getOrElse(i) { "" }, fmtRoi(v), if (v >= 100) MaterialTheme.colorScheme.primary else if (v >= 0) Warning else Danger) } }
                    if (rec && !closed) InfoBar("Kelishuvni qayta ko‘rib chiqish tavsiya etiladi.", "ROI 2 oy ketma-ket salbiy. Qaror Egasi tomonidan qabul qilinadi.", Tone.Danger)
                }
                if (closeOpen) ConfirmDialog("Kelishuvni yopish", "${dd.name} — kelishuv yopiladi, yozuv ro‘yxatda qoladi.", "Yopish", danger = true, reasonLabel = "Sabab", minReason = 3, onDismiss = { closeOpen = false }) { r -> c.repo.cmd("doctors.close", buildJsonObject { put("id", dd.id); put("reason", r) }); sel = null; reloadKey++; null }
            }
            if (formOpen) DoctorForm(c, formDoc, onDismiss = { formOpen = false }, onSaved = { formOpen = false; reloadKey++ })
        }
    }
}

@Composable
private fun DoctorForm(c: AppContainer, d: Doctor?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val vals = remember { androidx.compose.runtime.mutableStateMapOf("name" to (d?.name ?: ""), "clinic" to (d?.clinic ?: ""), "spec" to (d?.spec ?: ""), "region" to (d?.region ?: ""), "owner" to (d?.owner ?: ""), "model" to (d?.model ?: ""), "monthlyCost" to (d?.cost?.toString() ?: ""), "plan" to (d?.plan?.toString() ?: ""), "startDate" to (d?.startDate?.ifBlank { Fmt.isoDay() } ?: Fmt.isoDay()), "branches" to (d?.branches ?: ""), "meds" to (d?.meds ?: "")) }
    val req = listOf("name" to "Vrach F.I.Sh.", "clinic" to "Klinika", "spec" to "Mutaxassislik", "region" to "Hudud", "owner" to "Mas’ul", "model" to "To‘lov modeli", "startDate" to "Boshlanish sanasi")
    FormSheet(if (d == null) "Yangi kelishuv" else "Kelishuvni tahrirlash", onDismiss = onDismiss, onSubmit = {
        for ((k, l) in req) if ((vals[k] ?: "").isBlank()) return@FormSheet "Majburiy maydon: $l"
        c.repo.cmd("doctors.upsert", buildJsonObject { if (d != null) put("id", d.id); req.forEach { (k, _) -> put(k, vals[k]!!.trim()) }; put("monthlyCost", vals["monthlyCost"]?.toLongOrNull() ?: 0); put("plan", vals["plan"]?.toLongOrNull() ?: 0); vals["branches"]?.takeIf { it.isNotBlank() }?.let { put("branches", it) }; vals["meds"]?.takeIf { it.isNotBlank() }?.let { put("meds", it) } }); onSaved(); null
    }) {
        req.forEach { (k, l) -> Field("$l *", vals[k] ?: "", { vals[k] = it }) }
        MoneyField("Xarajat/oy *", vals["monthlyCost"] ?: "", { vals["monthlyCost"] = it }); MoneyField("Reja (oyiga sotuv) *", vals["plan"] ?: "", { vals["plan"] = it })
        Field("Filiallar", vals["branches"] ?: "", { vals["branches"] = it }); Field("Dorilar", vals["meds"] ?: "", { vals["meds"] = it }, minLines = 2)
    }
}

// ═════════════ Sroklar ═════════════
@Composable
fun ExpiryScreen(c: AppContainer, me: Me) {
    var tab by rememberSaveable { mutableStateOf("loss") }; var branch by rememberSaveable { mutableStateOf(if (me.role == "MANAGER") me.branchId ?: "" else "") }
    var preset by rememberSaveable { mutableStateOf("d30") }; var horizon by rememberSaveable { mutableStateOf(60) }
    var from by rememberSaveable { mutableStateOf(Fmt.isoDay(29)) }; var to by rememberSaveable { mutableStateOf(Fmt.isoDay()) }
    val bq = if (branch.isNotBlank()) "&branchId=$branch" else ""
    val loader = rememberLoader(tab, branch, from, to, horizon) { c.branchesCached(); c.api.getJson(if (tab == "loss") "/v1/expiry/writeoffs?from=$from&to=$to$bq" else "/v1/expiry/near?horizon=$horizon$bq") }
    fun bn(id: String?) = if (id == null) "—" else c.branchName(id)
    ScreenScaffold("Sroklar", "Srogi o‘tgan dorilar zarari va srogi yaqin partiyalarni ko‘chirish — FOM asosida") {
        DataBox(loader) { d ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Seg(listOf("loss" to "Srok zarari", "near" to "Srogi yaqin va ko‘chirish"), tab, { tab = it }) }
                if (!(me.role == "MANAGER" && me.branchId != null)) item { Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, branch, { branch = it }) }
                if (tab == "loss") item { Seg(listOf("d7" to "7 kun", "d30" to "30 kun", "d90" to "90 kun", "d365" to "1 yil", "month" to "Bu oy"), preset, { p -> preset = p; to = Fmt.isoDay(); from = if (p == "month") to.take(8) + "01" else Fmt.isoDay(mapOf("d7" to 6, "d30" to 29, "d90" to 89, "d365" to 364)[p] ?: 29) }) }
                else item { Seg(listOf(30, 60, 90, 120).map { "$it" to "$it kun" }, "$horizon", { horizon = it.toInt() }) }
                val connected = d.bool("connected") == true
                if (tab == "loss") {
                    if (!connected) { item { InfoBar("FOM ulanmagan.", "FOM agenti ulanmagan yoki sroklar ma’lumoti hali kelmagan (agent v1.1+ kerak).") }; return@LazyColumn }
                    val t = d.child("totals")
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Srok sababli zarar", ms(t.dbl("expiredCost")), modifier = Modifier.weight(1f)); KpiCard("Jami yozib tashlangan", ms(t.dbl("cost")), modifier = Modifier.weight(1f)) } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Srok sababli, dona", qty(t.dbl("expiredQty")), modifier = Modifier.weight(1f)); KpiCard("Yo‘qolgan savdo (chakana)", ms(t.dbl("retail")), modifier = Modifier.weight(1f)) } }
                    item { Text("Zarar tannarx bo‘yicha. «Srok sababli» — partiya srogi yozib tashlash sanasidan 7 kun ichida tugagan. Srogi topilmagan: ${qty(t.dbl("unknownQty"))} dona.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    val byBranch = d.list("byBranch")
                    if (byBranch.isEmpty()) { item { AppCard { Text("Bu oraliqda yozuv yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }; return@LazyColumn }
                    item { AppCard { Text("Filiallar bo‘yicha zarar", style = MaterialTheme.typography.titleSmall); HBars(byBranch.map { bn(it.str("branchId")) to Math.round(it.dbl("expiredCost") ?: 0.0) }, { ms(it) }) } }
                    sectionTitle("Eng ko‘p zarar keltirgan dorilar")
                    items(d.list("topGoods")) { g -> AppCard(padding = PaddingValues(0.dp)) { ListRow(g.str("good") ?: "", "${bn(g.str("branchId"))} · ${qty(g.dbl("qty"))} dona", trailing = { Column(horizontalAlignment = Alignment.End) { Text(ms(g.dbl("cost")), fontWeight = FontWeight.SemiBold); (g.dbl("expiredCost") ?: 0.0).takeIf { it > 0 }?.let { Text("srok: ${ms(it)}", color = Danger, style = MaterialTheme.typography.bodySmall) } } }) } }
                } else {
                    if (!connected || d.str("stockAt") == null) { item { InfoBar("FOM ulanmagan.", "FOM agenti ulanmagan yoki qoldiq ma’lumoti hali kelmagan (agent v1.1+ kerak).") }; return@LazyColumn }
                    val n = d.child("totals")
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Srogi yaqin tovar", ms(n.dbl("value")), modifier = Modifier.weight(1f), foot = "$horizon kun"); KpiCard("Tokchada srogi o‘tgan", ms(n.dbl("expiredOnShelfCost")), modifier = Modifier.weight(1f)) } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Sotilmay qolish xavfi", ms(n.dbl("atRiskCost")), modifier = Modifier.weight(1f)); KpiCard("Ko‘chirib saqlanadi", ms(n.dbl("savableCost")), modifier = Modifier.weight(1f)) } }
                    item { Text("Prognoz: oxirgi ${d.long("lookbackDays")} kunlik sotuv tezligi bilan. Ko‘chirishga ${d.long("transitDays")} kun yo‘l hisobga olingan. Ma’lumot: ${Fmt.dateTime(d.str("stockAt"))}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    sectionTitle("Ko‘chirish tavsiyalari")
                    val tr = d.list("transfers")
                    if (tr.isEmpty()) item { AppCard { Text("Hozircha foydali ko‘chirish yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    items(tr.take(30)) { x -> AppCard(padding = PaddingValues(0.dp)) { ListRow("${x.str("good")} · ${bn(x.str("fromBranchId"))}", "${x.str("expiry")} (${x.long("daysLeft")} kun) · ${qty(x.dbl("qty"))} dona → ${bn(x.str("toBranchId"))} · u yerda kuniga ${x.txt("toPerDay")} · ~${x.long("sellDays")} kunda sotiladi", trailing = { Badge("saqlanadi ${ms(x.dbl("savedCost"))}", Tone.Accent) }) } }
                    sectionTitle("Partiyalar")
                    val items = d.list("items")
                    if (items.isEmpty()) item { AppCard { Text("Bu muddat ichida srogi tugaydigan partiya yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    items(items) { x -> val st = x.str("status"); AppCard(padding = PaddingValues(12.dp)) {
                        Row { Column(Modifier.weight(1f)) { Text(x.str("good") ?: "", style = MaterialTheme.typography.titleSmall); Text(listOfNotNull(x.str("series"), bn(x.str("branchId"))).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Badge(if (st == "expired") "Srogi o‘tgan" else "${x.long("daysLeft")} kun", if (st == "expired") Tone.Danger else if (st == "critical") Tone.Warning else Tone.Neutral) }
                        Text("Srogi ${x.str("expiry")} · ${qty(x.dbl("qty"))} dona · kuniga ${x.txt("perDay")} · sotiladi ${qty(x.dbl("sellable"))}" + ((x.dbl("atRiskCost") ?: 0.0).takeIf { it > 0 }?.let { " · xavf ${ms(it)}" } ?: ""), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        if ((x.dbl("atRiskQty") ?: 0.0) == 0.0) Text("O‘zi sotilib ketadi", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        x.list("transfers").forEach { t2 -> Text("${qty(t2.dbl("qty"))} dona → ${bn(t2.str("toBranchId"))}", style = MaterialTheme.typography.bodySmall) }
                        (x.dbl("leftoverQty") ?: 0.0).takeIf { it > 0 }?.let { Text("${qty(it)} dona qoladi: chegirma yoki ta’minotchiga qaytarish", color = Warning, style = MaterialTheme.typography.bodySmall) }
                    } }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
