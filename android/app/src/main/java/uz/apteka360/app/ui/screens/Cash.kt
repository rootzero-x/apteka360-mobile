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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.CashChannels
import uz.apteka360.app.data.DeskState
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.Settings
import uz.apteka360.app.data.long
import uz.apteka360.app.data.str
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.InlineError
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.KpiMoney
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger
import java.util.UUID

data class CashData(val desk: DeskState, val fom: CashChannels?, val settings: Settings, val branchId: String)

/** Desktop `A.screens.cash`: FOM bugun, kassani yopish, bugungi yopilishlar, ziddiyat (409). */
@Composable
fun CashScreen(c: AppContainer, me: Me, initialBranch: String?) {
    val network = me.scope == "all" || me.branchIds.size > 1
    var bid by rememberSaveable { mutableStateOf(initialBranch ?: me.branchId ?: "") }
    val loader = rememberLoader(bid) {
        val br = c.branchesCached()
        val b = bid.ifBlank { me.branchId ?: br.firstOrNull()?.id ?: "" }
        val desk = c.repo.desktop(if (network) b else null)
        val fom = runCatching { c.repo.fomCashToday() }.getOrDefault(emptyMap())[desk.branchId.ifBlank { b }]
        CashData(desk, fom, runCatching { c.repo.settings() }.getOrDefault(Settings()), desk.branchId.ifBlank { b })
    }
    ScreenScaffold("Kassa", "Kunlik yopilish va FOM bilan solishtirish") {
        DataBox(loader) { x ->
            val b = c.branches.firstOrNull { it.id == x.branchId }
            val threshold = x.settings.cashThreshold.takeIf { it > 0 } ?: 50000L
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (network && c.branches.isNotEmpty()) item { Seg(c.branches.map { it.id to it.label }, x.branchId, { bid = it }) }
                x.desk.serverEdit?.let { e -> item { InfoBar("Web-panelda tahrir bor.", "${e.by} · ${e.time} · naqd ${Fmt.money(e.naqd)}" + (if (e.note.isNotBlank()) " · «${e.note}»" else ""), Tone.Warning) } }
                val fom = x.fom
                if (fom != null) {
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("Naqd (FOM)", fom.naqd, Modifier.weight(1f)); KpiMoney("Karta (FOM)", fom.karta, Modifier.weight(1f)) } }
                    item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { KpiMoney("QR (FOM)", fom.qr, Modifier.weight(1f)); KpiMoney("Jami (FOM)", fom.naqd + fom.karta + fom.qr, Modifier.weight(1f), foot = b?.label) } }
                } else item { InfoBar("FOM ma’lumoti yo‘q.", "Bu filial uchun bugungi FOM yig‘indisi kelmagan — kiritilgan summalar solishtirilmaydi.") }
                if (!Access.readOnly(me.role)) {
                    sectionTitle("Kassani yopish")
                    item { CloseForm(c, x, fom, threshold, network, b?.desks ?: 1, onDone = { }) }
                }
                sectionTitle("Bugungi yopilishlar")
                if (x.desk.closings.isEmpty()) item { AppCard { Text("Bugun hali yopilish yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                else x.desk.closings.forEach { cl -> item { AppCard(padding = PaddingValues(0.dp)) {
                    val (l, t) = when (cl.status) { "accepted" -> "Qabul qilindi" to Tone.Accent; "review" -> "Tasdiqda" to Tone.Warning; "synced" -> "Sinxronlandi" to Tone.Accent; else -> cl.status to Tone.Neutral }
                    ListRow("${cl.kassa} · ${Fmt.money(cl.naqd + cl.karta + cl.qr)}", listOf(cl.by, cl.reason ?: "").filter { it.isNotBlank() }.joinToString(" · "), trailing = { Badge(l, t) })
                } } }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun CloseForm(c: AppContainer, x: CashData, fom: CashChannels?, threshold: Long, network: Boolean, desks: Int, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var kassa by rememberSaveable { mutableStateOf("Kassa 1") }
    var naqd by rememberSaveable { mutableStateOf("") }; var karta by rememberSaveable { mutableStateOf("") }; var qr by rememberSaveable { mutableStateOf("") }
    var reason by rememberSaveable { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf<Pair<JsonObject, JsonObject>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var ok by remember { mutableStateOf<String?>(null) }
    fun p(s: String) = s.filter(Char::isDigit).toLongOrNull() ?: 0L
    val any = naqd.isNotBlank() || karta.isNotBlank() || qr.isNotBlank()
    val sum = if (fom != null && any) (p(naqd) - fom.naqd) + (p(karta) - fom.karta) + (p(qr) - fom.qr) else null
    val need = fom != null && any && kotlin.math.abs(sum ?: 0) > threshold
    fun input(): JsonObject = buildJsonObject {
        put("queueId", UUID.randomUUID().toString()); put("kassa", kassa); put("naqd", p(naqd)); put("karta", p(karta)); put("qr", p(qr))
        put("reason", reason.trim().ifBlank { null }); put("offline", false)
        x.desk.version?.let { put("baseVersion", it) }
        if (network) put("branchId", x.branchId)
    }
    fun submit(body: JsonObject) {
        busy = true; err = null
        scope.launch {
            try { val r = c.repo.deskCashClose(body); ok = "Kassa yopildi: $kassa · ${Fmt.money(p(naqd) + p(karta) + p(qr))}" + (if (r.str("status") == "review") " · moliya tasdig‘iga yuborildi" else ""); naqd = ""; karta = ""; qr = ""; reason = ""; onDone() }
            catch (e: ApiException) { if (e.status == 409 && e.details.long("naqd") != null) conflict = body to (e.details as JsonObject) else err = e.message }
            catch (e: Exception) { err = e.userMessage() } finally { busy = false }
        }
    }
    AppCard {
        Seg((1..desks.coerceAtLeast(1)).map { "Kassa $it" to "Kassa $it" }, kassa, { kassa = it })
        Spacer(Modifier.height(8.dp))
        listOf(Triple("Naqd", naqd, fom?.naqd) to { v: String -> naqd = v }, Triple("Karta", karta, fom?.karta) to { v: String -> karta = v }, Triple("QR / Click / Payme", qr, fom?.qr) to { v: String -> qr = v }).forEach { (t, set) ->
            val (label, value, f) = t
            val d = if (f != null && value.isNotBlank()) p(value) - f else null
            OutlinedTextField(value, { set(it.filter(Char::isDigit)) }, label = { Text(label) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                supportingText = { Text((if (f != null) "FOM: ${Fmt.num(f)}" else "FOM: —") + (if (d != null) "  ·  farq ${if (d > 0) "+" else ""}${Fmt.num(d)}" else ""), color = if (d != null && kotlin.math.abs(d) > threshold) Danger else MaterialTheme.colorScheme.onSurfaceVariant) })
            Spacer(Modifier.height(4.dp))
        }
        KeyValueRow("FOM bilan umumiy farq", if (sum != null) (if (sum > 0) "+" else "") + Fmt.money(sum) else "—", valueColor = if (need) Danger else MaterialTheme.colorScheme.primary)
        OutlinedTextField(reason, { reason = it }, label = { Text(if (need) "Sabab (majburiy — farq chegaradan katta)" else "Izoh (ixtiyoriy)") }, modifier = Modifier.fillMaxWidth())
        Text("Chegara: ${Fmt.money(threshold)}. Undan katta farqda sabab majburiy va yopilish moliya tasdig‘iga tushadi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        InlineError(err)
        ok?.let { Spacer(Modifier.height(6.dp)); InfoBar(it, tone = Tone.Accent) }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !busy, onClick = {
                err = null; ok = null
                if (!any) { err = "Kamida bitta summani kiriting."; return@Button }
                if (need && reason.isBlank()) { err = "Farq chegaradan katta — sabab majburiy."; return@Button }
                confirm = true
            }, modifier = Modifier.weight(1f)) { Text("Kassani yopish") }
            OutlinedButton({ naqd = ""; karta = ""; qr = ""; reason = ""; err = null; ok = null }) { Text("Tozalash") }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("$kassa yopilsinmi?") }, text = {
        Column { KeyValueRow("Naqd", Fmt.money(p(naqd))); KeyValueRow("Karta", Fmt.money(p(karta))); KeyValueRow("QR", Fmt.money(p(qr))); KeyValueRow("Jami", Fmt.money(p(naqd) + p(karta) + p(qr))); KeyValueRow("FOM bilan farq", if (sum != null) Fmt.money(sum) else "—") }
    }, confirmButton = { TextButton({ confirm = false; submit(input()) }) { Text("Yopish") } }, dismissButton = { TextButton({ confirm = false }) { Text("Bekor") } })
    conflict?.let { (body, srv) ->
        var how by remember { mutableStateOf("both") }
        AlertDialog(onDismissRequest = { conflict = null }, title = { Text("Ziddiyat: ikki joyda kiritilgan") }, text = {
            Column {
                Text("Bu kassa yopilishi web-panelda ham kiritilgan. Qaysi versiyani saqlash kerak?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                KeyValueRow("Telefonda (naqd)", Fmt.money(body.long("naqd"))); KeyValueRow("Serverda (naqd)", Fmt.money(srv.long("naqd")))
                Text("${srv.str("by") ?: ""} · ${srv.str("time") ?: ""}" + (srv.str("note")?.let { " · «$it»" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                listOf("local" to "Telefondagini saqlash (mudir/Egasi)", "server" to "Serverdagini qabul qilish", "both" to "Ikkalasini moliya tasdig‘iga yuborish").forEach { (id, l) -> FilterChip(selected = how == id, onClick = { how = id }, label = { Text(l) }) }
            }
        }, confirmButton = { TextButton({
            val b = body; conflict = null; busy = true
            scope.launch { try { val r = c.repo.deskResolveConflict(buildJsonObject { put("queueId", UUID.randomUUID().toString()); put("how", how); put("local", b) }); ok = if (r.str("status") == "review") "Ziddiyat hal qilindi — moliya tasdig‘iga yuborildi" else "Ziddiyat hal qilindi — sinxronlandi"; onDone() } catch (e: Exception) { err = e.userMessage() } finally { busy = false } }
        }) { Text("Davom etish") } }, dismissButton = { TextButton({ conflict = null }) { Text("Bekor") } })
    }
}
