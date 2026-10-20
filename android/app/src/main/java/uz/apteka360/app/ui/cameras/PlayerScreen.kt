package uz.apteka360.app.ui.cameras

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.launch
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.CameraAudio
import uz.apteka360.app.data.Repository
import uz.apteka360.app.data.TalkSession
import uz.apteka360.app.data.TalkState
import uz.apteka360.app.data.TalkUrl
import uz.apteka360.app.ui.components.KeyValueRow
import uz.apteka360.app.ui.components.userMessage

/**
 * Jonli ko'rish: cameras.streamUrl → hlsUrl (5 daqiqa) → Media3 ExoPlayer (HLS, AAC audio trek).
 *  - «Ovoz»: kamera mikrofoni bo'lsa (audio.in) — pleer ovozini yoqish/o'chirish (boshida o'chiq).
 *  - «Gapirish»: karnay bo'lsa (audio.out) — bosib turib gapirish: cameras.talk → Edge'ga PCM 8 kHz bo'laklar.
 */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(repo: Repository, cameraId: String, cameraName: String, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var hlsUrl by remember { mutableStateOf<String?>(null) }
    var audio by remember { mutableStateOf(CameraAudio()) }
    var error by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf("Ulanmoqda…") }
    var reloadKey by remember { mutableStateOf(0) }
    var soundOn by remember { mutableStateOf(false) }
    var full by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val activity = ctx as? android.app.Activity
    androidx.compose.runtime.LaunchedEffect(full) {
        activity ?: return@LaunchedEffect
        activity.requestedOrientation = if (full) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val ctl = androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (full) { ctl.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars()); ctl.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } else ctl.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
    }
    DisposableEffect(Unit) { onDispose { activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED; activity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView).show(androidx.core.view.WindowInsetsCompat.Type.systemBars()) } } }
    androidx.activity.compose.BackHandler(enabled = full) { full = false }

    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            playWhenReady = true
            volume = 0f // avtoijro: ovoz boshida o'chiq, «Ovoz» tugmasi yoqadi
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
    val talk = remember { TalkSession(repo.api, scope) }
    val talkState by talk.state.collectAsState()
    DisposableEffect(Unit) { onDispose { talk.stop(); player.release() } }

    LaunchedEffect(reloadKey) {
        error = null
        while (true) {
            try {
                val s = repo.streamUrl(cameraId)
                val url = s.hlsUrl ?: throw ApiException(500, "no_hls", "Bu kamera uchun HLS oqimi yo‘q")
                hlsUrl = url; audio = s.audio
                val ds = DefaultHttpDataSource.Factory().setUserAgent("Apteka360Mobile/android")
                player.setMediaSource(HlsMediaSource.Factory(ds).createMediaSource(MediaItem.fromUri(url)))
                player.prepare()
                delay(4 * 60 * 1000L) // 5 daqiqalik havolani 4 daqiqada yangilaymiz
            } catch (e: Exception) {
                error = when {
                    e is ApiException && e.code == "module_disabled" -> e.message
                    e is ApiException && e.status == 503 -> "Filial Edge serveri oflayn — jonli ko‘rish vaqtincha mavjud emas."
                    e is ApiException && e.status == 403 -> "Bu kamerani ko‘rishga ruxsat yo‘q."
                    else -> e.userMessage()
                }
                break
            }
        }
    }

    // ---- Gapirish: ruxsat + talk havolasi (5 daqiqa keshlanadi) ----
    var talkUrl by remember { mutableStateOf<TalkUrl?>(null) }
    var talkUrlAt by remember { mutableStateOf(0L) }
    var talkError by remember { mutableStateOf<String?>(null) }
    var pressed by remember { mutableStateOf(false) }
    val micGranted = { ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED }
    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        talkError = if (ok) null else "Mikrofon ruxsati berilmadi. Sozlamalar → Ilovalar → Apteka 360 → Ruxsatlar."
    }
    suspend fun beginTalk() {
        talkError = null
        val fresh = talkUrl?.takeIf { System.currentTimeMillis() - talkUrlAt < 4 * 60 * 1000L } ?: try {
            repo.talkUrl(cameraId).also { talkUrl = it; talkUrlAt = System.currentTimeMillis() }
        } catch (e: Exception) {
            talkError = when {
                e is ApiException && e.details?.toString()?.contains("no_audio_out") == true -> "Kamerada karnay yo‘q yoki Edge uni topmadi."
                e is ApiException && e.status == 503 -> "Filial Edge serveri oflayn — gapirish vaqtincha mavjud emas."
                e is ApiException && e.status == 403 -> "Gapirishga ruxsat yo‘q (faqat mudir va egasi)."
                else -> e.userMessage()
            }
            return
        }
        if (pressed) talk.start(fresh.talkUrl, fresh.maxSec)
    }
    LaunchedEffect(talkState) { if (talkState is TalkState.Error) { talkError = (talkState as TalkState.Error).message } }

    Scaffold(
        topBar = { if (!full) TopAppBar(title = { Text(cameraName) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") } }, actions = { IconButton(onClick = { full = true }) { Icon(Icons.Default.Fullscreen, "To‘liq ekran") } }) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Box((if (full) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)).background(Color.Black), contentAlignment = Alignment.Center) {
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

                // Ovoz tugmasi (kamera mikrofoni bo'lsa)
                if (audio.input && error == null) IconButton(
                    onClick = { soundOn = !soundOn; player.volume = if (soundOn) 1f else 0f },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = .45f), RoundedCornerShape(50)),
                ) { Icon(if (soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff, if (soundOn) "Ovozni o‘chirish" else "Ovozni yoqish", tint = Color.White) }
                if (full) IconButton(onClick = { full = false }, modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(Color.Black.copy(alpha = .45f), RoundedCornerShape(50))) { Icon(Icons.Default.FullscreenExit, "Chiqish", tint = Color.White) }
            }

            if (!full) Column(Modifier.padding(16.dp)) {
                // Gapirish (karnay bo'lsa)
                if (audio.output) {
                    val talking = talkState is TalkState.Talking
                    val connecting = talkState is TalkState.Connecting
                    Box(
                        Modifier.fillMaxWidth().height(64.dp)
                            .background(
                                when { talking -> MaterialTheme.colorScheme.error; connecting -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.primary },
                                MaterialTheme.shapes.large,
                            )
                            .pointerInput(cameraId) {
                                detectTapGestures(onPress = {
                                    if (!micGranted()) { askMic.launch(Manifest.permission.RECORD_AUDIO); return@detectTapGestures }
                                    pressed = true
                                    scope.launch { beginTalk() }
                                    try { awaitRelease() } finally { pressed = false; talk.stop() }
                                })
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, null, tint = if (connecting) MaterialTheme.colorScheme.onSecondaryContainer else Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                when (val t = talkState) {
                                    is TalkState.Talking -> "Gapiryapsiz… ${t.seconds}/${t.maxSec} s"
                                    TalkState.Connecting -> "Ulanmoqda…"
                                    else -> "Gapirish — bosib turing"
                                },
                                color = if (connecting) MaterialTheme.colorScheme.onSecondaryContainer else Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    (talkState as? TalkState.Talking)?.let { t ->
                        LinearProgressIndicator(progress = { t.seconds.toFloat() / t.maxSec }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    }
                    talkError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
                    Spacer(Modifier.height(12.dp))
                }

                KeyValueRow("Holat", if (error != null) "Xato" else state)
                KeyValueRow("Oqim", "HLS (sub) · havola har 4 daqiqada yangilanadi")
                KeyValueRow("Kamera ovozi", if (audio.input) (if (soundOn) "Yoqilgan" else "O‘chiq (tugma bilan yoqing)") else "Mikrofon yo‘q")
                KeyValueRow("Karnay", if (audio.output) "Bor — bosib turib gapiring" else "Yo‘q")
                Text("Har bir jonli ko‘rish va gapirish audit jurnaliga yoziladi.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}


/** Video devor plitkasi uchun ovozsiz, kontrolsiz HLS pleer. */
@OptIn(UnstableApi::class)
@Composable
fun MiniPlayer(repo: Repository, cameraId: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var err by remember { mutableStateOf<String?>(null) }
    val player = remember { ExoPlayer.Builder(ctx).build().apply { playWhenReady = true; volume = 0f } }
    DisposableEffect(Unit) { onDispose { player.release() } }
    LaunchedEffect(cameraId) {
        while (true) {
            try { val s = repo.streamUrl(cameraId); val url = s.hlsUrl ?: break
                player.setMediaSource(HlsMediaSource.Factory(DefaultHttpDataSource.Factory()).createMediaSource(MediaItem.fromUri(url))); player.prepare(); delay(4 * 60 * 1000L)
            } catch (e: Exception) { err = if (e is ApiException && e.status == 503) "Edge oflayn" else "Video ochilmadi"; break }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM } }, modifier = Modifier.fillMaxSize())
        err?.let { Text(it, color = Color(0xFF9A9A9A), style = MaterialTheme.typography.labelMedium) }
    }
}
