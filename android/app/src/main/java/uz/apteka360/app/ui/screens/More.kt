package uz.apteka360.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.apteka360.app.data.Me
import uz.apteka360.app.ui.components.AppCard
import uz.apteka360.app.ui.components.Avatar
import uz.apteka360.app.ui.components.ListRow
import uz.apteka360.app.ui.components.sectionTitle
import uz.apteka360.app.ui.home.roleLabel
import uz.apteka360.app.ui.nav.Access

/** «Yana»: pastki panelga sig'magan bo'limlar guruh bo'yicha (rolga qarab). */
@Composable
fun MoreScreen(c: uz.apteka360.app.AppContainer, me: Me, tabs: List<String>, go: (String, String?) -> Unit) {
    val groups = Access.moreGroups(me.role, tabs)
    ScreenScaffold("Yana", roleLabel(me.role)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { AppCard(padding = PaddingValues(0.dp)) { ListRow(me.name, uz.apteka360.app.ui.profile.profileSubtitle(c, me), leading = { Avatar(me.ini.ifBlank { me.name.take(1) }, 40) }, chevron = true, onClick = { go("profile", null) }) } }
            groups.forEach { (g, items) ->
                sectionTitle(g)
                item(g) { AppCard(padding = PaddingValues(0.dp)) { items.forEachIndexed { i, it -> if (i > 0) HorizontalDivider(); ListRow(it.label, leading = { Icon(it.icon, null, tint = MaterialTheme.colorScheme.primary) }, chevron = true, onClick = { go(it.id, null) }) } } }
            }
        }
    }
}
