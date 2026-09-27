package com.daybudget.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daybudget.app.data.BudgetRepository
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.PlannedExpense
import com.daybudget.app.domain.QuickPreset
import com.daybudget.app.domain.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface AppState {
    data object Loading : AppState
    data class Ready(
        val settings: UserSettings,
        val expenses: List<Expense>,
        val today: LocalDate,
        val presets: List<QuickPreset> = emptyList(),
        val planned: List<PlannedExpense> = emptyList(),
    ) : AppState
}

class MainViewModel(private val repo: BudgetRepository) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    val state: StateFlow<AppState> = combine(repo.settings, repo.expenses, today, repo.presets, repo.planned) { s, e, t, p, pl ->
        AppState.Ready(s ?: UserSettings(), e, t, p, pl)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

    init {
        // 深夜0時をまたいだら「今日」を更新する
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                delay(Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis() + 1_000)
                refreshToday()
            }
        }
    }

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun completeOnboarding(budget: Int, closingDay: Int) = viewModelScope.launch {
        repo.completeOnboarding(budget, closingDay, today.value)
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) = viewModelScope.launch { repo.updateSettings(transform) }

    fun addExpense(amount: Int, categoryId: String, memo: String?, date: LocalDate) =
        viewModelScope.launch { repo.addExpense(amount, categoryId, memo, date) }

    fun updateExpense(id: String, amount: Int, categoryId: String, memo: String?, date: LocalDate) =
        viewModelScope.launch { repo.updateExpense(id, amount, categoryId, memo, date) }

    suspend fun deleteExpense(id: String): Expense? = repo.deleteExpense(id)

    fun restoreExpense(expense: Expense) = viewModelScope.launch { repo.restoreExpense(expense) }

    fun resetAll() = viewModelScope.launch { repo.resetAll() }

    suspend fun allExpenses(): List<Expense> = repo.loadExpenses()

    suspend fun quickAdd(presetId: String): Expense? = repo.quickAdd(presetId, today.value)

    fun addPreset(label: String, amount: Int, categoryId: String) = viewModelScope.launch { repo.addPreset(label, amount, categoryId) }

    fun deletePreset(id: String) = viewModelScope.launch { repo.deletePreset(id) }

    fun addPlanned(label: String, amount: Int, date: LocalDate) = viewModelScope.launch { repo.addPlanned(label, amount, date) }

    fun deletePlanned(id: String) = viewModelScope.launch { repo.deletePlanned(id) }

    suspend fun exportBackup(): String = repo.exportBackup()

    suspend fun importBackup(json: String) = repo.importBackup(json)
}
