package uz.apteka360.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.apteka360.app.data.ApiClient
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.Repository
import uz.apteka360.app.data.TokenStore

/** Sessiya holati: ilova ochilganda tekshiriladi, 401 da login ekraniga qaytadi. */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val me: Me) : AuthState
}

class AppContainer(val context: Context) {
    val tokens = TokenStore(context.applicationContext)
    private val _auth = MutableStateFlow<AuthState>(AuthState.Loading)
    val auth: StateFlow<AuthState> = _auth

    val api = ApiClient(tokens, onSessionExpired = { _auth.value = AuthState.SignedOut })
    val repo = Repository(api, tokens)

    suspend fun restoreSession() {
        if (!tokens.hasSession) { _auth.value = AuthState.SignedOut; return }
        try {
            if (api.restore()) _auth.value = AuthState.SignedIn(repo.me()) else { tokens.clear(); _auth.value = AuthState.SignedOut }
        } catch (e: uz.apteka360.app.data.NetworkException) {
            // oflayn: saqlangan foydalanuvchi bilan kiritamiz, so'rovlar o'z xatosini ko'rsatadi
            val u = tokens.userJson?.let { runCatching { uz.apteka360.app.data.json.decodeFromString<uz.apteka360.app.data.User>(it) }.getOrNull() }
            _auth.value = if (u != null) AuthState.SignedIn(Me(role = u.role, name = u.name, ini = u.ini, title = u.title, scope = u.scope)) else AuthState.SignedOut
        } catch (e: Exception) {
            tokens.clear(); _auth.value = AuthState.SignedOut
        }
    }

    /** Deep link / push'dan kelgan marshrut (MainScaffold o‘qiydi va tozalaydi). */
    val pendingRoute = MutableStateFlow<String?>(null)
    private val prefs = context.applicationContext.getSharedPreferences("apteka360.ui", Context.MODE_PRIVATE)
    /** Biometrik tasdiq (tasdiqlar va 10 mln+ summalar) — foydalanuvchi sozlamasi. */
    var biometricEnabled: Boolean
        get() = prefs.getBoolean("biometric", true)
        set(v) { prefs.edit().putBoolean("biometric", v).apply() }

    /** Filiallar keshi (desktop `A.branchesCached`). */
    @Volatile var branches: List<uz.apteka360.app.data.Branch> = emptyList()
    suspend fun branchesCached(force: Boolean = false): List<uz.apteka360.app.data.Branch> {
        if (branches.isEmpty() || force) runCatching { repo.branches() }.onSuccess { branches = it }
        return branches
    }
    fun branchName(id: String?): String = if (id == null) "Tarmoq" else branches.firstOrNull { it.id == id }?.label ?: id

    suspend fun onSignedIn() { _auth.value = AuthState.SignedIn(repo.me()) }
    suspend fun signOut() { repo.logout(); _auth.value = AuthState.SignedOut }
}

val Context.container: AppContainer get() = (applicationContext as App).container
