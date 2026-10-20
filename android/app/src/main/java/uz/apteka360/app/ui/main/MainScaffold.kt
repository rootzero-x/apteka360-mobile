package uz.apteka360.app.ui.main

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.Me
import uz.apteka360.app.ui.cameras.CamerasScreen
import uz.apteka360.app.ui.cameras.PlayerScreen
import uz.apteka360.app.ui.components.EmptyBox
import uz.apteka360.app.ui.home.HomeScreen
import uz.apteka360.app.ui.home.HomeViewModel
import uz.apteka360.app.ui.nav.Access
import uz.apteka360.app.ui.notifications.NotificationsScreen
import uz.apteka360.app.ui.profile.ProfileScreen
import uz.apteka360.app.ui.screens.ApprovalsScreen
import uz.apteka360.app.ui.screens.AuditScreen
import uz.apteka360.app.ui.screens.BranchDetailScreen
import uz.apteka360.app.ui.screens.BranchesScreen
import uz.apteka360.app.ui.screens.CashScreen
import uz.apteka360.app.ui.screens.DashboardScreen
import uz.apteka360.app.ui.screens.MoreScreen
import uz.apteka360.app.ui.screens.*
import uz.apteka360.app.ui.screens.ScreenScaffold
import java.net.URLDecoder
import java.net.URLEncoder

private fun enc(s: String?) = URLEncoder.encode(s ?: "", "UTF-8")

@Composable
fun MainScaffold(c: AppContainer, me: Me, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val tabs = remember(me.role) { Access.bottomTabs(me.role) }
    val backStack by nav.currentBackStackEntryAsState()
    val dest = backStack?.destination
    val route = dest?.route ?: ""
    val hideBar = route.startsWith("player") || route.startsWith("branch/") || route.startsWith("employee/")
    /** go(id, param): bo'limga o'tish (param — id / filial). */
    val go: (String, String?) -> Unit = { id, p ->
        when (id) {
            "branch" -> nav.navigate("branch/${enc(p)}")
            "employee" -> nav.navigate("employee/${enc(p)}")
            "player" -> nav.navigate("player/${enc(p)}/${enc(p)}")
            else -> nav.navigate(if (p == null) id else "$id?p=${enc(p)}") { launchSingleTop = true }
        }
    }
    val goTab: (String) -> Unit = { id -> nav.navigate(id) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true } }

    Scaffold(bottomBar = {
        if (!hideBar) NavigationBar {
            tabs.forEach { id ->
                val it = Access.byId[id] ?: return@forEach
                val selected = dest?.hierarchy?.any { d -> d.route?.substringBefore('?') == id } == true
                NavigationBarItem(selected = selected, onClick = { goTab(id) }, icon = { Icon(it.icon, it.label) }, label = { Text(it.label.substringBefore(" ("), maxLines = 1, overflow = TextOverflow.Ellipsis) })
            }
            val moreSel = route == "more" || (route.substringBefore('?') !in tabs && !route.startsWith("player") && !route.startsWith("branch/"))
            NavigationBarItem(selected = moreSel, onClick = { goTab("more") }, icon = { Icon(Icons.Default.Menu, "Yana") }, label = { Text("Yana") })
        }
    }, containerColor = MaterialTheme.colorScheme.background) { pad ->
        val start = Access.home(me.role).let { if (Access.can(it, me.role)) it else tabs.firstOrNull() ?: "profile" }
        val param = navArgument("p") { type = NavType.StringType; nullable = true; defaultValue = null }
        fun p(e: androidx.navigation.NavBackStackEntry) = e.arguments?.getString("p")?.let { URLDecoder.decode(it, "UTF-8") }?.takeIf { it.isNotBlank() }
        NavHost(nav, startDestination = start, modifier = Modifier.padding(bottom = pad.calculateBottomPadding()),
            enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(250)) { it / 12 } }, exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(200)) }, popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(250)) { it / 12 } }) {
            fun screen(id: String, content: @Composable (String?) -> Unit) = composable("$id?p={p}", listOf(param)) { e -> if (Access.can(id, me.role) || id == "more") content(p(e)) else Denied(id) }
            screen("dashboard") { DashboardScreen(c, me, go) }
            screen("mobile") { val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(c, me) } }); HomeScreen(vm) }
            screen("branches") { BranchesScreen(c, go) }
            composable("branch/{p}", listOf(navArgument("p") { type = NavType.StringType })) { e -> BranchDetailScreen(c, me, p(e) ?: "", go, onBack = { nav.popBackStack() }) }
            screen("approvals") { ApprovalsScreen(c, me, it) }
            screen("notifications") { NotificationsScreen(c.repo) }
            screen("audit") { AuditScreen(c) }
            screen("cash") { CashScreen(c, me, it) }
            screen("cameras") { CamerasAdminScreen(c, me, it) { cam -> nav.navigate("player/${enc(cam.id)}/${enc(cam.name)}") } }
            composable("player/{id}/{name}") { e ->
                val id = e.arguments?.getString("id") ?: return@composable
                PlayerScreen(c.repo, URLDecoder.decode(id, "UTF-8"), URLDecoder.decode(e.arguments?.getString("name") ?: "", "UTF-8"), onBack = { nav.popBackStack() })
            }
            screen("profile") { ProfileScreen(c, me, onSignOut) }
            screen("employees") { EmployeesScreen(c, me, it, go) }
            composable("employee/{p}", listOf(navArgument("p") { type = NavType.StringType })) { e -> if (Access.can("employees", me.role)) EmployeeDetailScreen(c, me, p(e) ?: "", go, onBack = { nav.popBackStack() }) else Denied("employees") }
            screen("schedule") { ScheduleScreen(c, me, it, go) }
            screen("attendance") { AttendanceScreen(c, me, go) }
            screen("payroll") { PayrollScreen(c, me, go) }
            screen("payrollRules") { PayrollRulesScreen(c, me) }
            screen("expenses") { ExpensesScreen(c, me, it) }
            screen("cashflow") { CashflowScreen(c, me) }
            screen("pnl") { PnlScreen(c, me, go) }
            screen("forecast") { ForecastScreen(c, me) }
            screen("analytics") { AnalyticsScreen(c, me, go) }
            screen("reports") { ReportsScreen(c, me, it) }
            screen("tax") { TaxScreen(c, me) }
            screen("suppliers") { SuppliersScreen(c, me, it, go) }
            screen("payables") { PayablesScreen(c, me, it, go) }
            screen("doctors") { DoctorsScreen(c, me, it) }
            screen("expiry") { ExpiryScreen(c, me) }
            screen("ai") { AiReviewScreen(c, me, it) }
            screen("videowall") { VideoWallScreen(c, me) { cam -> nav.navigate("player/${enc(cam.id)}/${enc(cam.name)}") } }
            screen("live") { LiveBranchScreen(c, me, go) { cam -> nav.navigate("player/${enc(cam.id)}/${enc(cam.name)}") } }
            screen("fom") { FomScreen(c, me) }
            screen("health") { HealthScreen(c, me, go) }
            screen("users") { UsersScreen(c, me) }
            screen("roles") { RolesScreen(c, me) }
            screen("modules") { ModulesScreen(c, me) }
            screen("aiSettings") { AiSettingsScreen(c, me) }
            screen("settings") { SettingsScreen(c, me, go) }
            screen("more") { MoreScreen(me, tabs, go) }
        }
    }
}

@Composable
private fun Placeholder(id: String) {
    val it = Access.byId[id]
    ScreenScaffold(it?.label ?: id, it?.group) { EmptyBox("«${it?.label}» bo‘limi keyingi yangilanishda — API: tayyor, ekran qurilmoqda.", Modifier.fillMaxSize()) }
}

@Composable
private fun Denied(id: String) {
    ScreenScaffold(Access.byId[id]?.label ?: id) { Box(Modifier.fillMaxSize()) { EmptyBox("Bu bo‘lim sizning rolingiz uchun yopiq.", Modifier.fillMaxSize()) } }
}
