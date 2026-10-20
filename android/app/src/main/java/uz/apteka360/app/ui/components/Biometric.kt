package uz.apteka360.app.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** 10 mln so‘mdan katta summalar uchun biometrik tasdiq majburiy (Telegram Mini App qoidasi bilan bir xil). */
const val BIOMETRIC_THRESHOLD = 10_000_000L

object Biometric {
    private const val AUTH = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun available(activity: FragmentActivity): Boolean = BiometricManager.from(activity).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * @return null — tasdiqlandi (yoki qurilmada ekran qulfi yo‘q va majburiy emas); aks holda o‘zbekcha xato matni.
     * Mobil sessiya backendda `web` kanali sifatida qabul qilinadi, shuning uchun bu — telefondagi qo‘shimcha himoya (biometricToken faqat Telegram Mini App uchun).
     */
    suspend fun confirm(activity: FragmentActivity?, title: String, subtitle: String, required: Boolean): String? {
        if (activity == null) return if (required) "Biometrik tasdiq mavjud emas" else null
        if (!available(activity)) return if (required) "Bu amal uchun telefonda ekran qulfi (barmoq izi, yuz yoki PIN) yoqilgan bo‘lishi kerak." else null
        return suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { if (cont.isActive) cont.resume(null) }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { if (cont.isActive) cont.resume(if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) "Biometrik tasdiq bekor qilindi" else "Biometrik tasdiq: $errString") }
            })
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setSubtitle(subtitle).setAllowedAuthenticators(AUTH).build())
        }
    }
}

fun android.content.Context.findFragmentActivity(): FragmentActivity? {
    var c: android.content.Context? = this
    while (c is android.content.ContextWrapper) { if (c is FragmentActivity) return c; c = c.baseContext }
    return null
}
