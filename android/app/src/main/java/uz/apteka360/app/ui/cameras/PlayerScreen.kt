package uz.apteka360.app.ui.cameras

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.Repository
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.userMessage

/**
 * Jonli ko'rish: cameras.streamUrl → hlsUrl (5 daqiqa amal qiladi) → Media3 ExoPlayer (HLS).
 * Havola muddati tugashidan oldin qayta so'raladi.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(repo: Repository, cameraId: String, cameraName: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var hlsUrl by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf("Ulanmoqda…") }
    var reloadKey by remember { mutableStateOf(0) }

    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    state = when (playbackState) {
                        Player.STATE_BUFFERING -> "Yuklanmoqda…"; Player.STATE_READY -> "Jonli"; Player.STATE_ENDED -> "Oqim tugadi"; else -> "Ulanmoqda…"
                    }
                }
                override fun onPlayerError(e: PlaybackException) { error = "Video ijro xatosi: ${e.errorCodeName}" }
            })
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(reloadKey) {
        error = null
        while (true) {
            try {
                val s = repo.streamUrl(cameraId)
                val url = s.hlsUrl ?: throw ApiException(500, "no_hls", "Bu kamera uchun HLS oqimi yo‘q")
                hlsUrl = url
                val ds = DefaultHttpDataSource.Factory().setUserAgent("Apteka360Mobile/android")
                player.setMediaSource(HlsMediaSource.Factory(ds).createMediaSource(MediaItem.fromUri(url)))
                player.prepare()
                delay(4 * 60 * 1000L) // 5 daqiqalik havolani 4 daqiqada yangilaymiz
            } catch (e: Exception) {
                error = when {
                    e is ApiException && e.status == 503 -> "Filial Edge serveri oflayn — jonli ko‘rish vaqtincha mavjud emas."
                    e is ApiException && e.code == "module_disabled" -> e.message
                    e is ApiException && e.status == 403 -> "Bu kamerani ko‘rishga ruxsat yo‘q."
                    else -> e.userMessage()
                }
                break
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(cameraName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") } }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black), contentAlignment = Alignment.Center) {
                if (hlsUrl != null && error == null) AndroidView(factory = {
                    PlayerView(it).apply {
                        this.player = player
                        useController = true
                        controllerShowTimeoutMs = 2000
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                }, modifier = Modifier.fillMaxSize())
                if (error != null) Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = Color.White, textAlign = TextAlign.Center)
                    Button(onClick = { reloadKey++ }, modifier = Modifier.padding(top = 12.dp)) { Text("Qayta urinish") }
                } else if (state != "Jonli") CircularProgressIndicator(color = Color.White)
            }
            Column(Modifier.padding(16.dp)) {
                KeyValueRow("Holat", if (error != null) "Xato" else state)
                KeyValueRow("Oqim", "HLS (sub) · havola har 4 daqiqada yangilanadi")
                Text("Har bir jonli ko‘rish audit jurnaliga yoziladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
