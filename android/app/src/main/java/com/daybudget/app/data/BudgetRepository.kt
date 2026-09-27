package com.daybudget.app.data

import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class BudgetRepository(
    private val dao: BudgetDao,
    /** データが変わったらホーム画面ウィジェットを更新する */
    private val onDataChanged: suspend () -> Unit = {},
    private val clock: () -> Instant = Instant::now,
) {
    /** 未設定なら null */
    val settings: Flow<UserSettings?> = dao.observeSettings().map { it?.toDomain() }

    val expenses: Flow<List<Expense>> = dao.observeExpenses().map { list -> list.map { it.toDomain() } }

    suspend fun loadSettings(): UserSettings? = dao.getSettings()?.toDomain()

    suspend fun loadExpenses(): List<Expense> = dao.getExpenses().map { it.toDomain() }

    suspend fun completeOnboarding(monthlyBudget: Int, closingDay: Int, today: LocalDate) {
        val base = loadSettings() ?: UserSettings()
        saveSettings(base.copy(monthlyBudget = monthlyBudget, closingDay = closingDay, onboarded = true, startDate = today))
    }

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        saveSettings(transform(loadSettings() ?: UserSettings()))
    }

    private suspend fun saveSettings(settings: UserSettings) {
        val now = clock().toString()
        val createdAt = dao.getSettings()?.createdAt ?: now
        dao.upsertSettings(settings.toEntity(createdAt, now))
        onDataChanged()
    }

    suspend fun addExpense(amount: Int, categoryId: String, memo: String?, date: LocalDate): Expense {
        val now = clock()
        val expense = Expense(UUID.randomUUID().toString(), amount, categoryId, memo, date, now, now)
        dao.insertExpense(expense.toEntity())
        loadSettings()?.let { if (it.lastCategoryId != categoryId) dao.upsertSettings(it.copy(lastCategoryId = categoryId).toEntity(dao.getSettings()!!.createdAt, now.toString())) }
        onDataChanged()
        return expense
    }

    suspend fun updateExpense(id: String, amount: Int, categoryId: String, memo: String?, date: LocalDate) {
        val current = dao.getExpense(id)?.toDomain() ?: return
        dao.insertExpense(current.copy(amount = amount, categoryId = categoryId, memo = memo, date = date, updatedAt = clock()).toEntity())
        onDataChanged()
    }

    suspend fun deleteExpense(id: String): Expense? {
        val removed = dao.getExpense(id)?.toDomain() ?: return null
        dao.deleteExpense(id)
        onDataChanged()
        return removed
    }

    suspend fun restoreExpense(expense: Expense) {
        dao.insertExpense(expense.toEntity())
        onDataChanged()
    }

    suspend fun resetAll() {
        dao.resetAll()
        onDataChanged()
    }
}
