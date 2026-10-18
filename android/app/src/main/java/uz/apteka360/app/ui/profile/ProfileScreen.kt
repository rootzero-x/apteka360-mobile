package uz.apteka360.app.ui.profile

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.apteka360.app.BuildConfig
import uz.apteka360.app.data.AuthSession
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.Repository
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.Badge
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.SectionTitle
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.home.roleLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(me: Me, repo: Repository, onSignOut: () -> Unit) {
    var sessions by remember { mutableStateOf<List<AuthSession>>(emptyList()) }
    var showPassword by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    suspend fun loadSessions() { runCatching { repo.sessions() }.onSuccess { sessions = it } }
    LaunchedEffect(Unit) { loadSessions() }

    Scaffold(topBar = { TopAppBar(title = { Text("Profil") }) }, snackbarHost = { SnackbarHost(snack) }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(me.ini.ifBlank { me.name.take(1) }, 52)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(me.name, style = MaterialTheme.typography.titleLarge)
                            Text(listOf(me.title, roleLabel(me.role)).filter { it.isNotBlank() }.distinct().joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                    me.phone?.let { KeyValueRow("Telefon", it) }
                    me.email?.let { KeyValueRow("E-pochta", it) }
                    me.branchId?.let { KeyValueRow("Filial", it) }
                    if (me.branchIds.isNotEmpty()) KeyValueRow("Filiallar", me.branchIds.joinToString(", "))
                    me.position?.let { KeyValueRow("Lavozim", it) }
                }
            }
            item {
                SectionTitle("Xavfsizlik")
                AppCard {
                    OutlinedButton(onClick = { showPassword = true }, modifier = Modifier.fillMaxWidth()) { Text("Parolni almashtirish") }
                    if (sessions.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { scope.launch { runCatching { repo.revokeOthers() }.onSuccess { snack.showSnackbar("Boshqa sessiyalar yopildi"); loadSessions() }.onFailure { snack.showSnackbar(it.userMessage()) } } }, modifier = Modifier.fillMaxWidth()) { Text("Boshqa qurilmalardan chiqish") }
                    }
                }
            }
            if (sessions.isNotEmpty()) {
                item { SectionTitle("Faol sessiyalar") }
                items(sessions, key = { it.sid }) { s ->
                    AppCard(padding = PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.device.ifBlank { "Qurilma" }, style = MaterialTheme.typography.titleSmall)
                                Text("${s.ip} · ${s.lastSeen.take(16).replace('T', ' ')}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (s.current) Badge("Shu qurilma", Tone.Accent)
                            else TextButton(onClick = { scope.launch { runCatching { repo.revokeSession(s.sid) }.onSuccess { loadSessions() }.onFailure { snack.showSnackbar(it.userMessage()) } } }) { Text("Yopish") }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Chiqish") }
                Spacer(Modifier.height(12.dp))
                Text("Apteka 360 · v${BuildConfig.VERSION_NAME} · ${BuildConfig.API_BASE_URL.removePrefix("https://")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }

    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, title = { Text("Chiqish") }, text = { Text("Hisobdan chiqasizmi?") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onSignOut() }) { Text("Chiqish") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Bekor") } })

    if (showPassword) {
        var cur by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { showPassword = false }, title = { Text("Parolni almashtirish") }, text = {
            Column {
                OutlinedTextField(cur, { cur = it }, label = { Text("Joriy parol") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(next, { next = it }, label = { Text("Yangi parol (≥ 8)") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            }
        }, confirmButton = {
            TextButton(onClick = {
                if (next.length < 8) { err = "Kamida 8 belgi"; return@TextButton }
                scope.launch { runCatching { repo.changePassword(cur, next) }.onSuccess { showPassword = false; snack.showSnackbar("Parol yangilandi") }.onFailure { err = it.userMessage() } }
            }) { Text("Saqlash") }
        }, dismissButton = { TextButton(onClick = { showPassword = false }) { Text("Bekor") } })
    }
}
