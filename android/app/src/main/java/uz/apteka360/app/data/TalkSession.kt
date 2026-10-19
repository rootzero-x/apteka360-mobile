package uz.apteka360.app.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Push-to-talk holati (ekranda indikator uchun). */
sealed interface TalkState {
    data object Idle : TalkState
    data object Connecting : TalkState
    data class Talking(val seconds: Int, val maxSec: Int) : TalkState
    data class Error(val message: String) : TalkState
}

/**
 * Edge talk shartnomasi (openapi: cameras.talk):
 *   POST {talkUrl}&op=start → 200 {ok,maxSec,codec} | 409 band | 503 karnay yo'q
 *   POST {talkUrl}  tana: PCM 16-bit LE 8 kHz mono, ~200 ms bo'laklar (application/octet-stream)
 *   POST {talkUrl}&op=stop
 * Sof mantiq (URL, bo'lak hajmi, xato matnlari) unit-testlanadi; AudioRecord faqat start() ichida.
 */
object TalkProtocol {
    const val SAMPLE_RATE = 8000
    const val CHUNK_MS = 200
    /** 8000 Hz × 2 bayt × 0.2 s */
    const val CHUNK_BYTES = SAMPLE_RATE * 2 * CHUNK_MS / 1000

    fun opUrl(talkUrl: String, op: String): String = talkUrl + (if (talkUrl.contains('?')) "&" else "?") + "op=" + op

    /** Edge javobidagi `error` matni yoki HTTP kod bo'yicha o'zbekcha xabar. */
    fun errorMessage(code: Int, body: String): String {
        val fromBody = runCatching { json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
        return when (code) {
            401 -> "Gapirish havolasi muddati o‘tgan. Qayta urinib ko‘ring."
            404 -> "Seans topilmadi. Qayta bosib gapiring."
            409 -> fromBody ?: "Bu kameraga hozir boshqa foydalanuvchi gapiryapti"
            410 -> "Seans tugadi (${fromBody ?: "vaqt chegarasi"})."
            413 -> "Audio bo‘lagi juda katta"
            503 -> fromBody ?: "Edge yoki kamera karnayi mavjud emas"
            else -> fromBody ?: "Gapirishda xato ($code)"
        }
    }

    fun maxSecFrom(body: String, fallback: Int): Int =
        runCatching { json.parseToJsonElement(body).jsonObject["maxSec"]?.jsonPrimitive?.content?.toInt() }.getOrNull() ?: fallback
}

class TalkSession(private val api: ApiClient, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow<TalkState>(TalkState.Idle)
    val state: StateFlow<TalkState> = _state
    private var job: Job? = null

    val isActive get() = job?.isActive == true

    /** Tugma bosilganda: op=start, so'ng mikrofon bo'laklarini yuborish. RECORD_AUDIO ruxsati chaqiruvchi tomonidan tekshirilgan. */
    @SuppressLint("MissingPermission")
    fun start(talkUrl: String, maxSecHint: Int) {
        if (isActive) return
        job = scope.launch(Dispatchers.IO) {
            _state.value = TalkState.Connecting
            val (code, body) = try { api.rawPost(TalkProtocol.opUrl(talkUrl, "start"), ByteArray(0)) } catch (e: NetworkException) {
                _state.value = TalkState.Error("Edge serveriga ulanib bo‘lmadi"); return@launch
            }
            if (code != 200) { _state.value = TalkState.Error(TalkProtocol.errorMessage(code, body)); return@launch }
            val maxSec = TalkProtocol.maxSecFrom(body, maxSecHint)

            val minBuf = AudioRecord.getMinBufferSize(TalkProtocol.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val recorder = try {
                AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, TalkProtocol.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuf, TalkProtocol.CHUNK_BYTES * 4))
            } catch (e: Exception) { null }
            if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
                recorder?.release()
                stopRemote(talkUrl)
                _state.value = TalkState.Error("Mikrofon ochilmadi"); return@launch
            }
            val startedAt = System.currentTimeMillis()
            var failure: String? = null
            try {
                recorder.startRecording()
                val buf = ByteArray(TalkProtocol.CHUNK_BYTES)
                while (isActive) {
                    val sec = ((System.currentTimeMillis() - startedAt) / 1000).toInt()
                    if (sec >= maxSec) break
                    _state.value = TalkState.Talking(sec, maxSec)
                    var filled = 0
                    while (filled < buf.size && isActive) {
                        val n = recorder.read(buf, filled, buf.size - filled)
                        if (n <= 0) break
                        filled += n
                    }
                    if (filled <= 0) continue
                    val (c, b) = api.rawPost(talkUrl, if (filled == buf.size) buf else buf.copyOf(filled))
                    if (c != 200) { failure = TalkProtocol.errorMessage(c, b); break }
                }
            } catch (e: NetworkException) {
                failure = "Edge bilan aloqa uzildi"
            } catch (e: kotlinx.coroutines.CancellationException) {
                // foydalanuvchi tugmani qo'yib yubordi
            } finally {
                runCatching { recorder.stop() }; recorder.release()
                withContext(kotlinx.coroutines.NonCancellable) { stopRemote(talkUrl) }
                _state.value = failure?.let { TalkState.Error(it) } ?: TalkState.Idle
            }
        }
    }

    /** Tugma qo'yib yuborilganda. */
    fun stop() { job?.cancel(); job = null }

    private suspend fun stopRemote(talkUrl: String) {
        runCatching { api.rawPost(TalkProtocol.opUrl(talkUrl, "stop"), ByteArray(0)) }
    }
}
