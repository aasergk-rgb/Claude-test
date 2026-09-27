package com.daybudget.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.daybudget.app.ui.AppState
import com.daybudget.app.ui.DayBudgetRoot
import com.daybudget.app.ui.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel((application as DayBudgetApp).repository) } }
    }

    /** ウィジェットから開いたときの行き先（例: paywall） */
    private val requestedRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.state.value is AppState.Loading }
        enableEdgeToEdge()
        requestedRoute.value = intent.getStringExtra(EXTRA_ROUTE)
        setContent { DayBudgetRoot(viewModel, requestedRoute) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        requestedRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshToday()
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}

