package uz.apteka360.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uz.apteka360.app.ui.components.InlineError
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.som

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSheets(sheet: Sheet, ui: HomeUi, vm: HomeViewModel, onDismiss: () -> Unit) {
    val st = ui.state ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var localError by remember { mutableStateOf<String?>(null) }
    fun submit(success: String, block: suspend () -> uz.apteka360.app.data.MobileState) {
        localError = null
        vm.command(success, block); onDismiss()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            when (sheet) {
                Sheet.Advance -> {
                    var amount by remember { mutableStateOf("") }
                    var note by remember { mutableStateOf("") }
                    SheetTitle("Avans so‘rash", "Limit: ${st.advanceLimit.som()} (oylikning 30% gacha)")
                    OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Summa, so‘m") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(note, { note = it }, label = { Text("Izoh") }, modifier = Modifier.fillMaxWidth())
                    InlineError(localError)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val a = amount.toLongOrNull()
                        if (a == null || a <= 0) localError = "Summani kiriting" else submit("Avans so‘rovi yuborildi") { vm.repo.advanceRequest(a, note) }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Yuborish") }
                }
                Sheet.Leave -> {
                    val types = listOf("Yillik", "Kasallik", "O‘z hisobidan")
                    var type by remember { mutableStateOf(types[0]) }
                    var label by remember { mutableStateOf("") }
                    var days by remember { mutableStateOf("") }
                    var cover by remember { mutableStateOf("") }
                    var note by remember { mutableStateOf("") }
                    SheetTitle("Ta’til so‘rash", "Qoldiq: ${st.leaveBalance} kun")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { types.forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) }) } }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(label, { label = it }, label = { Text("Sana (masalan: 14–20 oktabr)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(days, { days = it.filter(Char::isDigit) }, label = { Text("Kunlar soni") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(cover, { cover = it }, label = { Text("O‘rinbosar (ixtiyoriy)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(note, { note = it }, label = { Text("Izoh") }, modifier = Modifier.fillMaxWidth())
                    InlineError(localError)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val d = days.toIntOrNull()
                        when {
                            label.isBlank() -> localError = "Sanani tanlang"
                            d == null || d <= 0 -> localError = "Kunlar sonini kiriting"
                            else -> submit("Ta’til so‘rovi yuborildi") { vm.repo.leaveRequest(type, label, d, cover, note) }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Yuborish") }
                }
                Sheet.Swap -> {
                    var d by remember { mutableStateOf<Int?>(st.shifts.firstOrNull { it.status == "normal" }?.d) }
                    var mateId by remember { mutableStateOf(ui.colleagues.firstOrNull()?.id ?: "") }
                    var backD by remember { mutableStateOf("") }
                    var reason by remember { mutableStateOf("") }
                    SheetTitle("Smena almashish", "Qaysi smenangizni hamkasbga berasiz?")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        st.shifts.take(7).forEach { s -> FilterChip(selected = d == s.d, onClick = { d = s.d }, label = { Text("${s.d}") }, enabled = s.status == "normal" || s.status == "now") }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (ui.colleagues.isNotEmpty()) {
                        Text("Hamkasb", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ui.colleagues.forEach { e -> FilterChip(selected = mateId == e.id, onClick = { mateId = e.id }, label = { Text("${e.name} · ${e.position}") }) }
                        }
                    } else {
                        OutlinedTextField(mateId, { mateId = it }, label = { Text("Hamkasb ID (masalan e2)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text("Hamkasblar ro‘yxati bu rol uchun berilmaydi — ID’ni mudirdan so‘rang.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(backD, { backD = it.filter(Char::isDigit) }, label = { Text("Evaz smena kuni (ixtiyoriy)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(reason, { reason = it }, label = { Text("Sabab") }, modifier = Modifier.fillMaxWidth())
                    InlineError(localError)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val day = d
                        when {
                            day == null -> localError = "Smena tanlanmagan"
                            mateId.isBlank() -> localError = "Hamkasbni tanlang"
                            else -> submit("So‘rov hamkasbga yuborildi") { vm.repo.swapRequest(day, mateId.trim(), backD.toIntOrNull(), reason) }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Yuborish") }
                }
                Sheet.Appeal -> {
                    val reasons = listOf("Kamera xato aniqlagan", "Boshqa vazifada edim", "Texnik sabab", "Boshqa")
                    var reason by remember { mutableStateOf("") }
                    var text by remember { mutableStateOf("") }
                    SheetTitle("Jarimaga e’tiroz", "${st.penalty.ruleCode} · ${st.penalty.amount.som()} · muddat: ${st.penalty.appealDeadline}")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { reasons.forEach { r -> FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r) }) } }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(text, { text = it }, label = { Text("Tushuntirish") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    InlineError(localError)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { if (reason.isBlank()) localError = "Sababni tanlang" else submit("E’tiroz yuborildi") { vm.repo.appeal(reason, text) } }, modifier = Modifier.fillMaxWidth()) { Text("Yuborish") }
                }
                is Sheet.Decide -> {
                    val r = sheet.req
                    var comment by remember { mutableStateOf("") }
                    var amount by remember { mutableStateOf("") }
                    val empId = r.id.substringAfter(':', "").ifBlank { null }
                    val kind = r.id.substringBefore(':')
                    SheetTitle(r.title, r.sub)
                    r.rows.forEach { row -> KeyValueRow(row.getOrElse(0) { "" }, row.getOrElse(1) { "" }) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(comment, { comment = it }, label = { Text("Izoh (rad etishda majburiy)") }, modifier = Modifier.fillMaxWidth())
                    if (kind == "ap") {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Kamaytirilgan summa (ixtiyoriy)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    }
                    InlineError(localError)
                    Spacer(Modifier.height(16.dp))
                    if (kind == "ap") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { if (comment.isBlank()) localError = "Izoh majburiy" else submit("Jarima bekor qilindi") { vm.repo.appealDecide("cancel", null, comment, empId) } }, modifier = Modifier.weight(1f)) { Text("Bekor") }
                            OutlinedButton(onClick = {
                                val a = amount.toLongOrNull()
                                if (comment.isBlank()) localError = "Izoh majburiy" else if (a == null) localError = "Summani kiriting" else submit("Jarima kamaytirildi") { vm.repo.appealDecide("reduce", a, comment, empId) }
                            }, modifier = Modifier.weight(1f)) { Text("Kamaytirish") }
                            Button(onClick = { if (comment.isBlank()) localError = "Izoh majburiy" else submit("Jarima kuchda qoldi") { vm.repo.appealDecide("keep", null, comment, empId) } }, modifier = Modifier.weight(1f)) { Text("Qoldirish") }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                if (comment.isBlank()) localError = "Rad etishda izoh majburiy" else submit("Rad etildi") {
                                    if (kind == "sw") vm.repo.swapDecide(false, comment, empId) else vm.repo.requestDecide(r.id, false, comment)
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Rad etish", color = MaterialTheme.colorScheme.error) }
                            Button(onClick = {
                                submit("Tasdiqlandi") { if (kind == "sw") vm.repo.swapDecide(true, comment, empId) else vm.repo.requestDecide(r.id, true, comment) }
                            }, modifier = Modifier.weight(1f)) { Text("Tasdiqlash") }
                        }
                    }
                }
                is Sheet.AiDecide -> {
                    val e = sheet.item
                    SheetTitle(e.label, "${e.emp} · ${e.cam}")
                    KeyValueRow("Kod", e.code); KeyValueRow("Ishonch", "${e.conf}%"); KeyValueRow("Vaqt", e.time); if (e.dur.isNotBlank()) KeyValueRow("Davomiyligi", e.dur)
                    Spacer(Modifier.height(6.dp))
                    Text("AI faqat taklif qiladi — qarorni siz qabul qilasiz. Har bir qaror audit jurnaliga yoziladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { submit("Hodisa rad etildi") { vm.repo.aiDecide(e.id, false) } }, modifier = Modifier.weight(1f)) { Text("Rad etish") }
                        Button(onClick = { submit("Hodisa tasdiqlandi") { vm.repo.aiDecide(e.id, true) } }, modifier = Modifier.weight(1f)) { Text("Tasdiqlash") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(title: String, sub: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(16.dp))
}
