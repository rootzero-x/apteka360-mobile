package uz.apteka360.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.InlineError
import uz.apteka360.app.ui.components.KeyValueRow

@Composable
fun AuthFlow(vm: AuthViewModel) {
    val ui by vm.ui.collectAsState()
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Header(showBack = ui.step !is AuthStep.Login, onBack = { vm.goTo(AuthStep.Login) })
        Spacer(Modifier.height(24.dp))
        when (val s = ui.step) {
            AuthStep.Login -> LoginForm(ui, vm::login, onForgot = { vm.goTo(AuthStep.Forgot) })
            is AuthStep.Otp -> OtpForm(ui, s, vm::otp, vm::resend)
            is AuthStep.NewDeviceConfirm -> NewDeviceForm(ui, s, vm::device)
            AuthStep.Forgot -> ForgotForm(ui, vm::forgot)
            is AuthStep.ResetPassword -> ResetForm(ui, vm::reset)
        }
    }
}

@Composable
private fun Header(showBack: Boolean, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showBack) IconButton(onClick = onBack, modifier = Modifier.padding(end = 4.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") }
        Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = Color.White)
        }
        Spacer(Modifier.size(12.dp))
        Text("Apteka 360", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Title(title: String, sub: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(6.dp))
    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun PrimaryButton(text: String, busy: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
        if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Messages(ui: AuthUi) {
    InlineError(ui.error)
    if (ui.info != null) {
        Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small).padding(12.dp)) {
            Text(ui.info, color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (ui.error != null || ui.info != null) Spacer(Modifier.height(12.dp))
}

@Composable
private fun LoginForm(ui: AuthUi, onLogin: (String, String) -> Unit, onForgot: () -> Unit) {
    var login by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    var show by rememberSaveable { mutableStateOf(false) }
    Title("Tizimga kirish", "Telefon raqam yoki e-pochta va parolingizni kiriting.")
    Messages(ui)
    OutlinedTextField(login, { login = it }, label = { Text("Telefon yoki e-pochta") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(pass, { pass = it }, label = { Text("Parol") }, singleLine = true,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = { IconButton({ show = !show }) { Icon(if (show) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } },
        modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onForgot) { Text("Parolni unutdingizmi?") } }
    Spacer(Modifier.height(8.dp))
    PrimaryButton("Kirish", ui.busy) { onLogin(login, pass) }
}

@Composable
private fun OtpForm(ui: AuthUi, s: AuthStep.Otp, onOtp: (String) -> Unit, onResend: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val via = when {
        s.channels.contains("telegram") && s.channels.contains("sms") -> "SMS yoki Telegram"
        s.channels.contains("telegram") -> "Telegram"
        else -> "SMS"
    }
    Title("Tasdiqlash kodi", "$via orqali ${s.login} ga yuborilgan 6 xonali kodni kiriting. Kod ${s.ttl / 60} daqiqa amal qiladi.")
    Messages(ui)
    if (ui.attemptsLeft != null) { Text("Qolgan urinishlar: ${ui.attemptsLeft}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall); Spacer(Modifier.height(8.dp)) }
    OutlinedTextField(code, { if (it.length <= 6 && it.all(Char::isDigit)) code = it }, label = { Text("Kod") }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        textStyle = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center),
        modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Davom etish", ui.busy, enabled = code.length == 6) { onOtp(code) }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        TextButton(onClick = onResend, enabled = ui.resendIn == 0 && !ui.busy) {
            Text(if (ui.resendIn > 0) "Qayta yuborish (${ui.resendIn} s)" else "Kodni qayta yuborish")
        }
    }
}

@Composable
private fun NewDeviceForm(ui: AuthUi, s: AuthStep.NewDeviceConfirm, onDecide: (Boolean) -> Unit) {
    Title("Yangi qurilma", "Bu qurilmadan birinchi marta kiryapsiz. Ma’lumotlar to‘g‘ri bo‘lsa, davom eting.")
    Messages(ui)
    AppCard {
        KeyValueRow("Qurilma", s.device.device.ifBlank { "—" })
        KeyValueRow("Joylashuv", s.device.location.ifBlank { "—" })
        KeyValueRow("IP manzil", s.device.ip.ifBlank { "—" })
    }
    Spacer(Modifier.height(20.dp))
    PrimaryButton("Bu men — davom etish", ui.busy) { onDecide(true) }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = { onDecide(false) }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.medium) {
        Text("Bu men emas — bloklash", color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(12.dp))
    Text("«Bloklash» hisobni qulflaydi va barcha sessiyalarni yopadi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ForgotForm(ui: AuthUi, onSend: (String) -> Unit) {
    var login by rememberSaveable { mutableStateOf("") }
    Title("Parolni tiklash", "Telefon yoki e-pochtani kiriting — tasdiqlash kodi Telegram (yoki SMS) orqali keladi.")
    Messages(ui)
    OutlinedTextField(login, { login = it }, label = { Text("Telefon yoki e-pochta") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Kod yuborish", ui.busy) { onSend(login) }
}

@Composable
private fun ResetForm(ui: AuthUi, onReset: (String, String) -> Unit) {
    var p1 by rememberSaveable { mutableStateOf("") }
    var p2 by rememberSaveable { mutableStateOf("") }
    Title("Yangi parol", "Kamida 8 belgi. Tiklashdan so‘ng barcha qurilmalardan qayta kirish kerak bo‘ladi.")
    Messages(ui)
    OutlinedTextField(p1, { p1 = it }, label = { Text("Yangi parol") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(p2, { p2 = it }, label = { Text("Parolni takrorlang") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(16.dp))
    PrimaryButton("Saqlash", ui.busy) { onReset(p1, p2) }
}
