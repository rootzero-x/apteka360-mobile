package uz.apteka360.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import uz.apteka360.app.AppContainer
import uz.apteka360.app.data.Me
import uz.apteka360.app.ui.cameras.CamerasScreen
import uz.apteka360.app.ui.cameras.PlayerScreen
import uz.apteka360.app.ui.home.HomeScreen
import uz.apteka360.app.ui.home.HomeViewModel
import uz.apteka360.app.ui.notifications.NotificationsScreen
import uz.apteka360.app.ui.profile.ProfileScreen
import java.net.URLDecoder
import java.net.URLEncoder

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val selected: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
fun MainScaffold(c: AppContainer, me: Me, onSignOut: () -> Unit) {
    val nav = rememberNavController()
    val tabs = buildList {
        add(Tab("home", "Asosiy", Icons.Outlined.Home, Icons.Filled.Home))
        add(Tab("notifications", "Xabarlar", Icons.Outlined.Notifications, Icons.Filled.Notifications))
        if (!me.isEmployee) add(Tab("cameras", "Kameralar", Icons.Outlined.Videocam, Icons.Filled.Videocam))
        add(Tab("profile", "Profil", Icons.Outlined.Person, Icons.Filled.Person))
    }
    val backStack by nav.currentBackStackEntryAsState()
    val dest = backStack?.destination
    val showBar = dest?.route?.startsWith("player") != true

    Scaffold(bottomBar = {
        if (showBar) NavigationBar {
            tabs.forEach { t ->
                val selected = dest?.hierarchy?.any { it.route == t.route } == true
                NavigationBarItem(selected = selected, onClick = {
                    nav.navigate(t.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                }, icon = { Icon(if (selected) t.selected else t.icon, t.label) }, label = { Text(t.label) })
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(bottom = pad.calculateBottomPadding())) {
            composable("home") {
                val vm: HomeViewModel = viewModel(factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { HomeViewModel(c, me) } })
                HomeScreen(vm)
            }
            composable("notifications") { NotificationsScreen(c.repo) }
            composable("cameras") { CamerasScreen(c.repo) { cam -> nav.navigate("player/${cam.id}/${URLEncoder.encode(cam.name, "UTF-8")}") } }
            composable("player/{id}/{name}") { e ->
                val id = e.arguments?.getString("id") ?: return@composable
                val name = URLDecoder.decode(e.arguments?.getString("name") ?: "", "UTF-8")
                PlayerScreen(c.repo, id, name, onBack = { nav.popBackStack() })
            }
            composable("profile") { ProfileScreen(me, c.repo, onSignOut) }
        }
    }
}
