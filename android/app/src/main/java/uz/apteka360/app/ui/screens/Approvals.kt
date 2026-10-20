package uz.apteka360.app.ui.screens

import uz.apteka360.app.ui.components.findFragmentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.Approval
import uz.apteka360.app.data.Me
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.DataBox
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.components.Fmt
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.InlineError
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.rememberLoader
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.theme.Danger

private fun riskTone(r: String) = when (r) { "yuqori" -> Tone.Danger; "o‘rta" -> Tone.Warning; "past" -> Tone.Accent; else -> Tone.Neutral }
private fun apStatus(s: String) = when (s) { "pending" -> "Kutilmoqda" to Tone.Warning; "approved" -> "Tasdiqlangan" to Tone.Accent; "rejected" -> "Rad etilgan" to Tone.Danger; "changes" -> "O‘zgartirish so‘ralgan" to Tone.Info; else -> s to Tone.Neutral }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalsScreen(c: AppContainer, me: Me, initialId: String?) {
    val loader = rememberLoader { c.branchesCached(); c.repo.approvals() }
    var filter by rememberSaveable { mutableStateOf("pending") }
    var sel by rememberSaveable { mutableStateOf(initialId) }
    var list by remember { mutableStateOf<List<Approval>?>(null) }
    LaunchedEffect(loader.data) { list = loader.data }
    val pend = list?.count { it.status == "pending" } ?: 0
    ScreenScaffold("Tasdiqlashlar", if (list != null) "$pend ta kutilmoqda" else null) {
        DataBox(loader) { _ ->
            val all = list ?: emptyList()
            val shown = all.filter { filter == "all" || it.status == "pending" }
            Column(Modifier.fillMaxSize()) {
                Seg(listOf("pending" to "Kutilmoqda", "all" to "Hammasi"), filter, { filter = it }, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                if (shown.isEmpty()) EmptyBox("Kutilayotgan tasdiqlash yo‘q", Modifier.fillMaxSize())
                else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { a ->
                        AppCard(padding = PaddingValues(0.dp)) {
                            ListRow(a.title, "${a.requester} · ${c.branchName(a.branchId)} · ${a.time}", trailing = {
                                Column(horizontalAlignment = Alignment.End) { if (a.amount != null) Text(Fmt.moneyShortText(a.amount), style = MaterialTheme.typography.labelLarge, color = if (a.amount < 0) Danger else MaterialTheme.colorScheme.onSurface); Badge(a.risk.ifBlank { "—" }, riskTone(a.risk)) }
                            }, onClick = { sel = a.id })
                        }
                    }
                }
            }
        }
    }
    val a = list?.firstOrNull { it.id == sel }
    if (a != null) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sel = null }, sheetState = sheet) {
            ApprovalDetail(c, me, a, onUpdate = { upd -> list = list?.map { if (it.id == upd.id) upd else it }; sel = null })
        }
    }
}

@Composable
fun ApprovalDetail(c: AppContainer, me: Me, a: Approval, onUpdate: (Approval) -> Unit) {
    val scope = rememberCoroutineScope()
    var decide by remember { mutableStateOf<String?>(null) }
    var comment by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val (stLabel, stTone) = apStatus(a.status)
    val act = androidx.compose.ui.platform.LocalContext.current.findFragmentActivity()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val big = kotlin.math.abs(a.amount ?: 0) > uz.apteka360.app.ui.components.BIOMETRIC_THRESHOLD
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
        Text(a.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(a.title, style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
            if (a.amount != null) Text(Fmt.money(a.amount), style = MaterialTheme.typography.headlineMedium, color = if (a.amount < 0) Danger else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            Badge(stLabel, stTone)
        }
        KeyValueRow("So‘rovchi", a.requester); KeyValueRow("Filial", c.branchName(a.branchId)); KeyValueRow("Vaqt", a.time); KeyValueRow("Tasdiq darajasi", a.level.ifBlank { "—" })
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("Xavf", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Badge(a.risk.ifBlank { "—" }, riskTone(a.risk)) }
        if (a.evidence.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text("Dalil", style = MaterialTheme.typography.titleSmall); Text(a.evidence, style = MaterialTheme.typography.bodyMedium) }
        if (big && a.status == "pending") { Spacer(Modifier.height(8.dp)); InfoBar("10 mln so‘mdan katta summa", "Tasdiqlash uchun barmoq izi / yuz (yoki ekran qulfi) talab qilinadi.", uz.apteka360.app.ui.components.Tone.Warning) }
        if (a.recommended.isNotBlank()) { Spacer(Modifier.height(8.dp)); InfoBar("Tavsiya", a.recommended) }
        if (a.history.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text("Tarix", style = MaterialTheme.typography.titleSmall); a.history.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        if (a.status == "pending" && !Access.readOnly(me.role)) {
            Spacer(Modifier.height(16.dp))
            if (decide == null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ decide = "approved" }, Modifier.weight(1f)) { Text("Tasdiqlash") }
                OutlinedButton({ decide = "changes" }, Modifier.weight(1f)) { Text("O‘zgartirish", maxLines = 1) }
                OutlinedButton({ decide = "rejected" }, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) { Text("Rad etish") }
            } else {
                val t = mapOf("approved" to "Tasdiqlash", "rejected" to "Rad etish", "changes" to "O‘zgartirish so‘rash")[decide]!!
                Text("$t: ${a.type}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(comment, { comment = it }, label = { Text(if (decide == "approved") "Izoh (ixtiyoriy)" else "Sabab (majburiy)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                InlineError(err)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    OutlinedButton({ decide = null; err = null }, Modifier.weight(1f)) { Text("Bekor") }
                    Button(enabled = !busy, onClick = {
                        val cm = comment.trim()
                        if (decide != "approved" && cm.isBlank()) { err = "Sababni yozing."; return@Button }
                        busy = true; err = null
                        scope.launch { try {
                            if (decide == "approved" && (big || c.biometricEnabled)) uz.apteka360.app.ui.components.Biometric.confirm(act, "Tasdiqlashni biometrik tasdiqlang", a.title + (a.amount?.let { " · " + Fmt.money(it) } ?: ""), required = big)?.let { err = it; return@launch }
                            onUpdate(c.repo.approvalsDecide(a.id, decide!!, cm)); haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        } catch (e: Exception) { err = e.userMessage() } finally { busy = false } }
                    }, modifier = Modifier.weight(1f)) { Text(t) }
                }
            }
        }
        if (Access.readOnly(me.role)) Text("Auditor rejimi — faqat o‘qish", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
    }
}
