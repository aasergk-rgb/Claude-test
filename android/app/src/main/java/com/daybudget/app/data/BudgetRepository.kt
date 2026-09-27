package com.daybudget.app.data

import com.daybudget.app.domain.DEFAULT_PRESETS
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.PlannedExpense
import com.daybudget.app.domain.QuickPreset
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

    val presets: Flow<List<QuickPreset>> = dao.observePresets().map { list -> list.map { it.toDomain() } }

    val planned: Flow<List<PlannedExpense>> = dao.observePlanned().map { list -> list.map { it.toDomain() } }

    suspend fun loadSettings(): UserSettings? = dao.getSettings()?.toDomain()

    suspend fun loadPresets(): List<QuickPreset> = dao.getPresets().map { it.toDomain() }

    suspend fun loadPlanned(): List<PlannedExpense> = dao.getPlanned().map { it.toDomain() }

    /** 初回だけ「よく使う金額」の例を入れる（全部消した人には入れ直さない） */
    suspend fun seedPresetsIfNeeded() {
        val s = loadSettings() ?: return
        if (s.presetsSeeded || !s.onboarded) return
        if (dao.getPresets().isEmpty()) {
            DEFAULT_PRESETS.forEachIndexed { i, (label, amount, cat) ->
                dao.insertPreset(PresetEntity(UUID.randomUUID().toString(), label, amount, cat, i))
            }
        }
        saveSettings(s.copy(presetsSeeded = true))
    }

    suspend fun addPreset(label: String, amount: Int, categoryId: String) {
        val order = (dao.getPresets().maxOfOrNull { it.order } ?: -1) + 1
        dao.insertPreset(PresetEntity(UUID.randomUUID().toString(), label, amount, categoryId, order))
        onDataChanged()
    }

    suspend fun deletePreset(id: String) {
        dao.deletePreset(id)
        onDataChanged()
    }

    /** よく使う金額で今日の支出を記録する（ウィジェットのボタンからも呼ぶ） */
    suspend fun quickAdd(presetId: String, today: LocalDate): Expense? {
        val p = dao.getPresets().firstOrNull { it.id == presetId } ?: return null
        return addExpense(p.amount, p.categoryId, p.label, today)
    }

    suspend fun addPlanned(label: String, amount: Int, date: LocalDate) {
        dao.insertPlanned(PlannedEntity(UUID.randomUUID().toString(), label, amount, date.toString()))
        onDataChanged()
    }

    suspend fun deletePlanned(id: String) {
        dao.deletePlanned(id)
        onDataChanged()
    }

    suspend fun exportBackup(): String = Backup.encode(
        dao.getSettings(), dao.getExpenses(), dao.getPresets(), dao.getPlanned(),
    )

    /** バックアップを読み込んで、今のデータと置き換える */
    suspend fun importBackup(json: String) {
        val b = Backup.decode(json)
        dao.replaceAll(b.settings, b.expenses, b.presets, b.planned)
        onDataChanged()
    }

    suspend fun loadExpenses(): List<Expense> = dao.getExpenses().map { it.toDomain() }

    suspend fun completeOnboarding(monthlyBudget: Int, closingDay: Int, today: LocalDate) {
        val base = loadSettings() ?: UserSettings()
        saveSettings(base.copy(monthlyBudget = monthlyBudget, closingDay = closingDay, onboarded = true, startDate = today))
        seedPresetsIfNeeded()
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
