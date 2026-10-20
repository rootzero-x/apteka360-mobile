package uz.apteka360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.AiEvent
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.Camera
import uz.apteka360.app.data.EmployeeFull
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.arr
import uz.apteka360.app.data.bool
import uz.apteka360.app.data.dbl
import uz.apteka360.app.data.long
import uz.apteka360.app.data.str
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.Badge
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
import uz.apteka360.app.ui.components.MoneyField
import uz.apteka360.app.ui.components.PickField
import uz.apteka360.app.ui.components.PickerSheet
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

private val CAM_ADMIN = listOf("OWNER", "SUPER_ADMIN", "MANAGER")
private val CAM_TEST = listOf("OWNER", "SUPER_ADMIN", "MANAGER", "OPERATOR")
fun camErr(e: Throwable): String = when {
    e is ApiException && e.code == "module_disabled" -> "Kameralar moduli o‘chirilgan. Egasi uni Tizim → Modullar bo‘limida yoqishi mumkin."
    e is ApiException && e.status == 503 && e.details?.toString()?.contains("edge_timeout") == true -> "Filial Edge serveri javob bermadi (15 s). Keyinroq urinib ko‘ring."
    e is ApiException && e.status == 503 && e.details?.toString()?.contains("edge_offline") == true -> "Filial Edge serveri oflayn — buyruqni bajarib bo‘lmaydi."
    e is ApiException && e.details?.toString()?.contains("no_audio_out") == true -> "Kamerada karnay topilmadi."
    e is ApiException && e.status == 403 -> e.message
    else -> e.userMessage()
}
fun camStatus(c: Camera) = when (c.st) { "online" -> "onlayn" to Tone.Accent; "offline" -> "oflayn" to Tone.Danger; else -> "noma’lum" to Tone.Neutral }
private val TASKS = listOf("attendance" to "Davomat", "people_count" to "Odam sanash", "idle" to "Idle", "fraud" to "Firibgarlik", "audio" to "Audio", "flow" to "Mijoz oqimi", "queue" to "Navbat", "pose" to "Poza (yotish/o‘tirish)")
private fun taskLabel(k: String) = TASKS.firstOrNull { it.first == k }?.second ?: k
private fun hs(s: String?) = when (s) { "ok" -> "ishlayapti" to Tone.Accent; "warn" -> "diqqat" to Tone.Warning; "down" -> "uzilgan" to Tone.Danger; else -> (s ?: "—") to Tone.Neutral }

// ═════════════ AI hodisalar — review navbati ═════════════
private val REJ_REASONS = listOf("Noto‘g‘ri xodim aniqlangan", "Ruxsat etilgan tanaffus", "Ish jarayoni (mijoz bilan ishlash)", "Video sifati past", "Boshqa")
private fun verdict(v: String?) = when (v) { "confirm" -> "AI tasdiqladi" to Tone.Accent; "reject" -> "AI rad etdi" to Tone.Danger; "unsure" -> "AI aniq emas" to Tone.Warning; else -> null }
private fun prioTone(p: String) = when (p) { "yuqori" -> Tone.Danger; "o‘rta" -> Tone.Warning; else -> Tone.Neutral }
data class AiData(val events: List<AiEvent>, val employees: List<EmployeeFull>, val reviewed: Long?)

@Composable
fun AiReviewScreen(c: AppContainer, me: Me, initialId: String?) {
    val scope = rememberCoroutineScope()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val loader = rememberLoader { c.branchesCached(); AiData(c.repo.aiEvents(), runCatching { c.repo.employeesFull(false) }.getOrDefault(emptyList()), runCatching { c.api.getJson("/v1/reviewedToday").let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() } }.getOrNull()) }
    var events by remember { mutableStateOf<List<AiEvent>>(emptyList()) }; var reviewed by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(loader.data) { loader.data?.let { events = it.events; reviewed = it.reviewed } }
    var filter by rememberSaveable { mutableStateOf("pending") }; var br by rememberSaveable { mutableStateOf("") }; var prio by rememberSaveable { mutableStateOf("") }; var q by rememberSaveable { mutableStateOf("") }
    var sel by rememberSaveable { mutableStateOf(initialId) }; var busy by remember { mutableStateOf(false) }; var toast by remember { mutableStateOf<String?>(null) }
    var reject by remember { mutableStateOf<AiEvent?>(null) }; var reassign by remember { mutableStateOf<AiEvent?>(null) }; var note by remember { mutableStateOf<AiEvent?>(null) }; var penalty by remember { mutableStateOf<AiEvent?>(null) }; var escal by remember { mutableStateOf<AiEvent?>(null) }
    val can = !Access.readOnly(me.role) && me.role in listOf("OWNER", "MANAGER", "OPERATOR"); val canEsc = !Access.readOnly(me.role) && me.role in listOf("OWNER", "FINANCE", "OPERATOR")
    val emps = loader.data?.employees ?: emptyList()
    fun emp(id: String?) = emps.firstOrNull { it.id == id }
    fun replace(upd: JsonObject?, id: String, patch: (AiEvent) -> AiEvent) { events = events.map { if (it.id == id) (upd?.let { u -> runCatching { uz.apteka360.app.data.json.decodeFromJsonElement(AiEvent.serializer(), u) }.getOrNull() } ?: patch(it)) else it } }
    suspend fun setStatus(ev: AiEvent, status: String, nt: String? = null) {
        val r = c.repo.cmd("ai.setStatus", buildJsonObject { put("id", ev.id); put("status", status); if (nt != null) put("note", nt) })
        replace(r as? JsonObject, ev.id) { it.copy(status = status, note = nt ?: it.note) }
        reviewed = runCatching { (c.api.getJson("/v1/reviewedToday") as? kotlinx.serialization.json.JsonPrimitive)?.content?.toLongOrNull() }.getOrNull() ?: reviewed
    }
    val scoped = events.filter { e -> (br.isBlank() || e.branchId == br) && (prio.isBlank() || e.priority == prio) && (q.isBlank() || listOf(e.label, e.code, e.camera, e.zone, emp(e.empId)?.name ?: "").joinToString(" ").contains(q, ignoreCase = true)) }
    val list = scoped.filter { filter == "all" || it.status == filter }
    ScreenScaffold("AI hodisalar — review", "Bugun ko‘rib chiqildi: ${reviewed ?: "—"}") {
        DataBox(loader) { _ ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (Access.readOnly(me.role)) item { InfoBar("Auditor rejimi.", "Hodisalarni faqat ko‘rish mumkin.") }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                item { Seg(listOf("pending", "confirmed", "rejected", "all").map { k -> k to (mapOf("pending" to "Kutilmoqda", "confirmed" to "Tasdiqlangan", "rejected" to "Rad etilgan", "all" to "Hammasi")[k]!! + " · " + (if (k == "all") scoped.size else scoped.count { it.status == k })) }, filter, { filter = it }) }
                item { SearchField(q, { q = it }, "Qidirish: hodisa, xodim, kamera") }
                if (c.branches.size > 1) item { Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, br, { br = it }) }
                item { Seg(listOf("" to "Barcha ustuvorlik", "yuqori" to "yuqori", "o‘rta" to "o‘rta", "past" to "past"), prio, { prio = it }) }
                if (list.isEmpty()) item { AppCard { Text(if (filter == "pending") "Navbat bo‘sh — barcha hodisalar ko‘rib chiqildi" else "Bu filtrda hodisa yo‘q", fontWeight = FontWeight.SemiBold); Text("Edge-server oflayn bo‘lgan filiallardan hodisa kelmaydi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(list, key = { it.id }) { e -> val en = emp(e.empId)
                    AppCard(Modifier.clickable { sel = e.id }, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(e.label, style = MaterialTheme.typography.titleSmall); Text(if (e.empId != null) (en?.name ?: e.empId) else "Xodim biriktirilmagan", style = MaterialTheme.typography.bodySmall); Text("${c.branchName(e.branchId)} · ${e.camera.substringBefore(" · ")} · ${e.date} ${e.time.take(5)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Column(horizontalAlignment = Alignment.End) { Text("${e.conf}%", fontWeight = FontWeight.Bold); Badge(e.priority.ifBlank { "—" }, prioTone(e.priority)); verdict(e.aiVerdict)?.let { Text(it.first, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                    } }
                item { Spacer(Modifier.height(24.dp)) }
            }
            events.firstOrNull { it.id == sel }?.let { ev ->
                val ce = emp(ev.empId); val pending = ev.status == "pending" && can && !busy
                val impact = if (ce != null && ce.idleLimit > 0 && ev.idleImpact > 0) "Tasdiqlansa idle hisobiga +${ev.idleImpact} daq qo‘shiladi: ${fmtHm(ce.idleUsed)} → ${fmtHm(ce.idleUsed + ev.idleImpact)} (limit ${fmtHm(ce.idleLimit)})" + (if (ce.idleUsed + ev.idleImpact > ce.idleLimit) ". Limitdan oshadi — jarima qoidasi P-04 taklif qilinadi." else ".")
                    else if (ev.code == "CASH_NO_RECEIPT") "Video: kassada pul olindi, lekin shu 40 soniyada FOM’da chek yo‘q. Tasdiqlansa, moliya admin tekshiruviga yuboriladi." else if (ev.code == "QUEUE_LONG") "Xodimga bog‘lanmagan. Tasdiqlansa, filial mudiriga ikkinchi kassani ochish haqida xabar boradi." else "Idle hisobiga ta’sir qilmaydi."
                fun next() { val l = list; val i = l.indexOfFirst { it.id == ev.id }; sel = if (filter == "pending") (l.getOrNull(i + 1) ?: l.getOrNull(i - 1))?.id else ev.id }
                FormSheet(ev.label, "${ev.code} · ${c.branchName(ev.branchId)}", primary = "Yopish", onDismiss = { sel = null }, onSubmit = { sel = null; null }) {
                    val privacy = Regex("dam olish", RegexOption.IGNORE_CASE).containsMatchIn(ev.zone) && Regex("xona", RegexOption.IGNORE_CASE).containsMatchIn(ev.zone)
                    val frames = ev.keyframes.filter { it.startsWith("http") }.take(4)
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF101010), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        when { privacy -> Text("Maxfiylik zonasi — video ko‘rsatilmaydi, faqat AI poza tahlili", color = Color(0xFF8A8A8A), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                            frames.isNotEmpty() -> Row(Modifier.fillMaxSize()) { frames.forEach { u -> NetImage(u, Modifier.weight(1f).fillMaxSize()) } }
                            else -> Text("NVR video-klip · kadrlar filial Edge’dan keladi (hali kelmagan)", color = Color(0xFF6E6E6E), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)) }
                        Text("${ev.camera} · ${ev.date} ${ev.time}", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
                        Text("${ev.code} · ${ev.conf}%", color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color(0xFF22A05F), RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) { Badge(aiStatusLabel(ev.status), aiStatusTone(ev.status)); verdict(ev.aiVerdict)?.let { Badge(it.first, it.second) } }
                    ev.aiVerdictNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) { Avatar(ce?.ini?.ifBlank { null } ?: Fmt.initials(ce?.name ?: "—"), 36); Spacer(Modifier.width(10.dp)); Column { Text(ce?.name ?: ev.empId ?: "Xodim aniqlanmagan", fontWeight = FontWeight.SemiBold); Text(ce?.let { listOf(it.position, it.schedule).filter { s -> s.isNotBlank() }.joinToString(" · ") } ?: "Hodisa zonaga tegishli", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    KeyValueRow("AI ishonchi", "${ev.conf}% · " + (if (ev.conf >= 85) "yuqori" else if (ev.conf >= 60) "o‘rta" else "past · qo‘shimcha tekshiruv")); KeyValueRow("Vaqt", "${ev.date} 2026, ${ev.time}"); KeyValueRow("Davomiylik", ev.dur); KeyValueRow("Zona", ev.zone); KeyValueRow("Kamera", ev.camera); KeyValueRow("Ustuvorlik", ev.priority)
                    InfoBar(impact); if (ev.note.isNotBlank()) Text("Izoh: ${ev.note}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    if (can) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = pending, onClick = { busy = true; scope.launch { try { setStatus(ev, "confirmed"); haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); if (ev.empId != null) penalty = ev else { toast = "Hodisa tasdiqlandi. Xodim biriktirilmagan — jarima taklif qilinmaydi."; next() } } catch (x: Exception) { toast = x.userMessage() } finally { busy = false } } }, modifier = Modifier.weight(1f)) { Text("Tasdiqlash") }
                            OutlinedButton(enabled = pending, onClick = { reject = ev }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Text("Rad etish") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                            OutlinedButton(enabled = pending, onClick = { reassign = ev }, modifier = Modifier.weight(1f)) { Text("Boshqa xodim", maxLines = 1) }
                            OutlinedButton(enabled = can && !busy, onClick = { note = ev }, modifier = Modifier.weight(1f)) { Text("Izoh") }
                        }
                        if (pending && ev.empId != null) TextButton({ busy = true; scope.launch { try { setStatus(ev, "confirmed"); penalty = ev } catch (x: Exception) { toast = x.userMessage() } finally { busy = false } } }) { Text("Tasdiqlash va jarima taklif qilish") }
                    }
                    if (me.role in listOf("OWNER", "FINANCE", "OPERATOR")) TextButton(enabled = canEsc && !busy, onClick = { escal = ev }) { Text("Eskalatsiya", color = Warning) }
                }
                reject?.let { r -> PickerSheet("Rad etish sababi", REJ_REASONS, { it }, onPick = { reason -> scope.launch { try { setStatus(r, "rejected", reason); toast = "Hodisa rad etildi: $reason. AI modeli uchun belgi saqlandi."; next() } catch (x: Exception) { toast = x.userMessage() } } }, onDismiss = { reject = null }) }
                reassign?.let { r -> PickerSheet("Boshqa xodimni tanlang", emps.filter { it.branchId == r.branchId && it.id != r.empId }, { it.name }, { listOf(it.position, it.status).filter { s -> s.isNotBlank() }.joinToString(" · ") }, onPick = { e -> scope.launch { try { val u = c.repo.cmd("ai.reassign", buildJsonObject { put("id", r.id); put("empId", e.id) }); replace(u as? JsonObject, r.id) { it.copy(empId = e.id) }; toast = "Hodisa ${e.name}ga biriktirildi." } catch (x: Exception) { toast = x.userMessage() } } }, onDismiss = { reassign = null }) }
                note?.let { r -> var t by remember { mutableStateOf("") }; FormSheet("Izoh", "Audit jurnaliga yoziladi", onDismiss = { note = null }, onSubmit = { if (t.isBlank()) return@FormSheet "Majburiy maydon."; val u = c.repo.cmd("ai.note", buildJsonObject { put("id", r.id); put("note", t.trim()) }); replace(u as? JsonObject, r.id) { it.copy(note = t.trim()) }; toast = "Izoh saqlandi."; null }) { Field("Izoh", t, { t = it }, minLines = 3) } }
                penalty?.let { r -> var amt by remember { mutableStateOf("150000") }; var nt by remember { mutableStateOf("") }
                    FormSheet("Jarima taklif qilish — ${emp(r.empId)?.name ?: ""}", "Hodisa tasdiqlandi. Jarima avtomatik qo‘llanmaydi: taklif filial mudiriga boradi, tasdiqdan keyin xodim 3 kun ichida e’tiroz bildira oladi.", primary = "Taklifni yuborish", onDismiss = { penalty = null; next() }, onSubmit = {
                        val n = amt.toLongOrNull() ?: 0; if (n <= 0) return@FormSheet "Summa 0 dan katta bo‘lishi kerak."
                        c.repo.cmd("ai.proposePenalty", buildJsonObject { put("eventId", r.id); put("amount", n); put("note", nt.trim()) }); toast = "Jarima taklifi filial mudiriga yuborildi."; null
                    }) { KeyValueRow("Qoida", "P-04 · Idle limitdan oshish"); MoneyField("Summa", amt, { amt = it }); Field("Izoh (xodimga ko‘rinadi)", nt, { nt = it }, minLines = 2) } }
                escal?.let { r -> ConfirmDialog("Eskalatsiya", "Hodisa yuqori darajaga (Egasi / moliya) yuboriladi va bildirishnoma jo‘natiladi.", "Eskalatsiya qilish", onDismiss = { escal = null }) { val u = c.repo.cmd("ai.escalate", buildJsonObject { put("id", r.id) }); replace(u as? JsonObject, r.id) { it }; toast = "Hodisa eskalatsiya qilindi."; null } }
            }
        }
    }
}

/** Oddiy tarmoq rasmi (kadr) — kutubxonasiz. */
@Composable
fun NetImage(url: String, modifier: Modifier = Modifier) {
    var bmp by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) { bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching { java.net.URL(url).openStream().use { android.graphics.BitmapFactory.decodeStream(it)?.asImageBitmap() } }.getOrNull() } }
    bmp?.let { androidx.compose.foundation.Image(it, null, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Fit) } ?: Box(modifier)
}

// ═════════════ Kameralar va zonalar ═════════════
@Composable
fun CamerasAdminScreen(c: AppContainer, me: Me, initialBranch: String?, openPlayer: (Camera) -> Unit) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.branchesCached(true); c.repo.cameras() }
    var br by rememberSaveable { mutableStateOf(initialBranch ?: "") }; var stf by rememberSaveable { mutableStateOf("") }; var q by rememberSaveable { mutableStateOf("") }
    var sel by remember { mutableStateOf<Camera?>(null) }; var addOpen by remember { mutableStateOf(false) }; var discover by remember { mutableStateOf(false) }; var toast by remember { mutableStateOf<String?>(null) }
    var prefill by remember { mutableStateOf<JsonObject?>(null) }
    val admin = !Access.readOnly(me.role) && me.role in CAM_ADMIN; val canTest = !Access.readOnly(me.role) && me.role in CAM_TEST
    val myBranches = if (me.role == "MANAGER" && me.branchId != null) c.branches.filter { it.id == me.branchId } else c.branches
    val err = loader.error.value
    ScreenScaffold("Kameralar va zonalar", loader.data?.let { cams -> "${cams.count { it.st == "online" }} / ${cams.size} onlayn · AI nazorat" }, fab = if (admin) Icons.Default.Add to { prefill = null; addOpen = true } else null) {
        if (loader.data == null && err is ApiException && err.code == "module_disabled") { Column(Modifier.padding(16.dp)) { InfoBar("Modul yoqilmagan.", camErr(err)) }; return@ScreenScaffold }
        DataBox(loader) { cams ->
            val shown = cams.filter { (br.isBlank() || it.branchId == br) && (stf.isBlank() || it.st == stf) && (q.isBlank() || listOf(it.name, it.zones, it.ai, it.host ?: "").joinToString(" ").contains(q, ignoreCase = true)) }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                item { SearchField(q, { q = it }, "Kamera, zona…") }
                if (c.branches.size > 1) item { Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, br, { br = it }) }
                item { Seg(listOf("" to "Hammasi", "online" to "Onlayn", "offline" to "Oflayn", "unknown" to "Noma’lum"), stf, { stf = it }) }
                if (admin) item { OutlinedButton({ discover = true }, Modifier.fillMaxWidth()) { Text("ONVIF qidirish") } }
                if (shown.isEmpty()) item { AppCard { Text("Kamera topilmadi", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(shown, key = { it.id }) { cam -> val (sl, stn) = camStatus(cam)
                    AppCard(Modifier.clickable { sel = cam }, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Videocam, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(cam.name, style = MaterialTheme.typography.titleSmall); if (cam.privacy) { Spacer(Modifier.width(6.dp)); Badge("maxfiy", Tone.Warning) } }
                                Text("${c.branchName(cam.branchId)} · ${cam.ai.ifBlank { cam.aiTasks.joinToString(", ") { taskLabel(it) } }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("RTSP ${hs(cam.rtsp).first} · NVR ${hs(cam.nvr).first} · ${if (cam.fps > 0) "${cam.fps} fps" else "—"} · ${cam.last}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Badge(sl, if (cam.health == "warn" && stn == Tone.Accent) Tone.Warning else stn)
                        }
                    } }
                item { Spacer(Modifier.height(72.dp)) }
            }
            sel?.let { cam -> CameraDetailSheet(c, me, cam, admin, canTest, onDismiss = { sel = null }, onLive = { sel = null; openPlayer(cam) }, onChanged = { t -> toast = t; reloadKey++ }) }
            if (addOpen) CameraForm(c, myBranches, prefill, onDismiss = { addOpen = false }, onSaved = { addOpen = false; toast = "Kamera qo‘shildi — Edge bir daqiqada ulaydi"; reloadKey++ })
            if (discover) DiscoverSheet(c, myBranches, onDismiss = { discover = false }, onAdd = { p -> discover = false; prefill = p; addOpen = true })
        }
    }
}

@Composable
private fun CameraDetailSheet(c: AppContainer, me: Me, cam: Camera, admin: Boolean, canTest: Boolean, onDismiss: () -> Unit, onLive: () -> Unit, onChanged: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var edit by remember { mutableStateOf(false) }; var priv by remember { mutableStateOf(false) }; var testOut by remember { mutableStateOf<String?>(null) }
    val b = c.branches.firstOrNull { it.id == cam.branchId }; val edgeOn = b?.let { it.edge == "online" }
    val (sl, stn) = camStatus(cam)
    FormSheet(cam.name, c.branchName(cam.branchId), primary = "Yopish", onDismiss = onDismiss, onSubmit = { onDismiss(); null }) {
        if (edgeOn == false) InfoBar("Edge offline.", "Filial Edge serveri oflayn — jonli ko‘rish, sinov va qidiruv ishlamaydi.", Tone.Warning)
        Button(onClick = onLive, enabled = cam.st == "online" && !cam.privacy, modifier = Modifier.fillMaxWidth()) { Text(if (cam.privacy) "Maxfiy kamera — jonli ko‘rish yo‘q" else if (cam.st != "online") "Kamera onlayn emas" else "Jonli ko‘rish") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            OutlinedButton(enabled = canTest, onClick = { testOut = "Edge kamerani ochishga urinmoqda…"; scope.launch { testOut = try { testText(c.repo.cmd("cameras.test", buildJsonObject { put("id", cam.id) }) as JsonObject) } catch (e: Exception) { camErr(e) } } }, modifier = Modifier.weight(1f)) { Text("Sinab ko‘rish") }
            OutlinedButton(enabled = admin, onClick = { edit = true }, modifier = Modifier.weight(1f)) { Text("Tahrirlash") }
        }
        OutlinedButton(enabled = admin, onClick = { priv = true }, modifier = Modifier.fillMaxWidth()) { Text(if (cam.privacy) "Maxfiylikni olib tashlash" else "Maxfiy qilish") }
        testOut?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Row { Text("Holat", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant); Badge(sl, stn) }
        KeyValueRow("Edge", if (edgeOn == null) "—" else if (edgeOn) "Edge online" else "Edge offline"); cam.kind?.let { KeyValueRow("Ulanish", it.uppercase()) }; cam.host?.let { KeyValueRow("Host", it) }
        KeyValueRow("RTSP", hs(cam.rtsp).first); KeyValueRow("NVR", hs(cam.nvr).first); KeyValueRow("FPS", if (cam.fps > 0) "${cam.fps} fps" else "—"); cam.bitrateKbps?.let { KeyValueRow("Bitreyt", "${Fmt.num(it)} kbit/s") }
        KeyValueRow("Oxirgi kadr", cam.last.ifBlank { "—" }); KeyValueRow("Yozib olish", if (cam.recording) "yoqilgan" else "o‘chiq"); KeyValueRow("Maxfiylik", if (cam.privacy) "Maxfiy · faqat poza" else "yo‘q")
        cam.audio?.let { KeyValueRow("Audio", listOfNotNull(if (it.input) "mikrofon" else null, if (it.output) "karnay" else null).joinToString(" + ").ifBlank { "yo‘q" }) }
        Text("Zonalar va AI vazifalar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        Text(cam.zones.ifBlank { "Zona belgilanmagan" }, style = MaterialTheme.typography.bodyMedium)
        Text(if (cam.aiTasks.isNotEmpty()) cam.aiTasks.joinToString(", ") { taskLabel(it) } else cam.ai.ifBlank { "AI vazifa yo‘q" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    if (edit) { var name by remember { mutableStateOf(cam.name) }; var zone by remember { mutableStateOf(cam.zones) }; val tasks = remember { mutableStateListOf<String>().apply { addAll(cam.aiTasks) } }; var rec by remember { mutableStateOf(cam.recording) }; var rtsp by remember { mutableStateOf("") }
        FormSheet("Kamerani tahrirlash — ${cam.name}", onDismiss = { edit = false }, onSubmit = {
            if (name.isBlank() || zone.isBlank()) return@FormSheet "Nomi va zona majburiy."
            if (rtsp.isNotBlank() && !Regex("^rtsps?://").containsMatchIn(rtsp.trim())) return@FormSheet "RTSP manzil rtsp:// bilan boshlanishi kerak."
            try { c.repo.cmd("cameras.update", buildJsonObject { put("id", cam.id); if (name.trim() != cam.name) put("name", name.trim()); if (zone.trim() != cam.zones) put("zone", zone.trim()); put("aiTasks", buildJsonArray { tasks.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }); put("recording", rec); if (rtsp.isNotBlank()) put("rtspUrl", rtsp.trim()) }); onChanged("Saqlandi va Edge-serverga yuborildi."); null } catch (e: Exception) { camErr(e) }
        }) { Field("Nomi", name, { name = it }); Field("Zona", zone, { zone = it }, hint = "Kassa, zal, ombor…"); TaskChips(tasks, cam.privacy); SwitchRow("Yozib olish", rec, { rec = it }); if (cam.kind != "device" && cam.kind != "onvif") Field("Yangi RTSP manzil", rtsp, { rtsp = it }, hint = "Bo‘sh qoldirilsa o‘zgarmaydi (audit: maxfiy)") } }
    if (priv) ConfirmDialog(if (cam.privacy) "Maxfiylikni olib tashlash" else "Maxfiy kamera qilish", if (!cam.privacy) "Maxfiy kamerada jonli ko‘rish o‘chiriladi, AI faqat poza tahlilini bajaradi va kadrlar AI tekshiruviga yuborilmaydi." else "Kamera uchun jonli ko‘rish va barcha AI vazifalar qayta ruxsat etiladi.", if (cam.privacy) "Olib tashlash" else "Maxfiy qilish", reasonLabel = "Sabab (kamida 3 belgi)", minReason = 3, onDismiss = { priv = false }) { r ->
        try { c.repo.cmd("cameras.setPrivacy", buildJsonObject { put("id", cam.id); put("privacy", !cam.privacy); put("reason", r) }); onChanged(if (!cam.privacy) "Kamera maxfiy qilindi" else "Maxfiylik olib tashlandi"); null } catch (e: Exception) { camErr(e) } }
}

private fun testText(r: JsonObject): String = if (r.bool("ok") == true) "Ulanish muvaffaqiyatli" + listOfNotNull(r.str("resolution"), r.str("codec")?.uppercase(), r.dbl("fps")?.let { "$it fps" }).joinToString("") { " · $it" } else "Kamera javob bermadi: ${r.str("error") ?: "noma’lum xato"}"

@Composable
private fun TaskChips(tasks: MutableList<String>, privacy: Boolean) {
    Text("AI vazifalar", style = MaterialTheme.typography.labelLarge)
    TASKS.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { (k, l) -> FilterChip(selected = k in tasks, enabled = !privacy || k == "pose", onClick = { if (k in tasks) tasks.remove(k) else tasks.add(k) }, label = { Text(l) }, modifier = Modifier.weight(1f)) } } }
    if (privacy) Text("Maxfiy kamerada faqat «Poza» vazifasi ruxsat etiladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CameraForm(c: AppContainer, branches: List<uz.apteka360.app.data.Branch>, init: JsonObject?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    var branch by remember { mutableStateOf(branches.firstOrNull { it.id == init.str("branchId") } ?: branches.firstOrNull()) }
    var name by remember { mutableStateOf(init.str("name") ?: "") }; var zone by remember { mutableStateOf("") }; var kind by remember { mutableStateOf(init.str("kind") ?: "rtsp") }
    var rtsp by remember { mutableStateOf("") }; var oh by remember { mutableStateOf(init.str("host") ?: "") }; var op by remember { mutableStateOf(init.long("port")?.toString() ?: "") }; var ou by remember { mutableStateOf("") }; var ow by remember { mutableStateOf("") }
    var priv by remember { mutableStateOf(false) }; var rec by remember { mutableStateOf(true) }; val tasks = remember { mutableStateListOf<String>() }; var testOut by remember { mutableStateOf<String?>(null) }
    fun onvif() = buildJsonObject { put("host", oh.trim()); put("username", ou.trim()); put("password", ow); op.toIntOrNull()?.let { put("port", it) } }
    fun validate(forTest: Boolean): String? = when {
        branch == null -> "Filialni tanlang."; !forTest && name.isBlank() -> "Nomi majburiy."; !forTest && zone.isBlank() -> "Zona majburiy."
        kind == "rtsp" && !Regex("^rtsps?://").containsMatchIn(rtsp.trim()) -> "RTSP manzil rtsp:// yoki rtsps:// bilan boshlanishi kerak."
        kind == "onvif" && (oh.isBlank() || ou.isBlank() || ow.isBlank()) -> "ONVIF host, login va parol majburiy."; else -> null }
    FormSheet("Kamera qo‘shish", "Edge bir daqiqa ichida kamerani ulaydi", primary = "Kamera qo‘shish", onDismiss = onDismiss, onSubmit = {
        validate(false)?.let { return@FormSheet it }
        try { c.repo.cmd("cameras.create", buildJsonObject { put("branchId", branch!!.id); put("name", name.trim()); put("zone", zone.trim()); put("kind", kind); put("aiTasks", buildJsonArray { tasks.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }); put("privacy", priv); put("recording", rec); if (kind == "rtsp") put("rtspUrl", rtsp.trim()); if (kind == "onvif") put("onvif", onvif()) }); onSaved(); null }
        catch (e: ApiException) { if (e.status == 409) "Bu qurilma filialga allaqachon qo‘shilgan." else camErr(e) }
    }) {
        PickField("Filial", branches, branch, { it.label }, { branch = it }); Field("Nomi", name, { name = it }, hint = "1-kamera"); Field("Zona", zone, { zone = it }, hint = "Kassa, zal, ombor…")
        Text("Ulanish turi", style = MaterialTheme.typography.labelLarge); Seg(listOf("rtsp" to "RTSP", "onvif" to "ONVIF", "device" to "Edge qurilmasi"), kind, { kind = it })
        Spacer(Modifier.height(8.dp))
        when (kind) { "rtsp" -> Field("RTSP", rtsp, { rtsp = it }, hint = "Login va parol bilan. Faqat Edge’ga yuboriladi."); "onvif" -> { Field("ONVIF host", oh, { oh = it }); Field("Port", op, { op = it }, number = true); Field("Login", ou, { ou = it }); Field("Parol", ow, { ow = it }, password = true) }; else -> Text("Edge serverga to‘g‘ridan-to‘g‘ri ulangan qurilma — manzil kerak emas.", style = MaterialTheme.typography.bodySmall) }
        TaskChips(tasks, priv)
        SwitchRow("Maxfiy kamera (faqat poza)", priv, { priv = it; if (it) tasks.retainAll(listOf("pose")) }); SwitchRow("Yozib olish", rec, { rec = it })
        OutlinedButton(onClick = { val v = validate(true); if (v != null) testOut = v else if (kind == "device") testOut = "Edge qurilmasini saqlangandan keyin sinab ko‘ring." else { testOut = "Edge kamerani ochishga urinmoqda…"; scope.launch { testOut = try { testText(c.repo.cmd("cameras.test", buildJsonObject { put("branchId", branch!!.id); if (kind == "rtsp") put("rtspUrl", rtsp.trim()) else put("onvif", onvif()) }) as JsonObject) } catch (e: Exception) { camErr(e) } } } }, modifier = Modifier.fillMaxWidth()) { Text("Sinab ko‘rish") }
        testOut?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DiscoverSheet(c: AppContainer, branches: List<uz.apteka360.app.data.Branch>, onDismiss: () -> Unit, onAdd: (JsonObject) -> Unit) {
    val scope = rememberCoroutineScope()
    var branch by remember { mutableStateOf(branches.firstOrNull()) }; var sec by remember { mutableStateOf("8") }
    var busy by remember { mutableStateOf(false) }; var out by remember { mutableStateOf<List<JsonObject>?>(null) }; var err by remember { mutableStateOf<String?>(null) }
    FormSheet("ONVIF qidirish", "Filial Edge serveri mahalliy tarmoqda WS-Discovery orqali kameralarni qidiradi.", primary = "Yopish", onDismiss = onDismiss, onSubmit = { onDismiss(); null }) {
        PickField("Filial", branches, branch, { it.label }, { branch = it }); Field("Kutish, s", sec, { sec = it }, number = true)
        Button(enabled = !busy && branch != null, onClick = { busy = true; err = null; out = null; scope.launch { try { out = (c.repo.cmd("cameras.discover", buildJsonObject { put("branchId", branch!!.id); put("timeoutSec", (sec.toIntOrNull() ?: 8).coerceIn(1, 30)) })).arr.map { it as JsonObject } } catch (e: Exception) { err = camErr(e) } finally { busy = false } } }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Qidirilmoqda…" else "Qidirish") }
        err?.let { InfoBar("Qidirib bo‘lmadi.", it, Tone.Danger) }
        out?.let { l -> if (l.isEmpty()) Text("Qurilma topilmadi", color = MaterialTheme.colorScheme.onSurfaceVariant)
            l.forEach { d -> ListRow(d.str("name") ?: d.str("model") ?: d.str("host") ?: "", "${listOfNotNull(d.str("manufacturer"), d.str("model")).joinToString(" · ")} · ${d.str("host")}:${d.long("port")}", trailing = { if (d.bool("alreadyAdded") == true) Badge("qo‘shilgan", Tone.Neutral) else TextButton({ onAdd(buildJsonObject { put("branchId", branch!!.id); put("kind", "onvif"); put("host", d.str("host")); d.long("port")?.let { put("port", it) }; put("name", d.str("name") ?: d.str("model")) }) }) { Text("Qo‘shish") } }) } }
    }
}

// ═════════════ Video devor ═════════════
@Composable
fun VideoWallScreen(c: AppContainer, me: Me, openPlayer: (Camera) -> Unit) {
    val loader = rememberLoader { c.branchesCached(); c.repo.cameras() }
    var br by rememberSaveable { mutableStateOf("") }; var cols by rememberSaveable { mutableStateOf(2) }
    val err = loader.error.value
    ScreenScaffold("Video devor", "Plitkani bosing — to‘liq ekran, ovoz va gapirish") {
        if (loader.data == null && err is ApiException && err.code == "module_disabled") { Column(Modifier.padding(16.dp)) { InfoBar("Modul yoqilmagan.", camErr(err)) }; return@ScreenScaffold }
        DataBox(loader) { cams ->
            val all = cams.filter { br.isBlank() || it.branchId == br }
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (c.branches.size > 1) Seg(listOf("" to "Barcha filiallar") + c.branches.map { it.id to it.label }, br, { br = it })
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("${all.count { it.st == "online" }} / ${all.size} onlayn", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)); Seg(listOf("1" to "1", "2" to "4", "3" to "9"), "$cols", { cols = it.toInt() }) }
                }
                if (all.isEmpty()) EmptyBox("Kamera yo‘q", Modifier.fillMaxSize())
                else LazyVerticalGrid(GridCells.Fixed(cols), Modifier.fillMaxSize().background(Color(0xFF101010)), contentPadding = PaddingValues(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(all, key = { it.id }) { cam -> WallTile(c, cam, cols) { if (cam.st == "online" && !cam.privacy) openPlayer(cam) } }
                }
            }
        }
    }
}

@Composable
private fun WallTile(c: AppContainer, cam: Camera, cols: Int, onClick: () -> Unit) {
    val off = cam.st != "online"
    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF181818), RoundedCornerShape(6.dp)).clickable(onClick = onClick)) {
        when { off -> Text(if (cam.st == "unknown") "Holat noma’lum · Edge jim" else "Signal yo‘q · RTSP uzilgan", color = Color(0xFF6E6E6E), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(8.dp))
            cam.privacy -> Text("Maxfiylik zonasi · faqat poza tahlili", color = Color(0xFF6E6E6E), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(8.dp))
            cols <= 2 -> uz.apteka360.app.ui.cameras.MiniPlayer(c.repo, cam.id, Modifier.fillMaxSize())
            else -> Icon(Icons.Outlined.Videocam, null, tint = Color(0xFF4A4A4A), modifier = Modifier.align(Alignment.Center).size(28.dp)) }
        Row(Modifier.fillMaxWidth().background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text(cam.name, color = Color(0xFFE6E6E6), style = MaterialTheme.typography.labelMedium, maxLines = 1, modifier = Modifier.weight(1f)); Text(if (off) "oflayn" else "● LIVE", color = if (off) Color(0xFF8A8A8A) else Color(0xFFFF8787), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
    }
}

// ═════════════ Jonli filial ═════════════
private fun zoneOf(e: EmployeeFull): String? = when { e.status == "tanaffus" -> "Dam olish"; e.status != "ishda" && e.status != "kech qoldi" -> null; e.position == "Kassir" -> "Kassa"; e.position == "Omborchi" -> "Ombor"; else -> "Zal" }
data class LiveData(val branches: List<uz.apteka360.app.data.Branch>, val emps: List<EmployeeFull>, val cams: List<Camera>, val ev: List<AiEvent>)

@Composable
fun LiveBranchScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit, openPlayer: (Camera) -> Unit) {
    var tick by remember { mutableStateOf(0) }
    val loader = rememberLoader { LiveData(c.branchesCached(true), runCatching { c.repo.employeesFull(false) }.getOrDefault(emptyList()), runCatching { c.repo.cameras() }.getOrDefault(emptyList()), runCatching { c.repo.aiEvents() }.getOrDefault(emptyList())) }
    var updated by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); loader.reload(true); updated = System.currentTimeMillis() } }
    var bid by rememberSaveable { mutableStateOf(me.branchId ?: "") }
    ScreenScaffold("Jonli filial ● JONLI", "Yangilandi ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(updated))} · har 30 soniyada") {
        DataBox(loader) { d ->
            val b = d.branches.firstOrNull { it.id == bid } ?: d.branches.firstOrNull()
            if (b == null) { EmptyBox("Filial yo‘q", Modifier.fillMaxSize()); return@DataBox }
            val emps = d.emps.filter { it.branchId == b.id }; val cams = d.cams.filter { it.branchId == b.id }; val evs = d.ev.filter { it.branchId == b.id && it.status == "pending" }; val inside = emps.filter { zoneOf(it) != null }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (d.branches.size > 1) item { Seg(d.branches.map { it.id to it.label }, b.id, { bid = it }) }
                if (b.edge != "online") item { InfoBar("Edge-server offline — jonli joylashuv mavjud emas.", "Xodimlar davomati mobil check-in orqali olinmoqda; kamera va AI ma’lumotlari kelmaydi.", Tone.Danger) }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Filialda xodim", "${if (emps.isNotEmpty()) inside.size else b.onShift} / ${emps.size.takeIf { it > 0 } ?: b.employees}", modifier = Modifier.weight(1f), foot = "smenada: ${b.onShift}"); KpiCard("Mijozlar (bugun)", Fmt.num(b.traffic), modifier = Modifier.weight(1f), foot = "konversiya ${b.conversion}%") } }
                item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiCard("Navbat (kassa)", "${b.queue}", "kishi", Modifier.weight(1f), foot = "${b.desks} ta kassa"); KpiCard("Kameralar", "${if (cams.isNotEmpty()) cams.count { it.st == "online" } else b.camerasOnline} / ${cams.size.takeIf { it > 0 } ?: b.cameras}", modifier = Modifier.weight(1f), foot = if (b.edge == "online") "Edge online" else "Edge offline") } }
                sectionTitle("Zonalar bo‘yicha joylashuv")
                listOf("Kirish", "Kassa", "Zal", "Ombor", "Dam olish").chunked(2).forEach { row -> item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { z -> val here = emps.filter { zoneOf(it) == z }; val zc = cams.count { it.zones.contains(z) }
                    AppCard(Modifier.weight(1f), padding = PaddingValues(12.dp)) { Row { Text(z, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(listOfNotNull(if (z == "Kassa" && b.queue > 0) "${b.queue} mijoz" else null, if (zc > 0) "$zc kamera" else null).joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) { if (here.isEmpty()) Text("xodim yo‘q", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); here.take(6).forEach { Avatar(it.ini.ifBlank { Fmt.initials(it.name) }, 28) } } } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } }
                sectionTitle("Smenadagi xodimlar · ${inside.size} / ${emps.size}")
                if (emps.isEmpty()) item { AppCard { Text("Xodimlar ma’lumoti mavjud emas", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                else item { AppCard(padding = PaddingValues(0.dp)) { emps.forEachIndexed { i, e -> if (i > 0) HorizontalDivider(); ListRow(e.name, "${e.position} · ${zoneOf(e) ?: "Filialda emas"}", leading = { Avatar(e.ini.ifBlank { Fmt.initials(e.name) }, 32) }, trailing = { Badge(e.status, empTone(e.status)) }, onClick = { go("employee", e.id) }) } } }
                sectionTitle("Kameralar")
                if (cams.isEmpty()) item { AppCard { Text("Kamera yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                else item { AppCard(padding = PaddingValues(0.dp)) { cams.forEachIndexed { i, cam -> if (i > 0) HorizontalDivider(); val (l, t) = camStatus(cam); ListRow(cam.name, cam.ai, leading = { Icon(Icons.Outlined.Videocam, null) }, trailing = { Badge(if (cam.privacy) "maxfiy" else l, if (cam.privacy) Tone.Warning else t) }, onClick = if (cam.st == "online" && !cam.privacy && Access.can("videowall", me.role)) { { openPlayer(cam) } } else null) } } }
                sectionTitle("AI hodisalar (kutilmoqda)")
                if (evs.isEmpty()) item { AppCard { Text("Kutilayotgan AI hodisa yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                else item { AppCard(padding = PaddingValues(0.dp)) { evs.take(6).forEachIndexed { i, e -> if (i > 0) HorizontalDivider(); ListRow(e.label, "${e.camera.substringBefore(" · ")} · ${e.time}", trailing = { Badge("${e.conf}%", prioTone(e.priority)) }, onClick = if (Access.can("ai", me.role)) { { go("ai", e.id) } } else null) } } }
                item { AppCard { KeyValueRow("Manzil", b.address); KeyValueRow("Ish vaqti", b.hours); KeyValueRow("Mudir", b.manager); KeyValueRow("FOM", b.fom?.let { "${if (it.status == "ok") "ulangan" else if (it.status == "delay") "kechikmoqda" else "uzilgan"} · ${it.lag ?: ""}" } ?: "—"); KeyValueRow("Salomatlik", "${b.health} / 100") } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}
