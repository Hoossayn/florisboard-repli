package dev.patrickgold.florisboard.app.settings.repli

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.Routes
import org.florisboard.lib.compose.stringRes

private val RepliAccent = Color(0xFF6654D1)
private val RepliAccentSoft = Color(0xFFEEEAFE)
private val RepliMuted = Color(0xFF6E6A80)

private data class RepliTab(val route: Any, val label: Int, val icon: Int)

private val RepliTabs = listOf(
    RepliTab(Routes.Settings.RepliHome, R.string.repli_nav__home, R.drawable.ic_nav_home),
    RepliTab(Routes.Settings.RepliChats, R.string.repli_nav__chats, R.drawable.ic_nav_chats),
    RepliTab(Routes.Settings.Home, R.string.repli_nav__settings, R.drawable.ic_nav_settings),
)

@Composable
fun RepliBottomNav(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedIndex = RepliTabs.indexOfFirst { destination?.hasRoute(it.route::class) == true }
    if (selectedIndex < 0) return
    NavigationBar(containerColor = Color.White) {
        RepliTabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = index == selectedIndex,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(tab.icon),
                        contentDescription = stringRes(tab.label),
                    )
                },
                label = { Text(stringRes(tab.label)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RepliAccent,
                    selectedTextColor = RepliAccent,
                    unselectedIconColor = RepliMuted,
                    unselectedTextColor = RepliMuted,
                    indicatorColor = RepliAccentSoft,
                ),
            )
        }
    }
}
