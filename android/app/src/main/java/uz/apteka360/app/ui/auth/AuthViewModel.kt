package uz.apteka360.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.ApiException
import uz.apteka360.app.data.NewDevice
import uz.apteka360.app.ui.components.userMessage

sealed interface AuthStep {
    data object Login : AuthStep
    data class Otp(val challengeId: String, val login: String, val ttl: Int, val channels: List<String>, val forReset: Boolean = false) : AuthStep
    data class NewDeviceConfirm(val device: NewDevice) : AuthStep
    data object Forgot : AuthStep
    data class ResetPassword(val challengeId: String, val code: String) : AuthStep
}

data class AuthUi(
    val step: AuthStep = AuthStep.Login,
    val busy: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val resendIn: Int = 0,
    val attemptsLeft: Int? = null,
)

class AuthViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(AuthUi())
    val ui: StateFlow<AuthUi> = _ui
    private var timer: Job? = null

    private fun run(block: suspend () -> Unit) = viewModelScope.launch {
        _ui.update { it.copy(busy = true, error = null, info = null) }
        try { block() } catch (e: Exception) {
            val attempts = (e as? ApiException)?.details?.let { d ->
                runCatching { (d as kotlinx.serialization.json.JsonObject)["attemptsLeft"]?.toString()?.toInt() }.getOrNull()
            }
            _ui.update { it.copy(error = e.userMessage(), attemptsLeft = attempts) }
        } finally { _ui.update { it.copy(busy = false) } }
    }

    private fun startResendTimer(sec: Int) {
        timer?.cancel()
        _ui.update { it.copy(resendIn = sec) }
        timer = viewModelScope.launch { while (_ui.value.resendIn > 0) { delay(1000); _ui.update { it.copy(resendIn = it.resendIn - 1) } } }
    }

    fun login(login: String, password: String) = run {
        if (login.isBlank() || password.isBlank()) throw ApiException(400, "validation", "Login va parolni kiriting")
        val ch = c.repo.login(login.trim(), password)
        _ui.update { it.copy(step = AuthStep.Otp(ch.challengeId, login.trim(), ch.otpTtlSec, ch.channels)) }
        startResendTimer(ch.resendInSec)
    }

    fun otp(code: String) = run {
        val s = _ui.value.step as? AuthStep.Otp ?: return@run
        if (code.length < 4) throw ApiException(400, "validation", "Kodni kiriting")
        if (s.forReset) { _ui.update { it.copy(step = AuthStep.ResetPassword(s.challengeId, code)) }; return@run }
        val r = c.repo.otp(s.challengeId, code)
        when {
            r.newDevice != null -> _ui.update { it.copy(step = AuthStep.NewDeviceConfirm(r.newDevice)) }
            r.accessToken != null -> c.onSignedIn()
            else -> throw ApiException(500, "unknown", "Kutilmagan javob")
        }
    }

    fun resend() = run {
        val s = _ui.value.step as? AuthStep.Otp ?: return@run
        val ch = c.repo.resendOtp(s.challengeId)
        startResendTimer(ch.resendInSec)
        _ui.update { it.copy(info = "Kod qayta yuborildi", attemptsLeft = null) }
    }

    fun device(trust: Boolean) = run {
        val s = _ui.value.step as? AuthStep.NewDeviceConfirm ?: return@run
        val session = c.repo.deviceDecision(s.device.confirmId, trust)
        if (session != null) c.onSignedIn()
        else _ui.update { it.copy(step = AuthStep.Login, info = "Hisob bloklandi va barcha sessiyalar yopildi. Administratorga murojaat qiling.") }
    }

    fun forgot(login: String) = run {
        if (login.isBlank()) throw ApiException(400, "validation", "Telefon yoki e-pochtani kiriting")
        val ch = c.repo.forgotPassword(login.trim())
        _ui.update { it.copy(step = AuthStep.Otp(ch.challengeId, login.trim(), ch.otpTtlSec, emptyList(), forReset = true)) }
        startResendTimer(ch.resendInSec)
    }

    fun reset(password: String, confirm: String) = run {
        val s = _ui.value.step as? AuthStep.ResetPassword ?: return@run
        if (password.length < 8) throw ApiException(400, "validation", "Parol kamida 8 belgi bo‘lsin")
        if (password != confirm) throw ApiException(400, "validation", "Parollar mos emas")
        c.repo.resetPassword(s.challengeId, s.code, password)
        _ui.update { it.copy(step = AuthStep.Login, info = "Parol yangilandi. Yangi parol bilan kiring.") }
    }

    fun goTo(step: AuthStep) { timer?.cancel(); _ui.update { it.copy(step = step, error = null, info = null, attemptsLeft = null) } }
}
