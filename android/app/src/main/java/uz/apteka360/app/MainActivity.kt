package uz.apteka360.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import kotlinx.coroutines.launch
import uz.apteka360.app.ui.auth.AuthFlow
import uz.apteka360.app.ui.auth.AuthViewModel
import uz.apteka360.app.ui.main.MainScaffold
import uz.apteka360.app.ui.theme.Apteka360Theme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val c = container
        splash.setKeepOnScreenCondition { c.auth.value is AuthState.Loading }
        handleIntent(intent)
        setContent {
            Apteka360Theme {
                val auth by c.auth.collectAsState()
                LaunchedEffect(Unit) { if (c.auth.value is AuthState.Loading) c.restoreSession() }
                val scope = rememberCoroutineScope()
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (val a = auth) {
                        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        AuthState.SignedOut -> {
                            val vm: AuthViewModel = viewModel(factory = viewModelFactory { initializer { AuthViewModel(c) } })
                            AuthFlow(vm)
                        }
                        is AuthState.SignedIn -> MainScaffold(c, a.me, onSignOut = { scope.launch { c.signOut() } })
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); handleIntent(intent) }

    /** apteka360://<bo‘lim>/<param> yoki push'dagi "route"/"link" → ilova ichidagi ekran. */
    private fun handleIntent(intent: android.content.Intent?) {
        intent ?: return
        val uri = intent.data
        val route = when {
            uri != null && uri.scheme == "apteka360" -> listOfNotNull(uri.host, uri.pathSegments.firstOrNull()).joinToString("/")
            intent.getStringExtra("route") != null -> intent.getStringExtra("route")
            intent.getStringExtra("link") != null -> intent.getStringExtra("link")
            else -> null
        }
        if (!route.isNullOrBlank()) container.pendingRoute.value = route
    }
}
