package com.daybudget.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daybudget.app.data.BudgetRepository
import com.daybudget.app.domain.Category
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.RecurringExpense
import com.daybudget.app.domain.OnboardingChoices
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
        /** 予約（手で入れたもの＋決まった出費の分）。isRecurring で見分ける */
        val planned: List<PlannedExpense> = emptyList(),
        val categories: List<Category> = emptyList(),
        val recurring: List<RecurringExpense> = emptyList(),
    ) : AppState
}

class MainViewModel(private val repo: BudgetRepository) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    private val base = combine(repo.settings, repo.expenses, today, repo.presets, repo.planned) { s, e, t, p, pl ->
        AppState.Ready(s ?: UserSettings(), e, t, p, pl)
    }

    val state: StateFlow<AppState> = combine(base, repo.categories, repo.recurring) { b, cats, rec ->
        val virtual = rec.flatMap { it.asPlanned(b.today.minusYears(1), b.today.plusYears(1)) }
        b.copy(planned = b.planned + virtual, categories = cats, recurring = rec)
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
        viewModelScope.launch {
            repo.ensureCategories()
            repo.materializeRecurring(today.value)
        }
    }

    fun saveCategory(c: Category) = viewModelScope.launch { repo.saveCategory(c) }

    fun addCategory(label: String, icon: String, color: Long) = viewModelScope.launch { repo.addCategory(label, icon, color) }

    fun deleteCategory(id: String) = viewModelScope.launch { repo.deleteCategory(id) }

    fun moveCategory(id: String, up: Boolean) = viewModelScope.launch { repo.moveCategory(id, up) }

    fun addRecurring(label: String, amount: Int, day: Int, categoryId: String) =
        viewModelScope.launch { repo.addRecurring(label, amount, day, categoryId, today.value) }

    fun deleteRecurring(id: String) = viewModelScope.launch { repo.deleteRecurring(id) }

    fun completeOnboarding(choices: OnboardingChoices) = viewModelScope.launch {
        repo.completeOnboarding(choices, today.value)
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) = viewModelScope.launch { repo.updateSettings(transform) }

    fun addExpense(amount: Int, categoryId: String, memo: String?, date: LocalDate) =
        viewModelScope.launch { repo.addExpense(amount, categoryId, memo, date) }

    /** 記録して、その記録を返す（「元に戻す」で消すため） */
    suspend fun addExpenseNow(amount: Int, categoryId: String, memo: String?, date: LocalDate): Expense =
        repo.addExpense(amount, categoryId, memo, date)

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
