package com.daybudget.app

import android.app.Application
import com.daybudget.app.data.AppDatabase
import com.daybudget.app.data.BudgetRepository
import com.daybudget.app.widget.WidgetUpdater

class DayBudgetApp : Application() {
    val repository: BudgetRepository by lazy {
        BudgetRepository(AppDatabase.get(this).dao(), onDataChanged = { WidgetUpdater.updateAll(this) })
    }

    override fun onCreate() {
        super.onCreate()
        WidgetUpdater.scheduleMidnightRefresh(this)
    }
}
