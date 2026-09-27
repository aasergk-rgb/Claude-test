package com.daybudget.app.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daybudget.app.ui.components.ExpenseSheet
import com.daybudget.app.ui.components.SheetTarget
import com.daybudget.app.ui.screens.DashboardScreen
import com.daybudget.app.ui.screens.HistoryScreen
import com.daybudget.app.ui.screens.OnboardingScreen
import com.daybudget.app.ui.screens.PaywallScreen
import com.daybudget.app.ui.screens.RecapScreen
import com.daybudget.app.ui.screens.SettingsScreen
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.DayBudgetTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

object Routes {
    const val DASHBOARD = "dashboard"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
    const val RECAP = "recap/{end}"

    /** ウィジェット・通知から「支出を記録」を開くときの行き先 */
    const val ADD = "add"

    fun recap(end: java.time.LocalDate) = "recap/$end"
}

/** 画面下のお知らせ（「元に戻す」付きにもできる） */
class Messenger(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(message, actionLabel, duration = if (actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

val LocalMessenger = staticCompositionLocalOf<Messenger> { error("Messenger がありません") }

@Composable
fun DayBudgetRoot(viewModel: MainViewModel, requestedRoute: MutableStateFlow<String?>) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? AppState.Ready ?: return
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val messenger = remember { Messenger(snackbar, scope) }
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }

    DayBudgetTheme(ready.settings.theme) {
        val dark = Db.colors.isDark
        val activity = LocalActivity.current as? ComponentActivity
        LaunchedEffect(dark, activity) {
            val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
            activity?.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        }
        CompositionLocalProvider(LocalMessenger provides messenger) {
            Box(Modifier.fillMaxSize().background(Db.colors.appBg)) {
                if (!ready.settings.onboarded) {
                    OnboardingScreen(ready.today, onDone = viewModel::completeOnboarding)
                } else {
                    val nav = rememberNavController()
                    val route by requestedRoute.collectAsState()
                    LaunchedEffect(route) {
                        route?.let {
                            if (it == Routes.ADD) sheet = SheetTarget.Add(ready.today)
                            else nav.navigate(it) { launchSingleTop = true }
                            requestedRoute.value = null
                        }
                    }
                    NavHost(
                        nav,
                        startDestination = Routes.DASHBOARD,
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
                    ) {
                        composable(Routes.DASHBOARD) {
                            DashboardScreen(ready, viewModel, openSheet = { sheet = it }, navigate = { nav.navigate(it) })
                        }
                        composable(Routes.HISTORY) {
                            HistoryScreen(ready, openSheet = { sheet = it }, onBack = { nav.popBackStack() }, openRecap = { nav.navigate(Routes.recap(it)) })
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(ready, viewModel, onBack = { nav.popBackStack() }, navigate = { nav.navigate(it) })
                        }
                        composable(Routes.PAYWALL) {
                            PaywallScreen(onClose = { nav.popBackStack() })
                        }
                        composable(Routes.RECAP) { entry ->
                            val end = entry.arguments?.getString("end")?.let(java.time.LocalDate::parse) ?: ready.today
                            RecapScreen(ready, end, onClose = { nav.popBackStack() }, navigate = { nav.navigate(it) })
                        }
                    }
                }

                sheet?.let { target ->
                    ExpenseSheet(target, ready, viewModel, onDismiss = { sheet = null })
                }

                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 92.dp)) { data ->
                    Snackbar(
                        data,
                        shape = RoundedCornerShape(14.dp),
                        containerColor = Db.colors.ink,
                        contentColor = Db.colors.appBg,
                        actionColor = Db.colors.accent,
                    )
                }
            }
        }
    }
}
