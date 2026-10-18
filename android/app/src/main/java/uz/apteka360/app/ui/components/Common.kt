package uz.apteka360.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.NetworkException
import uz.apteka360.app.ui.theme.Danger
import uz.apteka360.app.ui.theme.Info
import uz.apteka360.app.ui.theme.Warning
import java.text.NumberFormat
import java.util.Locale

fun Long.som(): String = NumberFormat.getIntegerInstance(Locale("uz")).format(this).replace(',', ' ').replace(' ', ' ') + " so‘m"
fun Int.som(): String = toLong().som()

fun Throwable.userMessage(): String = when (this) {
    is NetworkException -> "Internet bilan aloqa yo‘q. Tarmoqni tekshirib qayta urinib ko‘ring."
    is ApiException -> message
    else -> message ?: "Noma’lum xatolik"
}

@Composable
fun AppCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(Modifier.padding(padding), content = content) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

enum class Tone { Neutral, Accent, Danger, Warning, Info }

@Composable
fun Badge(text: String, tone: Tone = Tone.Neutral) {
    val (bg, fg) = when (tone) {
        Tone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        Tone.Accent -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Tone.Danger -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        Tone.Warning -> Color(0x2EE2A336) to Warning
        Tone.Info -> Color(0x1F4F8CFF) to Info
    }
    Box(Modifier.background(bg, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

fun statusTone(status: String): Tone = when (status) {
    "approved", "confirmed", "now", "online", "ok" -> Tone.Accent
    "rejected", "blocked", "offline", "kritik", "upheld" -> Tone.Danger
    "pending", "mate", "manager", "yuqori", "appealed" -> Tone.Warning
    "unknown", "o‘rta" -> Tone.Info
    else -> Tone.Neutral
}

fun statusLabel(status: String): String = when (status) {
    "pending" -> "Kutilmoqda"; "approved" -> "Tasdiqlandi"; "rejected" -> "Rad etildi"
    "mate" -> "Hamkasb roziligi"; "manager" -> "Mudir ko‘rmoqda"
    "confirmed" -> "Tasdiqlangan"; "appealed" -> "E’tiroz yuborilgan"; "cancelled" -> "Bekor qilingan"
    "reduced" -> "Kamaytirilgan"; "upheld" -> "Kuchda qoldi"; "none" -> "Yo‘q"
    "online" -> "Onlayn"; "offline" -> "Oflayn"; "unknown" -> "Noma’lum"
    "now" -> "Hozir"; "given" -> "Berilgan"; "taken" -> "Olingan"
    else -> status
}

@Composable
fun KeyValueRow(key: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
    }
}

@Composable
fun Avatar(initials: String, size: Int = 40) {
    Box(Modifier.size(size.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
        Text(initials.take(2), color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(error: Throwable, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(if (error is NetworkException) "Internet bilan aloqa yo‘q" else "Yuklab bo‘lmadi", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(error.userMessage(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry) { Text("Qayta urinish") }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Inbox, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
fun InlineError(text: String?) {
    if (!text.isNullOrBlank()) Box(
        Modifier.fillMaxWidth().border(1.dp, Danger.copy(alpha = .3f), MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small).padding(12.dp)
    ) { Text(text, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium) }
}
