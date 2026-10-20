package uz.apteka360.app.ui.components

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.apteka360.app.ui.theme.Danger

/** Forma bottom-sheet: sarlavha, maydonlar, «Saqlash». onSubmit xato matnini qaytaradi (null — muvaffaqiyat, sheet yopiladi). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(title: String, sub: String? = null, primary: String = "Saqlash", danger: Boolean = false, onDismiss: () -> Unit, onSubmit: suspend () -> String?, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding().imePadding().verticalScroll(rememberScrollState())) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            content()
            InlineError(err)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Bekor") }
                Button(enabled = !busy, onClick = { busy = true; err = null; scope.launch { try { val e = onSubmit(); if (e == null) onDismiss() else err = e } catch (x: Exception) { err = x.userMessage() } finally { busy = false } } },
                    modifier = Modifier.weight(1f), colors = if (danger) ButtonDefaults.buttonColors(containerColor = Danger) else ButtonDefaults.buttonColors()) { Text(if (busy) "…" else primary) }
            }
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, hint: String? = null, number: Boolean = false, password: Boolean = false, minLines: Int = 1, enabled: Boolean = true) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = minLines == 1, minLines = minLines, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Number else if (password) KeyboardType.Password else KeyboardType.Text),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        supportingText = hint?.let { { Text(it) } })
}

@Composable
fun MoneyField(label: String, value: String, onChange: (String) -> Unit, hint: String? = null, enabled: Boolean = true) {
    OutlinedTextField(value, { onChange(it.filter(Char::isDigit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), singleLine = true, enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = { Text("so‘m") },
        supportingText = { Text(hint ?: value.toLongOrNull()?.let { Fmt.num(it) } ?: "") })
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, sub: String? = null, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.bodyMedium); if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

/** Tasdiq dialogi (sabab bilan yoki bebahs). */
@Composable
fun ConfirmDialog(title: String, text: String, primary: String = "Ha", danger: Boolean = false, reasonLabel: String? = null, minReason: Int = 0, onDismiss: () -> Unit, onConfirm: suspend (String) -> String?) {
    val scope = rememberCoroutineScope()
    var reason by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column { Text(text); if (reasonLabel != null) { Spacer(Modifier.height(8.dp)); OutlinedTextField(reason, { reason = it }, label = { Text(reasonLabel) }, modifier = Modifier.fillMaxWidth()) }; InlineError(err) }
    }, confirmButton = {
        TextButton(enabled = !busy, onClick = {
            if (reasonLabel != null && reason.trim().length < minReason) { err = "Sabab kamida $minReason belgi bo‘lsin."; return@TextButton }
            busy = true; scope.launch { try { val e = onConfirm(reason.trim()); if (e == null) onDismiss() else err = e } catch (x: Exception) { err = x.userMessage() } finally { busy = false } }
        }) { Text(primary, color = if (danger) Danger else MaterialTheme.colorScheme.primary) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Bekor") } })
}

/** Ro'yxatdan tanlash (bottom sheet) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> PickerSheet(title: String, items: List<T>, label: (T) -> String, sub: ((T) -> String)? = null, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 8.dp).padding(bottom = 24.dp).navigationBarsPadding().verticalScroll(rememberScrollState())) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            items.forEach { it -> ListRow(label(it), sub?.invoke(it), onClick = { onPick(it); onDismiss() }) }
            if (items.isEmpty()) Text("Ro‘yxat bo‘sh", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
    }
}

/** Tanlov maydoni: bosilganda PickerSheet ochiladi */
@Composable
fun <T> PickField(label: String, items: List<T>, selected: T?, itemLabel: (T) -> String, onPick: (T) -> Unit, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(selected?.let(itemLabel) ?: "Tanlang", color = MaterialTheme.colorScheme.onSurface) }
    }
    if (open) PickerSheet(label, items, itemLabel, onPick = onPick, onDismiss = { open = false })
}
