package uz.apteka360.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/** Snackbar'ni istalgan ekrandan ko'rsatish uchun. */
val LocalSnack = compositionLocalOf<SnackbarHostState> { SnackbarHostState() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    sub: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: Pair<ImageVector, () -> Unit>? = null,
    content: @Composable () -> Unit,
) {
    val snack = remember { SnackbarHostState() }
    androidx.compose.runtime.CompositionLocalProvider(LocalSnack provides snack) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Column { Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1); if (!sub.isNullOrBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1) } },
                    navigationIcon = { if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") } },
                    actions = actions,
                )
            },
            snackbarHost = { SnackbarHost(snack) },
            floatingActionButton = { fab?.let { (ic, fn) -> FloatingActionButton(onClick = fn) { Icon(ic, null) } } },
            containerColor = MaterialTheme.colorScheme.background,
        ) { pad -> Box(Modifier.padding(pad).fillMaxSize()) { content() } }
    }
}
