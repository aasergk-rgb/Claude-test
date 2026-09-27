package com.daybudget.app

import android.app.Application
import com.daybudget.app.data.AppDatabase
import com.daybudget.app.data.BudgetRepository
import com.daybudget.app.notify.DailyNotifications
import com.daybudget.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayBudgetApp : Application() {
    val repository: BudgetRepository by lazy {
        BudgetRepository(
            AppDatabase.get(this).dao(),
            onDataChanged = {
                WidgetUpdater.updateAll(this)
                loadSettingsForNotifications()
            },
        )
    }

    /** アプリ全体で使う（通知の予約など、画面より長く生きる処理用） */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        DailyNotifications.ensureChannel(this)
        WidgetUpdater.scheduleMidnightRefresh(this)
        appScope.launch {
            repository.ensureCategories()
            repository.materializeRecurring(java.time.LocalDate.now())
            repository.seedPresetsIfNeeded()
            repository.loadSettings()?.let { DailyNotifications.schedule(this@DayBudgetApp, it) }
        }
    }

    private suspend fun loadSettingsForNotifications() {
        repository.loadSettings()?.let { DailyNotifications.schedule(this, it) }
    }
}
