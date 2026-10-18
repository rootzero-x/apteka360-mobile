package uz.apteka360.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.Employee
import uz.apteka360.app.data.Me
import uz.apteka360.app.data.MobileState
import uz.apteka360.app.ui.components.userMessage

data class HomeUi(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: Throwable? = null,
    val state: MobileState? = null,
    val colleagues: List<Employee> = emptyList(),
    val busy: Boolean = false,
    val toast: String? = null,
    val auditId: String? = null,
)

class HomeViewModel(private val c: AppContainer, val me: Me) : ViewModel() {
    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui

    init { load() }

    fun load(refresh: Boolean = false) = viewModelScope.launch {
        _ui.update { it.copy(loading = !refresh && it.state == null, refreshing = refresh, error = null) }
        try {
            val st = c.repo.mobile()
            _ui.update { it.copy(state = st, loading = false, refreshing = false) }
            if (me.isEmployee && _ui.value.colleagues.isEmpty()) runCatching { c.repo.employees() }.onSuccess { list ->
                _ui.update { it.copy(colleagues = list.filter { e -> e.id != st.me && e.status != "dismissed" }) }
            }
        } catch (e: Exception) {
            _ui.update { it.copy(loading = false, refreshing = false, error = if (it.state == null) e else null, toast = if (it.state != null) e.userMessage() else null) }
        }
    }

    /** Buyruq: muvaffaqiyatda yangilangan MobileState keladi. @return true — bajarildi */
    fun command(successText: String? = null, block: suspend () -> MobileState) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true) }
            try {
                val st = block()
                _ui.update { it.copy(state = st, busy = false, toast = successText) }
            } catch (e: Exception) {
                _ui.update { it.copy(busy = false, toast = e.userMessage()) }
            }
        }
    }

    val repo get() = c.repo
    fun consumeToast() = _ui.update { it.copy(toast = null) }
}
