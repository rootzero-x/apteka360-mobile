package uz.apteka360.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.apteka360.app.ui.theme.Danger
import uz.apteka360.app.ui.theme.Info
import uz.apteka360.app.ui.theme.Warning

// ---------- Yuklash holati ----------

class Loader<T>(val state: MutableState<T?>, val error: MutableState<Throwable?>, val refreshing: MutableState<Boolean>, private val run: suspend (Boolean) -> Unit) {
    val data get() = state.value
    val loading get() = state.value == null && error.value == null
    suspend fun reload(refresh: Boolean = false) = run(refresh)
}

/** Ekran ma'lumotini yuklash: skeleton → ma'lumot / xato; pull-to-refresh bilan qayta yuklash. `key` o'zgarsa qayta yuklanadi. */
@Composable
fun <T> rememberLoader(vararg keys: Any?, fetch: suspend () -> T): Loader<T> {
    val state = remember { mutableStateOf<T?>(null) }
    val error = remember { mutableStateOf<Throwable?>(null) }
    val refreshing = remember { mutableStateOf(false) }
    val loader = remember {
        Loader(state, error, refreshing) { refresh ->
            if (refresh) refreshing.value = true
            try { state.value = fetch(); error.value = null } catch (e: Exception) { if (state.value == null || !refresh) error.value = e }
            refreshing.value = false
        }
    }
    LaunchedEffect(*keys) { state.value = null; error.value = null; loader.reload() }
    return loader
}

/** Yuklanish / xato / ma'lumot holatlarini chizadi (pull-to-refresh bilan). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DataBox(loader: Loader<T>, modifier: Modifier = Modifier, skeleton: @Composable () -> Unit = { SkeletonList() }, content: @Composable (T) -> Unit) {
    val scope = rememberCoroutineScope()
    val data = loader.state.value
    val err = loader.error.value
    Box(modifier.fillMaxSize()) {
        when {
            data == null && err == null -> skeleton()
            data == null -> ErrorBox(err!!, onRetry = { scope.launch { loader.reload() } })
            else -> PullToRefreshBox(isRefreshing = loader.refreshing.value, onRefresh = { scope.launch { loader.reload(true) } }) { content(data) }
        }
    }
}

// ---------- Skeleton ----------

@Composable
fun Shimmer(modifier: Modifier = Modifier, height: Int = 16, radius: Int = 8) {
    val t = rememberInfiniteTransition(label = "sh")
    val a by t.animateFloat(0.35f, 0.75f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    Box(modifier.height(height.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = a), RoundedCornerShape(radius.dp)))
}

@Composable
fun SkeletonList(rows: Int = 5) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Shimmer(Modifier.weight(1f), 72, 14); Shimmer(Modifier.weight(1f), 72, 14) }
        repeat(rows) { AppCard { Shimmer(Modifier.fillMaxWidth(0.6f), 16); Spacer(Modifier.height(8.dp)); Shimmer(Modifier.fillMaxWidth(0.9f), 12); Spacer(Modifier.height(6.dp)); Shimmer(Modifier.fillMaxWidth(0.4f), 12) } }
    }
}

// ---------- KPI, delta, pill ----------

@Composable
fun DeltaBadge(d: Double?) {
    if (d == null) return
    val (c, ic) = when { d > 0 -> MaterialTheme.colorScheme.primary to Icons.AutoMirrored.Outlined.TrendingUp; d < 0 -> Danger to Icons.AutoMirrored.Outlined.TrendingDown; else -> MaterialTheme.colorScheme.onSurfaceVariant to null }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (ic != null) Icon(ic, null, tint = c, modifier = Modifier.size(14.dp))
        Text(Fmt.pct(d), color = c, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun KpiCard(label: String, value: String, unit: String = "", modifier: Modifier = Modifier, delta: Double? = null, foot: String? = null, src: String? = null, spark: List<Long> = emptyList()) {
    AppCard(modifier, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!src.isNullOrBlank()) Text(src, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .7f))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
            if (unit.isNotBlank()) { Spacer(Modifier.width(4.dp)); Text(unit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp)) }
        }
        if (delta != null || !foot.isNullOrBlank() || spark.size > 1) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeltaBadge(delta)
                if (!foot.isNullOrBlank()) Text(foot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (spark.size > 1) Sparkline(spark, Modifier.width(72.dp).height(24.dp))
            }
        }
    }
}

@Composable
fun KpiMoney(label: String, value: Long?, modifier: Modifier = Modifier, delta: Double? = null, foot: String? = null, src: String? = null, spark: List<Long> = emptyList()) {
    val (v, u) = Fmt.moneyShort(value); KpiCard(label, v, u, modifier, delta, foot, src, spark)
}

fun levelTone(l: String?): Tone = when (l) { "crit", "kritik", "yuqori", "down" -> Tone.Danger; "warn", "o‘rta" -> Tone.Warning; "ok", "past" -> Tone.Accent; else -> Tone.Info }

@Composable
fun LevelIcon(level: String?) {
    val t = levelTone(level)
    val (ic, c) = when (t) { Tone.Danger -> Icons.Outlined.ErrorOutline to Danger; Tone.Warning -> Icons.Outlined.WarningAmber to Warning; else -> Icons.Outlined.Info to Info }
    Icon(ic, null, tint = c, modifier = Modifier.size(20.dp))
}

/** Ro'yxat elementi: ikonka/avatar · sarlavha + izoh · o'ng meta */
@Composable
fun ListRow(title: String, sub: String? = null, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null, chevron: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(12.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) { Spacer(Modifier.width(8.dp)); trailing() }
        if (chevron) Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoBar(text: String, sub: String? = null, tone: Tone = Tone.Info) {
    val (bg, fg) = when (tone) { Tone.Danger -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer; Tone.Warning -> Color(0x2EE2A336) to Warning; Tone.Accent -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer; else -> Color(0x1F4F8CFF) to Info }
    Column(Modifier.fillMaxWidth().background(bg, MaterialTheme.shapes.medium).padding(12.dp)) {
        Text(text, color = fg, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        if (!sub.isNullOrBlank()) Text(sub, color = fg.copy(alpha = .85f), style = MaterialTheme.typography.bodySmall)
    }
}

/** Segment / filtr chiplari: [(id, label)] */
@Composable
fun Seg(options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) -> FilterChip(selected = value == id, onClick = { onChange(id) }, label = { Text(label) }) }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, placeholder = { Text(placeholder) }, singleLine = true, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
}

@Composable
fun rememberString(initial: String = ""): MutableState<String> = rememberSaveable { mutableStateOf(initial) }

fun LazyListScope.sectionTitle(text: String, key: String = "st-$text") { item(key) { Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) } }

@Composable
fun Sparkline(values: List<Long>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    androidx.compose.foundation.Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val mn = values.min(); val mx = values.max(); val rg = (mx - mn).toFloat().takeIf { it > 0 } ?: 1f
        val pts = values.mapIndexed { i, v -> androidx.compose.ui.geometry.Offset(i * size.width / (values.size - 1), size.height - 2 - (v - mn) / rg * (size.height - 4)) }
        for (i in 1 until pts.size) drawLine(color, pts[i - 1], pts[i], strokeWidth = 3f)
    }
}

/** Gorizontal ustunlar: [(label, value)] */
@Composable
fun HBars(items: List<Pair<String, Long>>, fmt: (Long) -> String = { Fmt.num(it) }) {
    val mx = (items.maxOfOrNull { kotlin.math.abs(it.second) } ?: 1L).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (l, v) ->
            Column {
                Row { Text(l, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); Text(fmt(v), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) }
                Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                    Box(Modifier.fillMaxWidth((kotlin.math.abs(v).toFloat() / mx).coerceIn(0.02f, 1f)).height(8.dp).background(if (v < 0) Danger else MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                }
            }
        }
        if (items.isEmpty()) Text("Ma’lumot yo‘q", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Ustunli diagramma (30 kun) */
@Composable
fun Bars(values: List<Long>, modifier: Modifier = Modifier.fillMaxWidth().height(120.dp)) {
    val accent = MaterialTheme.colorScheme.primary; val base = MaterialTheme.colorScheme.outlineVariant
    androidx.compose.foundation.Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val mx = (values.max().toFloat()).coerceAtLeast(1f); val bw = size.width / values.size
        values.forEachIndexed { i, v -> val h = (v / mx * (size.height - 4)).coerceAtLeast(2f); drawRoundRect(if (i == values.lastIndex) accent else base, topLeft = androidx.compose.ui.geometry.Offset(i * bw + 1.5f, size.height - h), size = androidx.compose.ui.geometry.Size(bw - 3f, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)) }
    }
}

/** Progress chizig'i 0..100 */
@Composable
fun BarLine(pct: Int, modifier: Modifier = Modifier.fillMaxWidth(), tone: Tone = if (pct >= 80) Tone.Accent else if (pct >= 60) Tone.Warning else Tone.Danger) {
    val c = when (tone) { Tone.Danger -> Danger; Tone.Warning -> Warning; else -> MaterialTheme.colorScheme.primary }
    Box(modifier.height(6.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))) { Box(Modifier.fillMaxWidth((pct / 100f).coerceIn(0f, 1f)).height(6.dp).background(c, RoundedCornerShape(3.dp))) }
}
