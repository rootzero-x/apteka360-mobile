package uz.apteka360.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.Account
import uz.apteka360.app.data.EdgeDevice
import uz.apteka360.app.data.FomAgent
import uz.apteka360.app.data.FomMapRow
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.RolePermissions
import uz.apteka360.app.data.Settings
import uz.apteka360.app.data.bool
import uz.apteka360.app.data.child
import uz.apteka360.app.data.dbl
import uz.apteka360.app.data.list
import uz.apteka360.app.data.long
import uz.apteka360.app.data.obj
import uz.apteka360.app.data.str
import uz.apteka360.app.data.text
import uz.apteka360.app.data.txt
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
import uz.apteka360.app.ui.components.PickField
import uz.apteka360.app.ui.components.SearchField
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.SwitchRow
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger

val ROLE_CODES = listOf("OWNER", "SUPER_ADMIN", "FINANCE", "MANAGER", "HR", "OPERATOR", "EMPLOYEE", "AUDITOR")
val ROLE = mapOf("OWNER" to "Egasi", "SUPER_ADMIN" to "Super admin", "FINANCE" to "Moliya admin", "MANAGER" to "Filial mudiri", "HR" to "HR menejer", "OPERATOR" to "Video operator", "EMPLOYEE" to "Xodim", "AUDITOR" to "Auditor")
private val SCOPES = mapOf("OWNER" to "Barcha filiallar", "SUPER_ADMIN" to "Barcha filiallar", "FINANCE" to "Bir nechta filial", "MANAGER" to "Bitta filial (o‘z filiali)", "HR" to "Bir nechta filial", "OPERATOR" to "Bir nechta filial", "EMPLOYEE" to "Faqat o‘zi", "AUDITOR" to "Barcha filiallar")
fun isAdmin(role: String) = (role == "OWNER" || role == "SUPER_ADMIN") && !Access.readOnly(role)
fun copyText(ctx: Context, v: String) { (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Apteka 360", v)) }

@Composable
fun CopyRow(label: String, value: String) {
    val ctx = LocalContext.current
    Column(Modifier.padding(vertical = 6.dp)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) { Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)); TextButton({ copyText(ctx, value) }) { Text("Nusxalash") } } }
}

// ═════════════ FOM integratsiya ═════════════
data class FomData(val agents: List<FomAgent>, val map: List<FomMapRow>?)

@Composable
fun FomScreen(c: AppContainer, me: Me) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.branchesCached(true); FomData(c.api.getAs("/v1/fom/agents"), runCatching { c.api.getAs<List<FomMapRow>>("/v1/fomMap") }.getOrNull()) }
    val admin = isAdmin(me.role)
    var add by remember { mutableStateOf(false) }; var secret by remember { mutableStateOf<Pair<String, String>?>(null) }; var toast by remember { mutableStateOf<String?>(null) }
    var rotate by remember { mutableStateOf<FomAgent?>(null) }; var del by remember { mutableStateOf<FomAgent?>(null) }; var mapFor by remember { mutableStateOf<Pair<FomAgent, JsonElement>?>(null) }
    ScreenScaffold("FOM integratsiya", "Dorixona kompyuteridagi agent har 2 daqiqada savdoni yuboradi", fab = if (admin) Icons.Default.Add to { add = true } else null) {
        DataBox(loader) { d ->
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (Access.readOnly(me.role)) item { InfoBar("Auditor rejimi.", "Faqat ko‘rish — agentlarni egasi yoki super-admin boshqaradi.") }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                if (d.agents.isEmpty()) item { AppCard { Text("Hali agent yo‘q. «+» bilan boshlang, keyin tools/fom-agent ni dorixona kompyuteriga o‘rnating.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(d.agents, key = { it.id }) { a -> val st = when (a.status) { "ok" -> "Onlayn" to Tone.Accent; "delay" -> "Kechikmoqda" to Tone.Warning; "down" -> "Uzilgan" to Tone.Danger; else -> a.status to Tone.Info }
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(a.name, style = MaterialTheme.typography.titleSmall); Text(a.id, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Badge(st.first, st.second) }
                        Text((a.lastSyncAt?.let { "Oxirgi sinxron: " + Fmt.dateTime(it) } ?: "hali ulanmagan") + (a.db?.let { db -> " · FOM bazasi: " + (if (db.bool("ok") == true) "ulangan · ${db.long("latencyMs") ?: "—"} ms" else "ulanmagan") } ?: "") + (a.agentVersion?.let { " · Agent v$it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        a.lastError?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                        if (a.otdels.isEmpty()) Text("Agent hali ma’lumot yubormadi. Dorixona kompyuterida: python fom_agent.py --once", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        a.otdels.forEach { o -> HorizontalDivider(Modifier.padding(vertical = 6.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(o.str("name") ?: "", fontWeight = FontWeight.SemiBold); Text((o.str("otdel")?.let { "OTDEL $it" } ?: "Butun baza") + " · bugun ${Fmt.money(o.long("revenueToday"))} · ${o.long("checksToday") ?: 0} chek", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            TextButton(enabled = admin, onClick = { mapFor = a to o }) { Text(o.str("branchId")?.let { c.branchName(it) } ?: "bog‘lanmagan") } } }
                        if (admin) Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) { OutlinedButton({ rotate = a }, Modifier.weight(1f)) { Text("Kalitni yangilash") }; OutlinedButton({ del = a }, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Text("O‘chirish") } }
                    } }
                d.map?.takeIf { it.isNotEmpty() }?.let { map ->
                    sectionTitle("Moslashtirish (mapping)")
                    item { AppCard(padding = PaddingValues(0.dp)) { map.forEachIndexed { i, row -> if (i > 0) HorizontalDivider(); ListRow("${row.name} · ${row.fom}", if (row.conf > 0) "${row.guess} · AI taklifi ${row.conf}%" else "mos topilmadi", trailing = { if (row.done) Badge("✓ bog‘landi", Tone.Accent) else TextButton(enabled = admin, onClick = { scope.launch { try { c.repo.cmd("fom.link", buildJsonObject { put("index", i) }); toast = "Bog‘landi: ${row.name}"; reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }) { Text("Bog‘lash") } }) } } }
                }
                item { InfoBar("Qanday ulanadi", "1) Dorixona kompyuterida FAPTEKA xizmati ishlashi kerak (http://127.0.0.1:8765). 2) Agent qo‘shib agent_id va secret oling. 3) tools/fom-agent ni o‘rnating, fom_agent.ini ni to‘ldiring, run_agent.bat ni avtostartga qo‘shing. 4) Har FOM filialini (OTDEL) Apteka 360 filialiga bog‘lang.") }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
    if (add) { var nm by remember { mutableStateOf("") }; FormSheet("Agent qo‘shish", primary = "Yaratish", onDismiss = { add = false }, onSubmit = { if (nm.isBlank()) return@FormSheet "Majburiy maydon."; val r = c.api.request("POST", "/v1/fom/agents", buildJsonObject { put("name", nm.trim()) }).let { uz.apteka360.app.data.json.parseToJsonElement(it) }; secret = (r.child("agent").str("id") ?: "") to (r.str("secret") ?: ""); reloadKey++; null }) { Field("Agent nomi", nm, { nm = it }, hint = "Masalan: Markaziy server yoki Chilonzor 12 kompyuteri") } }
    rotate?.let { a -> ConfirmDialog("Kalitni yangilash", "Yangi kalit beriladi — eski kalit darhol ishlamay qoladi.", "Yangilash", onDismiss = { rotate = null }) { val r = uz.apteka360.app.data.json.parseToJsonElement(c.api.request("POST", "/v1/fom/agents/${a.id}/rotate", buildJsonObject {})); secret = a.id to (r.str("secret") ?: ""); null } }
    del?.let { a -> ConfirmDialog("O‘chirish", "Rostdan o‘chirasizmi? Bog‘langan filiallar FOM ma’lumotini olmay qoladi.", "O‘chirish", danger = true, onDismiss = { del = null }) { c.api.request("DELETE", "/v1/fom/agents/${a.id}"); toast = "Agent o‘chirildi"; reloadKey++; null } }
    mapFor?.let { (a, o) -> uz.apteka360.app.ui.components.PickerSheet("Apteka 360 filiali", listOf<uz.apteka360.app.data.Branch?>(null) + c.branches, { it?.name ?: "— bog‘lanmagan —" }, onPick = { b -> scope.launch { try { c.api.request("PUT", "/v1/fom/agents/${a.id}/map", buildJsonObject { put("otdel", o.str("otdel")); put("branchId", b?.id) }); toast = "Filial bog‘landi"; reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }, onDismiss = { mapFor = null }) }
    secret?.let { (id, sec) -> val ini = "[apteka360]\napi_url = ${c.api.baseUrl}\nagent_id = $id\nsecret = $sec\n\n[fapteka]\nurl = http://127.0.0.1:8765\notdels =\ndays = 31\n\n[agent]\ninterval = 120"
        FormSheet("Agent kaliti — faqat hozir ko‘rinadi", "Bu qiymatlarni fom_agent.ini fayliga qo‘ying. Kalit qayta ko‘rsatilmaydi.", primary = "Tushunarli", onDismiss = { secret = null }, onSubmit = { secret = null; null }) { CopyRow("agent_id", id); CopyRow("secret", sec); CopyRow("fom_agent.ini", ini) } }
}

// ═════════════ Tizim holati ═════════════
data class HealthData(val br: List<uz.apteka360.app.data.Branch>, val edges: List<EdgeDevice>?, val edgeErr: String?, val agents: List<FomAgent>?, val ms: Long)

@Composable
fun HealthScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit) {
    val fomOk = me.role in listOf("OWNER", "SUPER_ADMIN", "FINANCE", "AUDITOR")
    val loader = rememberLoader { val t0 = System.currentTimeMillis(); val br = c.branchesCached(true); val ms = System.currentTimeMillis() - t0
        var ee: String? = null; val edges = try { c.api.getAs<List<EdgeDevice>>("/v1/edges") } catch (e: uz.apteka360.app.data.ApiException) { if (e.status != 403) ee = e.message; null }
        HealthData(br, edges, ee, if (fomOk) runCatching { c.api.getAs<List<FomAgent>>("/v1/fom/agents") }.getOrNull() else null, ms) }
    ScreenScaffold("Tizim holati", "Server, FOM sinxronlash, Edge serverlar va kameralar") {
        DataBox(loader) { d ->
            val edgeBy = d.edges?.associateBy { it.branchId } ?: emptyMap()
            val cams = d.br.sumOf { it.cameras }; val camsOn = d.br.sumOf { it.camerasOnline }; val fomN = d.br.count { it.fom?.status == "ok" }; val edgeOn = d.br.count { edgeBy[it.id]?.online ?: (it.edge == "online") }
            fun lvl(ok: Int, total: Int) = if (total == 0) "Offline" to Tone.Neutral else if (ok == total) "Sog‘lom" to Tone.Accent else if (ok == 0) "Kritik" to Tone.Danger else "Ogohlantirish" to Tone.Warning
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val tiles = buildList { add(Triple("API server", "Javob: ${d.ms} ms", if (d.ms < 1500) "Sog‘lom" to Tone.Accent else "Ogohlantirish" to Tone.Warning)); add(Triple("FOM sinxronlash", "$fomN / ${d.br.size} filial sinxron", lvl(fomN, d.br.size)))
                    d.agents?.let { a -> val on = a.count { it.status == "ok" }; add(Triple("FOM agentlar", if (a.isNotEmpty()) "$on / ${a.size} onlayn" else "Agent qo‘shilmagan", lvl(on, a.size))) }
                    add(Triple("Edge serverlar", "$edgeOn / ${d.br.size} onlayn", lvl(edgeOn, d.br.size))); add(Triple("Kameralar", "$camsOn / $cams onlayn", lvl(camsOn, cams))) }
                items(tiles) { (n, m, k) -> AppCard(padding = PaddingValues(0.dp)) { ListRow(n, m, trailing = { Badge(k.first, k.second) }) } }
                d.edgeErr?.let { item { InfoBar("Edge holatini olib bo‘lmadi.", it, Tone.Warning) } }
                sectionTitle("Filiallar bo‘yicha")
                items(d.br, key = { it.id }) { b -> val e = edgeBy[b.id]; val on = e?.online ?: (b.edge == "online")
                    AppCard(Modifier.clickable(enabled = Access.can("branches", me.role)) { go("branch", b.id) }, padding = PaddingValues(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { HealthDot(b.health); Spacer(Modifier.width(8.dp)); Text(b.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); Text("${b.health}%", fontWeight = FontWeight.SemiBold) }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) { Badge(when (b.fom?.status) { "ok" -> "FOM sinxron"; "down" -> "FOM uzilgan"; else -> "FOM ${b.fom?.status ?: "—"}" }, when (b.fom?.status) { "ok" -> Tone.Accent; "down" -> Tone.Danger; else -> Tone.Warning }); Badge(if (on) "Edge onlayn" else "Edge oflayn", if (on) Tone.Accent else Tone.Danger); Badge("${b.camerasOnline}/${b.cameras} kam.", if (b.camerasOnline < b.cameras) Tone.Danger else Tone.Neutral) }
                        e?.let { Text(listOfNotNull("v${it.version}", "oxirgi: ${Fmt.dateTime(it.lastSeen)}", it.gpu.str("model"), it.tempC?.let { t -> "$t°C" }, it.disk?.let { dk -> "disk ${dk.txt("usedGb")}/${dk.txt("totalGb")} GB" }, it.nvr?.let { n -> listOfNotNull(n.long("retentionDays")?.let { r -> "NVR $r kun" }).joinToString() }).filter { s -> s.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                    } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═════════════ Foydalanuvchilar ═════════════
@Composable
fun UsersScreen(c: AppContainer, me: Me) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.branchesCached(); c.api.getAs<List<Account>>("/v1/admin/users") }
    val manage = isAdmin(me.role)
    var q by rememberSaveable { mutableStateOf("") }; var rf by rememberSaveable { mutableStateOf("") }; var sel by remember { mutableStateOf<Account?>(null) }; var form by remember { mutableStateOf<Account?>(null) }; var formOpen by remember { mutableStateOf(false) }
    var resetPw by remember { mutableStateOf<Account?>(null) }; var block by remember { mutableStateOf<Account?>(null) }; var toast by remember { mutableStateOf<String?>(null) }
    fun canTouch(a: Account) = manage && (me.role == "OWNER" || a.role != "OWNER")
    ScreenScaffold("Foydalanuvchilar", loader.data?.let { "${it.size} ta foydalanuvchi" }, fab = if (manage) Icons.Default.Add to { form = null; formOpen = true } else null) {
        DataBox(loader) { list ->
            val rows = list.filter { (rf.isBlank() || it.role == rf) && (q.isBlank() || listOf(it.name, it.phone, it.email, it.title).joinToString(" ").contains(q, ignoreCase = true)) }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (me.role == "AUDITOR") item { InfoBar("Ko‘rish rejimi.", "Foydalanuvchilarni faqat Egasi va Super admin boshqaradi.") }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                item { SearchField(q, { q = it }, "Ism, telefon yoki e-pochta") }
                item { Seg(listOf("" to "Barcha rollar") + ROLE_CODES.map { it to ROLE[it]!! }, rf, { rf = it }) }
                if (rows.isEmpty()) item { AppCard { Text("Foydalanuvchi topilmadi", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                items(rows, key = { it.id }) { a -> AppCard(Modifier.clickable(enabled = canTouch(a)) { sel = a }, padding = PaddingValues(0.dp)) {
                    ListRow(a.name, listOf(ROLE[a.role] ?: a.role, a.branchId?.let { c.branchName(it) } ?: "", a.phone, a.email).filter { it.isNotBlank() }.joinToString(" · ") + "\nOxirgi kirish: " + (a.lastLoginAt?.let { Fmt.dateTime(it) } ?: "Kirmagan") + listOfNotNull(if (a.demo) "demo" else null, if (a.telegram) "Telegram" else null).joinToString("") { " · $it" },
                        leading = { Avatar(Fmt.initials(a.name), 36) }, trailing = { Badge(if (a.status == "active") "Faol" else "Bloklangan", if (a.status == "active") Tone.Accent else Tone.Danger) }) } }
                if (manage) item { TelegramCard(c, me, list) }
                item { Spacer(Modifier.height(72.dp)) }
            }
            sel?.let { a -> FormSheet(a.name, ROLE[a.role], primary = "Yopish", onDismiss = { sel = null }, onSubmit = { sel = null; null }) {
                OutlinedButton({ sel = null; form = a; formOpen = true }, Modifier.fillMaxWidth()) { Text("Tahrirlash") }
                OutlinedButton({ sel = null; resetPw = a }, Modifier.fillMaxWidth()) { Text("Parolni almashtirish") }
                if (a.status == "active") OutlinedButton({ sel = null; block = a }, Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Text("Bloklash") }
                else Button({ scope.launch { try { c.repo.cmd("users.setStatus", buildJsonObject { put("id", a.id); put("status", "active") }); toast = "«${a.name}» faollashtirildi"; sel = null; reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }, Modifier.fillMaxWidth()) { Text("Faollashtirish") }
            } }
            if (formOpen) UserForm(c, me, form, onDismiss = { formOpen = false }, onSaved = { t -> formOpen = false; toast = t; reloadKey++ })
            resetPw?.let { a -> var pw by remember { mutableStateOf("") }; FormSheet("Parolni almashtirish", a.name, onDismiss = { resetPw = null }, onSubmit = { if (pw.length < 8) return@FormSheet "Kamida 8 ta belgi."; c.repo.cmd("users.resetPassword", buildJsonObject { put("id", a.id); put("password", pw) }); toast = "«${a.name}» paroli almashtirildi, sessiyalari yopildi"; null }) { Field("Yangi parol", pw, { pw = it }, password = true, hint = "Kamida 8 ta belgi. Foydalanuvchiga xavfsiz yo‘l bilan yetkazing.") } }
            block?.let { a -> ConfirmDialog("Bloklash", a.name, "Bloklash", danger = true, reasonLabel = "Bloklash sababi", onDismiss = { block = null }) { r -> c.repo.cmd("users.setStatus", buildJsonObject { put("id", a.id); put("status", "blocked"); if (r.isNotBlank()) put("reason", r) }); toast = "«${a.name}» bloklandi — barcha sessiyalari yopildi"; reloadKey++; null } }
        }
    }
}

@Composable
private fun UserForm(c: AppContainer, me: Me, acc: Account?, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    val roles = ROLE_CODES.filter { me.role == "OWNER" || it != "OWNER" }
    var name by remember { mutableStateOf(acc?.name ?: "") }; var role by remember { mutableStateOf(acc?.role ?: "MANAGER") }; var branch by remember { mutableStateOf(c.branches.firstOrNull { it.id == acc?.branchId } ?: c.branches.firstOrNull()) }
    var title by remember { mutableStateOf(acc?.title ?: "") }; var phone by remember { mutableStateOf(acc?.phone ?: "") }; var email by remember { mutableStateOf(acc?.email ?: "") }; var pw by remember { mutableStateOf("") }
    val needsBranch = role == "MANAGER" || role == "EMPLOYEE"
    FormSheet(if (acc == null) "Foydalanuvchi qo‘shish" else "Foydalanuvchini tahrirlash", if (acc != null) "Rol yoki filial o‘zgarsa, foydalanuvchi qaytadan kiradi." else null, onDismiss = onDismiss, onSubmit = {
        if (name.isBlank() || phone.isBlank() || email.isBlank()) return@FormSheet "F.I.Sh., telefon va e-pochta majburiy."
        if (acc == null && pw.length < 8) return@FormSheet "Parol kamida 8 ta belgi bo‘lsin."
        val r = c.repo.cmd(if (acc == null) "users.create" else "users.update", buildJsonObject { if (acc != null) put("id", acc.id); put("name", name.trim()); put("role", role); put("phone", phone.trim()); put("email", email.trim()); if (needsBranch) put("branchId", branch?.id) else { put("branchId", null as String?); if (title.isNotBlank()) put("title", title.trim()) }; if (acc == null) put("password", pw) })
        onSaved(if (acc == null) "«${r.str("name") ?: name}» qo‘shildi. Endi u telefon yoki e-pochta va parol bilan kira oladi." else "«${r.str("name") ?: name}» saqlandi"); null
    }) {
        Field("F.I.Sh.", name, { name = it }); Text("Rol", style = MaterialTheme.typography.labelLarge); Seg(roles.map { it to ROLE[it]!! }, role, { role = it }); Spacer(Modifier.height(8.dp))
        if (needsBranch) PickField("Filial", c.branches, branch, { it.name }, { branch = it }) else Field("Lavozim (ixtiyoriy)", title, { title = it })
        Field("Telefon", phone, { phone = it }, hint = "+998 90 123 45 67"); Field("E-pochta", email, { email = it })
        if (acc == null) Field("Boshlang‘ich parol", pw, { pw = it }, password = true, hint = "Kamida 8 ta belgi.")
    }
}

/** Telegram'ga ulash kartasi (profil va foydalanuvchilar). */
@Composable
fun TelegramCard(c: AppContainer, me: Me, users: List<Account>? = null) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.api.getJson("/v1/telegram") }
    val ro = Access.readOnly(me.role); val admin = isAdmin(me.role)
    var target by remember { mutableStateOf<Account?>(null) }; var issued by remember { mutableStateOf<Pair<String, String>?>(null) }; var err by remember { mutableStateOf<String?>(null) }
    val targets = if (admin) (users ?: emptyList()).filter { it.status == "active" && it.role != "AUDITOR" && (me.role == "OWNER" || it.role != "OWNER") && it.id != me.id } else emptyList()
    AppCard {
        Text("Telegram’ga ulash", style = MaterialTheme.typography.titleSmall); Text("Bot orqali so‘rovlarni tasdiqlash va bildirishlarni olish.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val tg = loader.data; val le = loader.error.value
        if (tg == null && le != null) { InfoBar("Telegram holatini olib bo‘lmadi.", le.userMessage(), Tone.Warning); return@AppCard }
        if (tg == null) { Text("Yuklanmoqda…", color = MaterialTheme.colorScheme.onSurfaceVariant); return@AppCard }
        val links = tg.list("links"); val pending = tg.list("pending")
        Text("Ulangan chatlar", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        if (links.isEmpty()) Text("Hali ulangan Telegram chat yo‘q.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        links.forEach { l -> Row(verticalAlignment = Alignment.CenterVertically) { Text("${l.str("name")} · chat ${l.txt("chatId")}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Badge(ROLE[l.str("role")] ?: (l.str("role") ?: ""), Tone.Accent); if (!ro) TextButton({ scope.launch { try { c.repo.cmd("tg.unlink", buildJsonObject { put("chatId", l.child("chatId") ?: JsonPrimitive("")) }); reloadKey++ } catch (e: Exception) { err = e.userMessage() } } }) { Text("Uzish") } } }
        if (pending.isNotEmpty()) { Text("Ishlatilmagan havolalar", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp)); pending.forEach { p -> Row(verticalAlignment = Alignment.CenterVertically) { Text("${ROLE[p.str("role")] ?: p.str("role")} · ${Fmt.dateTime(p.str("expiresAt"))} gacha", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); TextButton({ scope.launch { try { c.repo.cmd("tg.revokeToken", buildJsonObject { put("id", p.str("id")) }); reloadKey++ } catch (e: Exception) { err = e.userMessage() } } }) { Text("Bekor qilish") } } } }
        err?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
        if (ro) { Text("Auditor rejimida Telegram ulash mavjud emas.", style = MaterialTheme.typography.bodySmall); return@AppCard }
        if (targets.isNotEmpty()) PickField("Kim uchun havola", listOf<Account?>(null) + targets, target, { it?.let { a -> "${a.name} · ${ROLE[a.role]}" } ?: "O‘zim · ${ROLE[me.role]}" }, { target = it; issued = null })
        Button({ scope.launch { try { val r = c.repo.cmd("tg.createLinkToken", buildJsonObject { target?.let { put("userId", it.id) } }); issued = (r.str("token") ?: "") to (target?.name ?: me.name); reloadKey++ } catch (e: Exception) { err = e.userMessage() } } }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(if (issued == null) "Havola yaratish" else "Yangi havola") }
        issued?.let { (tok, who) -> val bot = uz.apteka360.app.BuildConfig.TG_BOT.trimStart('@'); val url = if (bot.isNotBlank()) "https://t.me/$bot?start=$tok" else null
            CopyRow(if (url != null) "Havola" else "Botga yuboring", url ?: "/start $tok")
            if (url != null) OutlinedButton({ ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }, Modifier.fillMaxWidth()) { Text("Telegram’da ochish") }
            Text("Havola bir marta ishlaydi va 24 soat amal qiladi." + (if (target != null) " Bu havola «$who» uchun — faqat shu xodimga yuboring." else ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

// ═════════════ Rollar va ruxsatlar ═════════════
@Composable
fun RolesScreen(c: AppContainer, me: Me) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.api.getJson("/v1/roles") }
    var sel by rememberSaveable { mutableStateOf("MANAGER") }; val draftA = remember { mutableStateMapOf<String, Boolean>() }; val draftS = remember { mutableStateMapOf<String, Boolean>() }
    var reason by remember { mutableStateOf("") }; var toast by remember { mutableStateOf<String?>(null) }
    ScreenScaffold("Rollar va ruxsatlar", "Jadval faqat cheklay oladi: asosiy huquqdan tashqari ruxsat qo‘shib bo‘lmaydi.") {
        DataBox(loader) { doc ->
            val roles = doc.list("roles").mapNotNull { runCatching { uz.apteka360.app.data.json.decodeFromJsonElement(RolePermissions.serializer(), it) }.getOrNull() }
            val r = roles.firstOrNull { it.code == sel } ?: RolePermissions(sel, locked = true)
            val ed = !Access.readOnly(me.role) && me.role == "OWNER" && !r.locked
            val actions = doc.list("actions"); val sens = doc.list("sensitive")
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Seg(ROLE_CODES.map { it to ROLE[it]!! }, sel, { sel = it; draftA.clear(); draftS.clear() }) }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                if (r.locked) item { InfoBar("Egasi roli o‘zgartirilmaydi.", "Egasi barcha ruxsatlarga ega.") } else if (me.role != "OWNER") item { InfoBar("Faqat ko‘rish.", "Ruxsatlarni faqat Egasi o‘zgartiradi.") }
                item { AppCard { Text("Filial doirasi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(SCOPES[sel] ?: "", fontWeight = FontWeight.SemiBold) } }
                actions.groupBy { it.str("group") ?: "" }.forEach { (g, list) -> sectionTitle(g, "g-$g"); item("ga-$g") { AppCard { list.forEach { a -> val k = a.str("key") ?: ""; SwitchRow(a.str("label") ?: k, draftA[k] ?: (r.actions[k] == true), { v -> if (v == (r.actions[k] == true)) draftA.remove(k) else draftA[k] = v }, sub = k, enabled = ed) } } } }
                sectionTitle("Maxfiy ruxsatlar · AUDIT")
                item { AppCard { sens.forEach { s -> val k = s.str("key") ?: ""; SwitchRow(s.str("label") ?: k, draftS[k] ?: (r.sensitive[k] == true), { v -> if (v == (r.sensitive[k] == true)) draftS.remove(k) else draftS[k] = v }, enabled = ed) }; Text("Maxfiy ma’lumotni har bir ko‘rish audit jurnaliga yoziladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                r.updatedAt?.let { item { Text("Oxirgi o‘zgarish: ${r.updatedBy ?: "—"} · ${Fmt.dateTime(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                if (ed) item { AppCard { Field("Sabab (kamida 3 belgi)", reason, { reason = it }); Button(enabled = (draftA.isNotEmpty() || draftS.isNotEmpty()) && reason.trim().length >= 3, onClick = { scope.launch { try { c.repo.cmd("roles.update", buildJsonObject { put("code", sel); putJsonObject("actions") { draftA.forEach { (k, v) -> put(k, v) } }; putJsonObject("sensitive") { draftS.forEach { (k, v) -> put(k, v) } }; put("reason", reason.trim()) }); toast = "Ruxsatlar saqlandi. Foydalanuvchilar keyingi so‘rovda yangi ruxsat bilan ishlaydi."; draftA.clear(); draftS.clear(); reason = ""; reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }, modifier = Modifier.fillMaxWidth()) { Text("O‘zgarishlarni saqlash") } } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═════════════ Modullar ═════════════
private val MODULES = listOf("ops", "expiry", "hr", "suppliers", "doctors", "vision", "analytics", "tax", "admin", "cameras", "aiVerify")
private val OFF_BY_DEFAULT = setOf("cameras", "aiVerify")
private val MOD_NAME = mapOf("dashboard" to "Dashboard", "system" to "Tizim", "ops" to "Operatsiyalar", "hr" to "HR", "suppliers" to "Firmalar", "doctors" to "Vrachlar", "vision" to "AI nazorat", "analytics" to "Analitika", "expiry" to "Sroklar", "tax" to "Soliq", "admin" to "Boshqaruv", "cameras" to "Kameralar (ulash va jonli video)", "aiVerify" to "AI tekshiruv")
private val MOD_DESC = mapOf("dashboard" to "Bosh sahifa, KPI va grafiklar", "system" to "Foydalanuvchilar, rollar, sozlamalar, modullar", "ops" to "Filiallar, kassa yopilishi, harajatlar, pul oqimi", "hr" to "Xodimlar, grafik, davomat, jonli filial, oylik", "suppliers" to "Firmalar, qarzlar va to‘lovlar", "doctors" to "Vrachlar bilan kelishuvlar va ROI (maxfiy)", "vision" to "Kamera va Edge-AI hodisalari, video devor", "analytics" to "P&L, analitika, prognoz, hisobotlar", "expiry" to "Srogi o‘tgan/yaqin dorilar, zarar va ko‘chirish", "tax" to "my.soliq.uz: kassa savdosi, e-faktura, soliq qarzi", "admin" to "Tasdiqlar, bildirishlar, audit jurnali", "cameras" to "Kamera qo‘shish, tekshirish, qidirish va jonli video. Filialda Edge server kerak.", "aiVerify" to "Edge-AI hodisalarini kadrlar bo‘yicha vision model bilan tekshirish.")

@Composable
fun ModulesScreen(c: AppContainer, me: Me) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.api.getJson("/v1/workspace") }
    val canEdit = isAdmin(me.role); var toast by remember { mutableStateOf<String?>(null) }
    val local = remember { mutableStateMapOf<String, Boolean>() }
    ScreenScaffold("Modullar", "O‘chirilgan modul menyudan yo‘qoladi, ma’lumot o‘chmaydi") {
        DataBox(loader) { ws ->
            val mods = ws.child("modules")
            fun on(m: String) = local[m] ?: if (m in OFF_BY_DEFAULT) mods.bool(m) == true else mods.bool(m) != false
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!canEdit) item { InfoBar("Faqat ko‘rish.", "Modullarni faqat egasi yoki super-admin yoqib-o‘chira oladi.") }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                item { AppCard(padding = PaddingValues(0.dp)) {
                    (listOf("dashboard") + MODULES + listOf("system")).forEachIndexed { i, m -> if (i > 0) HorizontalDivider(); val core = m == "dashboard" || m == "system"
                        ListRow(MOD_NAME[m] ?: m, MOD_DESC[m], trailing = { if (core) Badge("Asosiy", Tone.Neutral) else Switch(checked = on(m), enabled = canEdit, onCheckedChange = { v -> local[m] = v; scope.launch { try { c.api.request("POST", "/v1/workspace/apply", buildJsonObject { put("actions", buildJsonArray { add(buildJsonObject { put("type", "setModule"); put("module", m); put("enabled", v) }) }) }); toast = "«${MOD_NAME[m]}» moduli ${if (v) "yoqildi" else "o‘chirildi"}" } catch (e: Exception) { local[m] = !v; toast = e.userMessage() } } }) }) } } }
                item { Text("Modul o‘chirilishi faqat web-paneldagi ko‘rinishga ta’sir qiladi. Telegram bot, mobil ilovalar va backend ishlashda davom etadi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item { AppCard { Text("Ranglar", style = MaterialTheme.typography.titleSmall); ws.child("theme").obj?.entries?.forEach { (k, v) -> KeyValueRow(k, v.text) }; if (ws.child("theme").obj.isNullOrEmpty()) Text("Standart ranglar", color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Ranglarni web panel yoki desktop ilovada o‘zgartiring.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═════════════ AI sozlamalari ═════════════
@Composable
fun AiSettingsScreen(c: AppContainer, me: Me) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.api.getJson("/v1/ai/settings") to runCatching { c.api.getJson("/v1/ai/models") }.getOrNull() }
    val canEdit = isAdmin(me.role); var toast by remember { mutableStateOf<String?>(null) }
    ScreenScaffold("AI sozlamalari", "Lokal LLM yoki OpenRouter/OpenAI") {
        DataBox(loader) { (s, models) ->
            val hasOa = s.child("openai") is JsonObject
            var mode by remember(s) { mutableStateOf(s.str("mode") ?: "auto") }; var temp by remember(s) { mutableStateOf(s.txt("temperature")) }
            var baseUrl by remember(s) { mutableStateOf(s.child("local").str("baseUrl") ?: "") }; var lm by remember(s) { mutableStateOf(s.child("local").str("model") ?: "") }
            var oaModel by remember(s) { mutableStateOf(s.child("openai").str("model") ?: "gpt-4o-mini") }; var oaKey by remember { mutableStateOf("") }; var oaVis by remember(s) { mutableStateOf(s.child("openai").str("visionModel") ?: "") }
            var orModel by remember(s) { mutableStateOf(s.child("openrouter").str("model") ?: "") }; var orKey by remember { mutableStateOf("") }; var orVis by remember(s) { mutableStateOf(s.child("openrouter").str("visionModel") ?: "") }; var vLocal by remember(s) { mutableStateOf(s.str("visionModel") ?: "") }
            var testOut by remember { mutableStateOf<String?>(null) }
            fun payload(orK: String? = orKey.takeIf { it.isNotBlank() }, oaK: String? = oaKey.takeIf { it.isNotBlank() }, clearOr: Boolean = false, clearOa: Boolean = false) = buildJsonObject {
                put("mode", mode); put("temperature", temp.replace(',', '.').toDoubleOrNull() ?: 0.0); put("visionModel", vLocal.ifBlank { null })
                putJsonObject("local") { put("baseUrl", baseUrl.trim()); put("model", lm.trim()) }
                putJsonObject("openrouter") { put("model", orModel.trim()); put("visionModel", orVis.ifBlank { null }); if (clearOr) put("key", null as String?) else if (orK != null) put("key", orK) }
                if (hasOa) putJsonObject("openai") { put("model", oaModel.ifBlank { "gpt-4o-mini" }); put("visionModel", oaVis.ifBlank { null }); if (clearOa) put("key", null as String?) else if (oaK != null) put("key", oaK) }
            }
            suspend fun save(p: JsonObject) { c.api.request("PUT", "/v1/ai/settings", p); orKey = ""; oaKey = "" }
            fun test(target: String) = scope.launch { testOut = "Tekshirilmoqda…"; testOut = try { save(payload()); val t = uz.apteka360.app.data.json.parseToJsonElement(c.api.request("POST", "/v1/ai/test", buildJsonObject { put("target", target) })); if (t.bool("ok") == true) "Ishlayapti · ${t.str("model") ?: ""} · ${t.long("ms")} ms" else "Xato: ${t.str("error") ?: ""}" } catch (e: Exception) { "Xato: ${e.userMessage()}" } }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!canEdit) item { InfoBar("Faqat ko‘rish.", "AI sozlamalarini faqat egasi yoki super-admin o‘zgartira oladi.") }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                testOut?.let { item { InfoBar(it) } }
                item { AppCard { Text("Ishlash tartibi", style = MaterialTheme.typography.titleSmall); Seg(listOf("auto" to "Avto", "local" to "Faqat lokal") + (if (hasOa) listOf("openai" to "Faqat OpenAI") else emptyList()) + listOf("openrouter" to "Faqat OpenRouter"), mode, { if (canEdit) mode = it }); Field("Ijodkorlik (temperature)", temp, { temp = it }, number = true, enabled = canEdit) } }
                item { AppCard { Text("Lokal LLM", style = MaterialTheme.typography.titleSmall); Text("Ollama: http://localhost:11434/v1 · LM Studio: http://localhost:1234/v1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Field("Server manzili", baseUrl, { baseUrl = it }, enabled = canEdit); Field("Model", lm, { lm = it }, enabled = canEdit)
                    val found = models.list("local"); if (found.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { found.take(4).forEach { m -> FilterChip(selected = lm == m.text, onClick = { if (canEdit) lm = m.text }, label = { Text(m.text) }) } } else Text("Lokal server javob bermadi — Ollama yoki LM Studio ishga tushirilganini tekshiring.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (canEdit) OutlinedButton({ test("local") }) { Text("Tekshirish") } } }
                if (hasOa) item { AppCard { Text("OpenAI", style = MaterialTheme.typography.titleSmall); Field("Model", oaModel, { oaModel = it }, enabled = canEdit); Field("API kalit", oaKey, { oaKey = it }, password = true, enabled = canEdit, hint = if (s.child("openai").bool("hasKey") == true) "Kalit saqlangan: ${s.child("openai").str("keyHint") ?: ""}" else "Kalit kiritilmagan"); Field("Vision modeli", oaVis, { oaVis = it }, enabled = canEdit)
                    if (canEdit) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ test("openai") }) { Text("Tekshirish") }; if (s.child("openai").bool("hasKey") == true) OutlinedButton({ scope.launch { try { save(payload(clearOa = true)); reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }) { Text("Kalitni o‘chirish", color = Danger) } } } }
                item { AppCard { Text("OpenRouter", style = MaterialTheme.typography.titleSmall); Field("Model", orModel, { orModel = it }, enabled = canEdit, hint = "openai/gpt-4o-mini, anthropic/claude-sonnet-4.5 …"); Field("API kalit", orKey, { orKey = it }, password = true, enabled = canEdit, hint = if (s.child("openrouter").bool("hasKey") == true) "Kalit saqlangan: ${s.child("openrouter").str("keyHint") ?: ""}" else "Kalit kiritilmagan")
                    if (canEdit) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ test("openrouter") }) { Text("Tekshirish") }; if (s.child("openrouter").bool("hasKey") == true) OutlinedButton({ scope.launch { try { save(payload(clearOr = true)); reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }) { Text("Kalitni o‘chirish", color = Danger) } } } }
                item { AppCard { Text("Vision modeli (AI tekshiruv)", style = MaterialTheme.typography.titleSmall); Field("Lokal vision modeli", vLocal, { vLocal = it }, enabled = canEdit); Field("OpenRouter vision modeli", orVis, { orVis = it }, enabled = canEdit); if (canEdit) OutlinedButton({ test("vision") }) { Text("Tekshirish") } } }
                if (canEdit) item { Button({ scope.launch { try { save(payload()); toast = "AI sozlamalari saqlandi"; reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }, Modifier.fillMaxWidth()) { Text("Saqlash") } }
                item { InfoBar("Xavfsizlik qolipi", "AI kod yozmaydi — faqat ruxsat etilgan amallarni taklif qiladi. Har amal serverda tekshiriladi va audit jurnaliga yoziladi. AI faqat rolingiz ko‘ra oladigan ma’lumotni oladi.") }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

// ═════════════ Sozlamalar ═════════════
@Composable
fun SettingsScreen(c: AppContainer, me: Me, go: (String, String?) -> Unit) {
    val scope = rememberCoroutineScope()
    var reloadKey by remember { mutableStateOf(0) }
    val loader = rememberLoader(reloadKey) { c.repo.settings() }
    val ro = !isAdmin(me.role)
    var tab by rememberSaveable { mutableStateOf("limitlar") }; var toast by remember { mutableStateOf<String?>(null) }
    ScreenScaffold("Sozlamalar") {
        DataBox(loader) { st ->
            var base by remember(st) { mutableStateOf(st.limitBase.ifBlank { "Naqd + karta" }) }; var mode by remember(st) { mutableStateOf(st.limitMode.ifBlank { "Ogohlantirish + Egasi tasdig‘i" }) }; var coef by remember(st) { mutableStateOf(Math.round(st.limitCoef).toString()) }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (Access.readOnly(me.role)) item { InfoBar("Auditor rejimi.", "Sozlamalarni faqat ko‘rish mumkin.") }
                item { Seg(listOf("limitlar" to "Limitlar va tasdiqlar", "idle" to "Idle limitlar", "kategoriya" to "Harajat kategoriyalari", "bildirish" to "Bildirishlar", "xavfsizlik" to "Xavfsizlik", "profil" to "Profil"), tab, { if (it == "profil") go("profile", null) else tab = it }) }
                toast?.let { item { InfoBar(it, tone = Tone.Accent) } }
                when (tab) {
                    "limitlar" -> {
                        item { AppCard { Text("Ertangi kun harajat limiti", style = MaterialTheme.typography.titleSmall); Text("Limit bazasi", style = MaterialTheme.typography.labelLarge); Seg(listOf("Faqat naqd" to "Faqat naqd", "Naqd + karta" to "Naqd + karta"), base, { if (!ro) base = it })
                            Field("Koeffitsient, %", coef, { coef = it.filter(Char::isDigit).take(3) }, number = true, enabled = !ro)
                            val b = if (base == "Faqat naqd") 18_450_000L else 40_790_000L; Text("Chilonzor 12, bugun: ${Fmt.num(b)} × ${coef.ifBlank { "0" }}% = ${Fmt.money(b * (coef.toLongOrNull() ?: 0) / 100)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Limitdan oshganda", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp)); Seg(listOf("Bloklash", "Ogohlantirish + Egasi tasdig‘i", "Faqat ogohlantirish").map { it to it }, mode, { if (!ro) mode = it })
                            Button(enabled = !ro, onClick = { val cf = coef.toIntOrNull() ?: 0; if (cf > 100) toast = "Koeffitsient 0–100% oralig‘ida bo‘lsin" else scope.launch { try { c.repo.cmd("settings.saveLimits", buildJsonObject { put("limitBase", base); put("limitCoef", cf); put("limitMode", mode) }); toast = "Limitlar saqlandi. Ertangi kundan amal qiladi."; reloadKey++ } catch (e: Exception) { toast = e.userMessage() } } }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Saqlash") } } }
                        item { AppCard { Text("Tasdiqlash zanjiri", style = MaterialTheme.typography.titleSmall); Text("Qiymatlar serverdan; bu chegaralar hozircha faqat ko‘rish uchun.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); KeyValueRow("Filial mudiri — gacha (X)", Fmt.money(st.approvalX)); KeyValueRow("Moliya admin — gacha (Y)", Fmt.money(st.approvalY)); KeyValueRow("Kassa farqi chegarasi", Fmt.money(st.cashThreshold)); KeyValueRow("Soliq stavkasi", "${st.taxRate}") } }
                    }
                    "idle" -> item { AppCard(padding = PaddingValues(0.dp)) { listOf(Triple("Farmatsevt", "8 soat", "P-04 · 150 000"), Triple("Katta farmatsevt", "8 soat", "P-04 · 150 000"), Triple("Kassir", "6 soat", "P-04 · 150 000"), Triple("Omborchi", "10 soat", "P-04 · 100 000"), Triple("Filial mudiri", "hisoblanmaydi", "—")).forEachIndexed { i, (a, b, cc) -> if (i > 0) HorizontalDivider(); ListRow(a, cc, trailing = { Text(b) }) }; Text("Rejadagi tanaffus va dam olish xonasidagi vaqt idle hisoblanmaydi. Faqat inson tasdiqlagan AI hodisalari hisobga olinadi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) } }
                    "kategoriya" -> item { AppCard(padding = PaddingValues(0.dp)) { listOf("Xodimlar" to "12 mln/oy", "Kommunal" to "18 mln/oy", "IT" to "8 mln/oy", "Logistika" to "6 mln/oy", "Davlat / bank" to "4 mln/oy", "Tijorat" to "5 mln/oy", "Vrachlar" to "maxfiy", "Boshqa" to "2 mln/oy · izoh majburiy").forEachIndexed { i, (a, b) -> if (i > 0) HorizontalDivider(); ListRow(a, trailing = { Text(b) }) } } }
                    "bildirish" -> item { AppCard { Text("Kanallar bo‘yicha bildirish turlari (Telegram · Push · E-pochta)", style = MaterialTheme.typography.titleSmall); listOf("Kassa farqi > chegara", "FOM sync uzilishi > 15 daq", "AI hodisa (yuqori ustuvorlik)", "Tasdiq kutilmoqda", "Firma: chegirma muddati", "Hujjat muddati tugaydi", "Kunlik hisobot 23:30").forEach { KeyValueRow(it, "Telegram · Push") }; Text("Web panelda ham bu jadval hozircha serverga saqlanmaydi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    "xavfsizlik" -> { item { AppCard { SwitchRow("Ikki bosqichli tasdiqlash (2FA)", true, {}, sub = "Yoqilgan · SMS + Telegram. Egasi, Moliya admin va Super admin uchun majburiy.", enabled = false); SwitchRow("Biometrik tasdiq (shu telefonda)", c.biometricEnabled, { c.biometricEnabled = it }, sub = "Tasdiqlar va 10 mln+ summalar uchun barmoq izi / yuz") } }; item { SessionsAndLogins(c) } }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun SessionsAndLogins(c: AppContainer) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf(0) }
    val sessions = rememberLoader(key) { c.repo.sessions() }
    val logins = rememberLoader { c.api.getJson("/v1/auth/logins") }
    var err by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Faol sessiyalar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f)); TextButton(enabled = (sessions.data?.size ?: 0) > 1, onClick = { scope.launch { try { val r = c.repo.cmd("auth.revokeOthers", buildJsonObject {}); err = "${r.long("ended") ?: 0} ta boshqa sessiya yakunlandi"; key++ } catch (e: Exception) { err = e.userMessage() } } }) { Text("Boshqalardan chiqish", color = Danger) } }
            sessions.data?.forEach { s -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) { Column(Modifier.weight(1f)) { Text(s.device, style = MaterialTheme.typography.bodyMedium); Text("${s.ip} · kirgan ${Fmt.dateTime(s.createdAt)} · faol ${Fmt.dateTime(s.lastSeen)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (s.current) Badge("Shu qurilma", Tone.Accent) else TextButton({ scope.launch { try { c.repo.revokeSession(s.sid); key++ } catch (e: Exception) { err = e.userMessage() } } }) { Text("Chiqarish") } } }
            err?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        AppCard {
            Text("Kirish tarixi", style = MaterialTheme.typography.titleSmall)
            val l = logins.data?.let { it as? kotlinx.serialization.json.JsonArray }?.toList() ?: emptyList()
            if (logins.data != null && l.isEmpty()) Text("Hali kirishlar yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant)
            l.take(30).forEach { x -> val res = x.str("result"); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) { Column(Modifier.weight(1f)) { Text(Fmt.dateTime(x.str("at")), style = MaterialTheme.typography.bodySmall); Text("${x.str("device") ?: ""} · ${x.str("ip") ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Badge(when (res) { "ok" -> "Muvaffaqiyatli"; "failed" -> "Parol noto‘g‘ri"; "blocked" -> "Bloklandi"; else -> res ?: "" }, when (res) { "ok" -> Tone.Accent; "failed" -> Tone.Warning; "blocked" -> Tone.Danger; else -> Tone.Info }) } }
        }
    }
}
