package fr.isep.campuspass.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.isep.campuspass.domain.Role
import fr.isep.campuspass.domain.UserAccount
import fr.isep.campuspass.ui.components.LocalSnackbar
import fr.isep.campuspass.ui.screens.*
import fr.isep.campuspass.ui.theme.Brand
import fr.isep.campuspass.viewmodel.Factories
import fr.isep.campuspass.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

private object Routes {
    const val EVENTS = "events"
    const val TICKETS = "tickets"
    const val SCANNER = "scanner"
    const val PROFILE = "profile"
    const val LOGIN = "login"
    const val EVENT = "event/{id}"
    const val CHECKOUT = "checkout/{id}/{qty}"
    const val SUCCESS = "success/{tx}"
    const val TICKET = "ticket/{id}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Protected route: shows the login screen until a user is signed in. */
@Composable
private fun Protected(user: UserAccount?, onBack: (() -> Unit)?, content: @Composable () -> Unit) {
    if (user == null) LoginScreen(onBack) else content()
}

@Composable
fun CampusPassApp(session: SessionViewModel = viewModel(factory = Factories.session)) {
    val nav = rememberNavController()
    val user by session.user.collectAsStateWithLifecycle()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val back: () -> Unit = { nav.popBackStack() }

    val tabs = buildList {
        add(Tab(Routes.EVENTS, "Events", Icons.Rounded.Explore))
        if (user?.role == Role.ORGANIZER) add(Tab(Routes.SCANNER, "Scanner", Icons.Rounded.QrCodeScanner))
        else add(Tab(Routes.TICKETS, "My tickets", Icons.Rounded.ConfirmationNumber))
        add(Tab(Routes.PROFILE, "Profile", Icons.Rounded.Person))
    }
    fun goTab(target: String) = nav.navigate(target) {
        popUpTo(Routes.EVENTS) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    CompositionLocalProvider(LocalSnackbar provides { msg: String -> scope.launch { snackbarHost.showSnackbar(msg) } }) {
        Scaffold(
            containerColor = Brand.Background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(snackbarHost) { Snackbar(it, containerColor = Brand.Navy, contentColor = Color.White, actionColor = Brand.Yellow) }
            },
            bottomBar = {
                if (tabs.any { it.route == route }) NavigationBar(containerColor = Color.White) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = { goTab(tab.route) },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Brand.Navy, selectedTextColor = Brand.Navy, indicatorColor = Brand.Yellow,
                                unselectedIconColor = Brand.Muted, unselectedTextColor = Brand.Muted,
                            ),
                        )
                    }
                }
            },
        ) { inner ->
            NavHost(
                nav, startDestination = Routes.EVENTS, modifier = Modifier.fillMaxSize().padding(inner),
                enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 12 } },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(250)) },
                popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { it / 12 } },
            ) {
                composable(Routes.EVENTS) {
                    EventsScreen(user, onOpen = { nav.navigate("event/$it") }, onLogin = { nav.navigate(Routes.LOGIN) })
                }
                composable(Routes.EVENT, listOf(navArgument("id") { type = NavType.IntType })) { e ->
                    EventDetailScreen(e.arguments!!.getInt("id"), back, onCheckout = { id, qty -> nav.navigate("checkout/$id/$qty") })
                }
                composable(Routes.CHECKOUT, listOf(navArgument("id") { type = NavType.IntType }, navArgument("qty") { type = NavType.IntType })) { e ->
                    Protected(user, back) {
                        CheckoutScreen(e.arguments!!.getInt("id"), e.arguments!!.getInt("qty"), back, onPaid = { tx ->
                            nav.navigate("success/$tx") { popUpTo(Routes.EVENTS) }
                        })
                    }
                }
                composable(Routes.SUCCESS, listOf(navArgument("tx") { type = NavType.StringType })) { e ->
                    SuccessRoute(e.arguments!!.getString("tx")!!, onTickets = { goTab(Routes.TICKETS) }, onHome = { goTab(Routes.EVENTS) })
                }
                composable(Routes.TICKETS) {
                    Protected(user, null) { MyTicketsScreen(onOpen = { nav.navigate("ticket/$it") }, onBrowse = { goTab(Routes.EVENTS) }) }
                }
                composable(Routes.TICKET, listOf(navArgument("id") { type = NavType.StringType })) { e ->
                    Protected(user, back) { TicketDetailScreen(e.arguments!!.getString("id")!!, back) }
                }
                composable(Routes.SCANNER) { Protected(user, null) { ScannerScreen() } }
                composable(Routes.PROFILE) { Protected(user, null) { ProfileScreen() } }
                composable(Routes.LOGIN) {
                    LaunchedEffect(user) { if (user != null) nav.popBackStack() }
                    LoginScreen(back)
                }
            }
        }
    }
}

@Composable
private fun SuccessRoute(tx: String, onTickets: () -> Unit, onHome: () -> Unit) = PaymentSuccessScreen(tx, onTickets, onHome)
