package uz.apteka360.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Refresh token va deviceId — Android Keystore bilan shifrlangan SharedPreferences'da.
 * Access token faqat xotirada (API hujjati 2.4).
 */
class TokenStore(context: Context) {
    private val prefs: SharedPreferences = try {
        val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "apteka360.secure", key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Keystore buzilgan bo'lsa (juda kam) — oddiy prefs, foydalanuvchi qayta kiradi
        context.getSharedPreferences("apteka360.fallback", Context.MODE_PRIVATE)
    }

    @Volatile var accessToken: String? = null

    var refreshToken: String?
        get() = prefs.getString("refreshToken", null)
        set(v) { prefs.edit().apply { if (v == null) remove("refreshToken") else putString("refreshToken", v) }.apply() }

    var userJson: String?
        get() = prefs.getString("user", null)
        set(v) { prefs.edit().apply { if (v == null) remove("user") else putString("user", v) }.apply() }

    /** Ilova o'rnatilganda bir marta yaratiladigan barqaror ID (ishonchli qurilma). */
    val deviceId: String
        get() = prefs.getString("deviceId", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("deviceId", it).apply()
        }

    val hasSession get() = refreshToken != null

    fun clear() {
        accessToken = null
        prefs.edit().remove("refreshToken").remove("user").apply()
    }
}
