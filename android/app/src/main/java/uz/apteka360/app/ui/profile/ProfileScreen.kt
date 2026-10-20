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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uz.apteka360.app.AppContainer
import uz.apteka360.app.BuildConfig
import uz.apteka360.app.data.Me
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.Field
import uz.apteka360.app.ui.components.InfoBar
import uz.apteka360.app.ui.components.InlineError
import uz.apteka360.app.ui.components.Seg
import uz.apteka360.app.ui.components.Tone
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.components.userMessage
import uz.apteka360.app.ui.screens.ROLE
import uz.apteka360.app.ui.screens.ScreenScaffold
import uz.apteka360.app.ui.screens.SessionsAndLogins
import uz.apteka360.app.ui.screens.TelegramCard
import uz.apteka360.app.ui.theme.ThemePref

/** Desktop `A.screens.profile`: shaxsiy ma'lumot, parol, Telegram, sessiyalar, kirish tarixi + telefonga xos: mavzu, biometriya. */
@Composable
fun ProfileScreen(c: AppContainer, me: Me, onSignOut: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(me.name) }; var phone by remember { mutableStateOf(me.phone ?: "") }; var email by remember { mutableStateOf(me.email ?: "") }
    var err1 by remember { mutableStateOf<String?>(null) }; var ok1 by remember { mutableStateOf<String?>(null) }
    var cur by remember { mutableStateOf("") }; var nx by remember { mutableStateOf("") }; var rp by remember { mutableStateOf("") }; var err2 by remember { mutableStateOf<String?>(null) }; var ok2 by remember { mutableStateOf<String?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    ScreenScaffold("Profil", "Shaxsiy ma’lumotlar, parol, sessiyalar va Telegram") {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (me.role == "EMPLOYEE") item { InfoBar("Xodim rejimi.", "Ish grafigi, so‘rovlar va bildirishlar «Mening ishim» bo‘limida.") }
            item { AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) { Avatar(me.ini.ifBlank { me.name.take(1) }, 56); Spacer(Modifier.width(14.dp)); Column { Text(me.name, style = MaterialTheme.typography.titleLarge); Text(listOfNotNull(me.title.ifBlank { null }, ROLE[me.role]?.takeIf { it != me.title }, me.branchId?.let { c.branchName(it) }).joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) } }
                Spacer(Modifier.height(12.dp))
                Field("F.I.Sh.", name, { name = it }); Field("Telefon", phone, { phone = it }); Field("E-pochta", email, { email = it })
                InlineError(err1); ok1?.let { InfoBar(it, tone = Tone.Accent) }
                Button({ if (name.trim().length < 3) { err1 = "F.I.Sh. kamida 3 belgi."; return@Button }; err1 = null; scope.launch { try { c.repo.cmd("profile.update", buildJsonObject { put("name", name.trim()); put("phone", phone.trim()); put("email", email.trim()) }); ok1 = "Profil saqlandi"; c.onSignedIn() } catch (e: Exception) { err1 = e.userMessage() } } }, Modifier.fillMaxWidth()) { Text("Saqlash") }
            } }
            item { AppCard {
                Text("Parolni almashtirish", style = MaterialTheme.typography.titleSmall)
                Field("Joriy parol", cur, { cur = it }, password = true); Field("Yangi parol", nx, { nx = it }, password = true, hint = "Kamida 8 ta belgi."); Field("Yangi parolni takrorlang", rp, { rp = it }, password = true)
                InlineError(err2); ok2?.let { InfoBar(it, tone = Tone.Accent) }
                Button({ err2 = when { cur.isBlank() -> "Joriy parolni kiriting."; nx.length < 8 -> "Kamida 8 ta belgi."; nx != rp -> "Yangi parollar mos emas"; else -> null }; if (err2 == null) scope.launch { try { c.repo.changePassword(cur, nx); ok2 = "Parol almashtirildi. Boshqa qurilmalardagi sessiyalar yopildi."; cur = ""; nx = ""; rp = "" } catch (e: Exception) { err2 = e.userMessage() } } }, Modifier.fillMaxWidth()) { Text("Saqlash") }
            } }
            sectionTitle("Ilova")
            item { AppCard {
                Text("Mavzu", style = MaterialTheme.typography.labelLarge)
                var theme by remember { mutableStateOf(ThemePref.mode) }
                Seg(listOf("system" to "Tizim bo‘yicha", "light" to "Yorug‘", "dark" to "Qorong‘i"), theme, { theme = it; ThemePref.set(c, it) })
                uz.apteka360.app.ui.components.SwitchRow("Biometrik tasdiq", c.biometricEnabled, { c.biometricEnabled = it }, sub = "Tasdiqlar va 10 mln+ summalar uchun barmoq izi / yuz")
            } }
            item { TelegramCard(c, me) }
            item { SessionsAndLogins(c) }
            item {
                Button(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) { Text("Chiqish") }
                Spacer(Modifier.height(12.dp))
                Text("Apteka 360 · v${BuildConfig.VERSION_NAME} · ${BuildConfig.API_BASE_URL.removePrefix("https://")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, title = { Text("Chiqish") }, text = { Text("Hisobdan chiqasizmi?") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onSignOut() }) { Text("Chiqish") } }, dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Bekor") } })
}
