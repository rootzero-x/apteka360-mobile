package uz.apteka360.app.data

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import uz.apteka360.app.BuildConfig
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
    isLenient = true
}

/**
 * Apteka 360 API mijozi (docs/API-MOBILE-DESKTOP.md):
 *  - X-Client: mobile/<versiya>, X-Client-Device — har so'rovda
 *  - Authorization: Bearer <accessToken>
 *  - 401 → bitta refresh (navbat bilan), yana 401 → sessiya tugadi
 *  - Buyruqlarga Idempotency-Key
 */
class ApiClient(
    private val tokens: TokenStore,
    private val onSessionExpired: () -> Unit,
    val baseUrl: String = BuildConfig.API_BASE_URL,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val refreshMutex = Mutex()

    val clientHeader = "mobile/${BuildConfig.VERSION_NAME.removeSuffix("-debug")}"
    // HTTP sarlavhasi faqat ASCII bo'lishi kerak (OkHttp tekshiradi)
    val deviceHeader: String = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} - Apteka 360"
        .replace(Regex("[^\\x20-\\x7E]"), "?")

    // ---------- umumiy ----------

    suspend fun get(path: String, auth: Boolean = true): String = call("GET", path, null, auth)

    suspend fun post(path: String, body: String?, auth: Boolean = true, idempotent: Boolean = false): String =
        call("POST", path, body ?: "{}", auth, idempotent)

    /** PUT / DELETE / POST — desktop `A.api(method, path, body)` muqobili. */
    suspend fun request(method: String, path: String, body: JsonObject? = null): String =
        call(method, path, if (method == "GET") null else (body?.toString() ?: "{}"), auth = true, idempotent = method == "POST")

    suspend inline fun <reified T> requestAs(method: String, path: String, body: JsonObject? = null): T = json.decodeFromString(request(method, path, body))

    suspend fun getJson(path: String): JsonElement = json.parseToJsonElement(get(path))
    suspend fun postJson(path: String, body: JsonObject): JsonElement = json.parseToJsonElement(post(path, body.toString(), true, true))

    suspend inline fun <reified T> getAs(path: String): T = json.decodeFromString(get(path))

    suspend inline fun <reified T> postAs(path: String, body: JsonObject, idempotent: Boolean = false): T =
        json.decodeFromString(post(path, body.toString(), true, idempotent))

    suspend fun command(name: String, body: JsonObject): String =
        post("/v1/commands/$name", body.toString(), auth = true, idempotent = true)

    suspend inline fun <reified T> commandAs(name: String, body: JsonObject): T = json.decodeFromString(command(name, body))

    private suspend fun call(method: String, path: String, body: String?, auth: Boolean, idempotent: Boolean = false): String {
        val idem = if (idempotent) UUID.randomUUID().toString() else null
        val first = execute(method, path, body, auth, idem)
        if (first.code != 401 || !auth) return first.handle()
        // bitta refresh, so'ng qayta urinish
        val refreshed = refreshMutex.withLock {
            val before = tokens.accessToken
            // boshqa so'rov allaqachon yangilagan bo'lishi mumkin
            if (before != null && before != first.usedToken) true else tryRefresh()
        }
        if (!refreshed) {
            tokens.clear(); onSessionExpired()
            throw ApiException(401, "unauthorized", "Sessiya tugadi. Qaytadan kiring.")
        }
        val second = execute(method, path, body, auth, idem)
        if (second.code == 401) { tokens.clear(); onSessionExpired() }
        return second.handle()
    }

    private class Resp(val code: Int, val body: String, val usedToken: String?) {
        fun handle(): String {
            if (code in 200..299) return body
            val err = try { json.decodeFromString<ApiErrorBody>(body) } catch (_: Exception) {
                ApiErrorBody(error = if (code >= 500) "Server vaqtincha ishlamayapti ($code)" else "Xatolik ($code)", code = "http_$code")
            }
            throw ApiException(code, err.code, err.error, err.details, err.requestId)
        }
    }

    private suspend fun execute(method: String, path: String, body: String?, auth: Boolean, idem: String?): Resp =
        withContext(Dispatchers.IO) {
            val token = if (auth) tokens.accessToken else null
            val b = Request.Builder().url(baseUrl + path)
                .header("Accept", "application/json")
                .header("X-Client", clientHeader)
                .header("X-Client-Device", deviceHeader)
            if (token != null) b.header("Authorization", "Bearer $token")
            if (idem != null) b.header("Idempotency-Key", idem)
            when (method) {
                "POST" -> b.post((body ?: "{}").toRequestBody(jsonType))
                "PUT" -> b.put((body ?: "{}").toRequestBody(jsonType))
                "DELETE" -> b.delete((body ?: "{}").toRequestBody(jsonType))
                "PATCH" -> b.patch((body ?: "{}").toRequestBody(jsonType))
            }
            try {
                http.newCall(b.build()).execute().use { r -> Resp(r.code, r.body?.string() ?: "", token) }
            } catch (e: IOException) {
                throw NetworkException(e)
            }
        }

    /**
     * Edge media-proksiga to'g'ridan-to'g'ri so'rov (talkUrl bilan imzolangan, Authorization kerak emas).
     * @return HTTP kodi va tana.
     */
    suspend fun rawPost(url: String, body: ByteArray, contentType: String = "application/octet-stream"): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url)
                .header("X-Client", clientHeader)
                .post(body.toRequestBody(contentType.toMediaType()))
                .build()
            try {
                http.newCall(req).execute().use { r -> r.code to (r.body?.string() ?: "") }
            } catch (e: IOException) {
                throw NetworkException(e)
            }
        }

    /** @return true — yangilandi */
    private suspend fun tryRefresh(): Boolean {
        val rt = tokens.refreshToken ?: return false
        val r = execute("POST", "/v1/auth/refresh", """{"refreshToken":${json.encodeToString(kotlinx.serialization.serializer<String>(), rt)}}""", auth = false, idem = null)
        if (r.code !in 200..299) return false
        val s = json.decodeFromString<Session>(r.body)
        applySession(s)
        return true
    }

    fun applySession(s: Session) {
        tokens.accessToken = s.accessToken
        s.refreshToken?.let { tokens.refreshToken = it }
        tokens.userJson = json.encodeToString(User.serializer(), s.user)
    }

    /** Ilova ochilganda: saqlangan refresh bilan access olish. */
    suspend fun restore(): Boolean = refreshMutex.withLock { tryRefresh() }
}

fun String.toJsonObject(): JsonObject = json.parseToJsonElement(this).jsonObject
