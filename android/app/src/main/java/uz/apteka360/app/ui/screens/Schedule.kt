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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.EmployeeFull
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.SwapRow
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.ConfirmDialog
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.SearchField
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger
import uz.apteka360.app.ui.theme.Info
import uz.apteka360.app.ui.theme.Warning

// ═════════════ Ish grafigi (desktop hr.js W07) ═════════════
private val WEEK = listOf("Du 12", "Se 13", "Ch 14", "Pa 15", "Ju 16", "Sh 17", "Ya 18")
private val ORDER = listOf("D", "N", "F", "X", "T")
private data class ShiftKind(val label: String, val name: String, val bg: Color, val fg: Color)
private val SHIFT = mapOf(
    "D" to ShiftKind("08–20", "Kunduzgi (12 s)", Color(0x1F1B8A55), Color(0xFF167346)),
    "N" to ShiftKind("20–08", "Tungi (12 s)", Color(0x1F4F8CFF), Info),
    "F" to ShiftKind("09–18", "Standart (9 s)", Color(0x14888888), Color(0xFF26302B)),
    "X" to ShiftKind("Dam", "Dam olish", Color.Transparent, Color(0xFF8A928D)),
    "T" to ShiftKind("Ta’til", "Ta’til / kasal", Color(0x2EE2A336), Warning),
)
private fun defaultRow(e: EmployeeFull): List<String> = when {
    e.status == "ta’til" || e.status == "kasal" -> List(7) { "T" }
    e.schedule.startsWith("1/2") -> listOf("D", "X", "X", "D", "X", "X", "D")
    e.schedule.startsWith("5/2") -> listOf("F", "F", "F", "F", "F", "X", "X")
    else -> listOf("D", "D", "X", "X", "D", "D", "X")
}
private fun hoursOf(row: List<String>) = row.sumOf { when (it) { "D", "N" -> 12; "F" -> 9; else -> 0 }.toInt() }

data class ScheduleData(val employees: List<EmployeeFull>, val sched: Map<String, List<String>>, val swaps: List<SwapRow>?)

@Composable
fun ScheduleScreen(c: AppContainer, me: Me, initialBranch: String?, go: (String, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val loader = rememberLoader { c.branchesCached(); ScheduleData(c.repo.employeesFull(false), c.repo.schedule(), runCatching { c.repo.swaps() }.getOrNull()) }
    var bid by rememberSaveable { mutableStateOf(initialBranch ?: "") }
    var sched by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var swaps by remember { mutableStateOf<List<SwapRow>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var fillConfirm by remember { mutableStateOf(false) }
    LaunchedEffect(loader.data) { loader.data?.let { sched = it.sched; swaps = it.swaps } }
    val edit = !Access.readOnly(me.role) && me.role in listOf("OWNER", "MANAGER", "HR")
    val canDecide = edit
    val branches = c.branches
    val b = branches.firstOrNull { it.id == bid } ?: branches.firstOrNull { it.id == me.branchId } ?: branches.firstOrNull()
    val emps = loader.data?.employees?.filter { it.branchId == b?.id } ?: emptyList()
    fun rowOf(e: EmployeeFull) = sched[e.id] ?: defaultRow(e)
    val coverage = (0 until 7).map { i -> emps.count { rowOf(it)[i] in listOf("D", "F") } to emps.count { rowOf(it)[i] == "N" } }
    val bad = WEEK.filterIndexed { i, _ -> coverage[i].first < 2 }
    fun setCell(e: EmployeeFull, i: Int, code: String) { busy = true; scope.launch {
        try { val r = c.repo.cmd("schedule.setCell", buildJsonObject { put("empId", e.id); put("day", i); put("code", code) })
            val row = runCatching { r.jsonArray.map { it.jsonPrimitive.content } }.getOrNull() ?: rowOf(e).toMutableList().also { it[i] = code }
            sched = sched + (e.id to row)
        } catch (x: Exception) { toast = x.userMessage() } finally { busy = false } } }
    ScreenScaffold("Ish grafigi" + (b?.let { " · ${it.label}" } ?: ""), "12–18 oktabr 2026 · katakni bosib smena turini almashtiring") {
        DataBox(loader) { _ ->
            if (b == null) { EmptyBox("Hali filial yo‘q — Filiallar bo‘limida qo‘shing", Modifier.fillMaxSize()); return@DataBox }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (branches.size > 1) item { Seg(branches.map { it.id to it.label }, b.id, { bid = it }) }
                toast?.let { item { InfoBar(it, tone = Tone.Danger) } }
                if (bad.isNotEmpty()) item { InfoBar("Minimal 2 xodim talabi bajarilmaydi.", "${b.name} · ${bad.joinToString(", ")}, 08:00–20:00. Grafikni saqlash mumkin, lekin mudir va HR’ga xabar boradi.", Tone.Warning) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) { ORDER.forEach { k -> val s = SHIFT[k]!!; Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.background(s.bg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) { Text(s.label, color = s.fg, style = MaterialTheme.typography.labelMedium) }; Spacer(Modifier.width(4.dp)); Text(s.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } } }
                if (!edit && !Access.readOnly(me.role)) item { InfoBar("Faqat ko‘rish.", "Grafikni Egasi, filial mudiri yoki HR o‘zgartiradi.") }
                if (emps.isEmpty()) item { AppCard { Text("Bu filialda xodim yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(emps, key = { it.id }) { e ->
                    val row = rowOf(e)
                    AppCard(padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { go("employee", e.id) }) { Text(e.name, style = MaterialTheme.typography.titleSmall); Text("${e.position} · ${e.schedule}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text("${hoursOf(row)} s", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEachIndexed { i, code ->
                                val s = SHIFT[code] ?: SHIFT["X"]!!
                                Column(Modifier.weight(1f).background(s.bg, RoundedCornerShape(8.dp)).then(if (edit && !busy) Modifier.clickable { setCell(e, i, ORDER[(ORDER.indexOf(code) + 1) % ORDER.size]) } else Modifier).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(WEEK[i].substringBefore(' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(s.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = s.fg, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                item { AppCard(padding = PaddingValues(12.dp)) { Text("Qamrov (kunduz / tun) · min 2", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) { coverage.forEachIndexed { i, (d, n) -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(WEEK[i].substringBefore(' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("$d / $n", fontWeight = FontWeight.SemiBold, color = if (d < 2) Danger else MaterialTheme.colorScheme.onSurface) } } } } }
                if (edit) item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy, onClick = { fillConfirm = true }, modifier = Modifier.weight(1f)) { Text("Shablondan to‘ldirish", maxLines = 1) }
                    Button(enabled = !busy, onClick = { busy = true; scope.launch { try { c.repo.cmd("schedule.save", buildJsonObject { put("branchId", b.id); put("branchName", b.name) }); toast = if (bad.isNotEmpty()) "Grafik saqlandi. Minimal xodim talabi bo‘yicha ogohlantirish mudirga yuborildi." else "Grafik saqlandi." } catch (x: Exception) { toast = x.userMessage() } finally { busy = false } } }, modifier = Modifier.weight(1f)) { Text("Saqlash") }
                } }
                swaps?.let { sw ->
                    sectionTitle("Smena almashish so‘rovlari")
                    if (sw.isEmpty()) item { AppCard { Text("So‘rov yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    items(sw, key = { "sw-" + it.id }) { w ->
                        AppCard(padding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text("${w.from} → ${w.to} · ${w.day} · ${w.shift}", style = MaterialTheme.typography.titleSmall); Text("Sabab: ${w.note.ifBlank { "—" }} · mobil ilovadan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                Badge(when (w.status) { "pending" -> "Kutilmoqda"; "approved" -> "Tasdiqlangan"; "rejected" -> "Rad etilgan"; else -> w.status }, when (w.status) { "pending" -> Tone.Warning; "approved" -> Tone.Accent; "rejected" -> Tone.Danger; else -> Tone.Neutral })
                            }
                            if (w.status == "pending" && canDecide) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                listOf("approved" to "Tasdiqlash", "rejected" to "Rad etish").forEach { (st, l) ->
                                    OutlinedButton(enabled = !busy, onClick = { busy = true; scope.launch { try { c.repo.cmd("swaps.decide", buildJsonObject { put("id", w.id); put("status", st) }); swaps = sw.map { if (it.id == w.id) it.copy(status = st) else it }; sched = runCatching { c.repo.schedule() }.getOrDefault(sched); toast = if (st == "approved") "Smena almashish tasdiqlandi. Ikkala xodimga bildirish yuborildi." else "Smena almashish rad etildi." } catch (x: Exception) { toast = x.userMessage() } finally { busy = false } } }, modifier = Modifier.weight(1f)) { Text(l) }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
            if (fillConfirm) {
                val jobs = emps.flatMap { e -> val want = defaultRow(e); val cur = rowOf(e); want.mapIndexedNotNull { i, code -> if (cur[i] != code) Triple(e, i, code) else null } }
                ConfirmDialog("Shablondan to‘ldirish", if (jobs.isEmpty()) "Grafik shablon bilan bir xil." else "${b.name}: ${jobs.size} ta katak xodimning grafik turi (2/2, 5/2, 1/2) bo‘yicha qayta to‘ldiriladi.", "To‘ldirish", onDismiss = { fillConfirm = false }) {
                    for ((e, i, code) in jobs) { val r = c.repo.cmd("schedule.setCell", buildJsonObject { put("empId", e.id); put("day", i); put("code", code) }); sched = sched + (e.id to (runCatching { r.jsonArray.map { it.jsonPrimitive.content } }.getOrNull() ?: rowOf(e).toMutableList().also { it[i] = code })) }
                    toast = "Grafik shablondan to‘ldirildi. Saqlashni unutmang."; null
                }
            }
        }
    }
}

// ═════════════ Davomat (desktop hr.js W08) ═════════════
private data class TlItem(val t: String, val code: String, val label: String, val meta: String, val tone: Tone, val evidence: Boolean = false)
private val TIMELINE = listOf(
    TlItem("07:56", "ARRIVED", "Keldi", "CAM-01 · Kirish · yuz 98%", Tone.Accent), TlItem("08:02", "ZONE_ENTER", "Kassa zonasiga kirdi", "CAM-03 · Kassa 1", Tone.Neutral),
    TlItem("11:40", "ZONE_EXIT", "Kassa zonasidan chiqdi", "Zal → Ombor · 6 daq", Tone.Neutral), TlItem("13:00", "BREAK", "Tanaffus", "13:00–13:30 · rejada", Tone.Info),
    TlItem("14:07", "IDLE", "Telefon ishlatish (AI)", "12 daq · 92% · tekshiruv kutilmoqda", Tone.Warning, true), TlItem("16:20", "ZONE_ENTER", "Zal", "CAM-05 · mijoz bilan", Tone.Neutral),
    TlItem("20:04", "LEFT", "Ketdi", "CAM-01 · Kirish", Tone.Accent),
)

@Composable
fun AttendanceScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val loader = rememberLoader { c.branchesCached(); c.repo.employeesFull(false) }
    var q by rememberSaveable { mutableStateOf("") }; var br by rememberSaveable { mutableStateOf("") }; var sel by rememberSaveable { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val canFix = !Access.readOnly(me.role) && me.role in listOf("OWNER", "MANAGER", "HR")
    val list = (loader.data ?: emptyList()).filter { (br.isBlank() || it.branchId == br) && (q.isBlank() || it.name.contains(q, ignoreCase = true)) }
    val ae = list.firstOrNull { it.id == sel } ?: list.firstOrNull()
    ScreenScaffold("Davomat · oktabr 2026", "Manba: kamera (yuz) · zaxira: mobil check-in") {
        DataBox(loader) { all ->
            if (all.isEmpty()) { EmptyBox("Hali xodim yo‘q — Xodimlar bo‘limida qo‘shing yoki import qiling", Modifier.fillMaxSize()); return@DataBox }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SearchField(q, { q = it }, "Xodim qidirish") }
                if (c.branches.size > 1) item { Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, br, { br = it }) }
                item { Text(LEGEND.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                items(list, key = { it.id }) { e ->
                    AppCard(Modifier.clickable { sel = e.id }, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(e.name, style = MaterialTheme.typography.titleSmall, color = if (ae?.id == e.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface); Text(shortBranch(c.branchName(e.branchId)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(e.attendance?.let { "${it.toInt()}%" } ?: "—", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { (1..31).forEach { d -> val mk = attendanceMark(e, d); val (bg, fg) = if (d > 8) Color.Transparent to Color(0xFF8A928D) else markColors(mk); Box(Modifier.weight(1f).height(18.dp).background(bg, RoundedCornerShape(3.dp)), contentAlignment = Alignment.Center) { Text(mk, color = fg, style = MaterialTheme.typography.labelMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp))) } } }
                    }
                }
                if (list.isEmpty()) item { EmptyBox("Hech narsa topilmadi") }
                ae?.let { e ->
                    sectionTitle("Kun timeline’i")
                    item { AppCard(padding = PaddingValues(0.dp)) {
                        Column(Modifier.padding(16.dp)) { Text("${e.name} · ${c.branchName(e.branchId)} · 8 okt", style = MaterialTheme.typography.titleSmall); Text("idle: " + (if (e.idleLimit > 0) "${fmtHm(e.idleUsed)} / ${fmtHm(e.idleLimit)}" else "limit yo‘q"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        TIMELINE.forEach { x -> HorizontalDivider(); ListRow("${x.t} · ${x.label}", "${x.code} · ${x.meta}", leading = { Box(Modifier.size(10.dp).background(when (x.tone) { Tone.Accent -> MaterialTheme.colorScheme.primary; Tone.Warning -> Warning; Tone.Info -> Info; Tone.Danger -> Danger; else -> MaterialTheme.colorScheme.outline }, RoundedCornerShape(5.dp))) }, trailing = if (x.evidence && Access.can("ai", me.role)) { { TextButton({ go("ai", null) }) { Text("Dalil") } } } else null) }
                        if (canFix) Row(Modifier.padding(16.dp)) { OutlinedButton({ scope.launch { try { c.repo.cmd("attendance.requestFix", buildJsonObject { put("empId", e.id) }); toast = "Tuzatish so‘rovi HR + filial mudiri tasdig‘iga yuborildi." } catch (x: Exception) { toast = x.userMessage() } } }, Modifier.fillMaxWidth()) { Text("Tuzatish kiritish") } }
                        Text("Timeline va kunlik belgilar web-panel prototipi ma’lumoti (API’da kunlik davomat/timeline endpointi hali yo‘q).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                    } }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
