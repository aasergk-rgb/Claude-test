package com.daybudget.app.data

import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.Category
import com.daybudget.app.domain.DEFAULT_PRESETS
import com.daybudget.app.domain.RecurringExpense
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.MAX_PRESETS
import com.daybudget.app.domain.OnboardingChoices
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

    /** カテゴリ（変わるたびに全体の一覧 Categories にも反映する） */
    val categories: Flow<List<Category>> = dao.observeCategories().map { list ->
        list.map { it.toDomain() }.also(Categories::update)
    }

    val recurring: Flow<List<RecurringExpense>> = dao.observeRecurring().map { list -> list.map { it.toDomain() } }

    suspend fun loadCategories(): List<Category> = dao.getCategories().map { it.toDomain() }.also(Categories::update)

    suspend fun loadRecurring(): List<RecurringExpense> = dao.getRecurring().map { it.toDomain() }

    /** 予約（手で入れたもの＋決まった出費の分）。計算にはこちらを使う */
    suspend fun loadAllPlanned(today: LocalDate): List<PlannedExpense> =
        loadPlanned() + loadRecurring().flatMap { it.asPlanned(today.minusYears(1), today.plusYears(1)) }

    /** カテゴリが空なら最初の6つを入れる */
    suspend fun ensureCategories() {
        if (dao.getCategories().isEmpty()) dao.upsertCategories(Categories.DEFAULTS.map { it.toEntity() })
        loadCategories()
    }

    suspend fun saveCategory(category: Category) {
        dao.upsertCategories(listOf(category.toEntity()))
        loadCategories()
        onDataChanged()
    }

    suspend fun addCategory(label: String, icon: String, color: Long): Category {
        val order = (dao.getCategories().maxOfOrNull { it.order } ?: -1) + 1
        val c = Category("c_" + UUID.randomUUID().toString().take(8), label, color, icon, order)
        dao.upsertCategories(listOf(c.toEntity()))
        loadCategories()
        onDataChanged()
        return c
    }

    /** 自分で作ったカテゴリを消す（そのカテゴリの支出は「その他」として表示される） */
    suspend fun deleteCategory(id: String) {
        dao.deleteCategory(id)
        loadCategories()
        onDataChanged()
    }

    /** 並び順を1つ上げ下げする */
    suspend fun moveCategory(id: String, up: Boolean) {
        val list = dao.getCategories().sortedBy { it.order }.toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        list[i] = list[j].also { list[j] = list[i] }
        dao.upsertCategories(list.mapIndexed { index, c -> c.copy(order = index) })
        loadCategories()
        onDataChanged()
    }

    suspend fun addRecurring(label: String, amount: Int, dayOfMonth: Int, categoryId: String, today: LocalDate) {
        val r = RecurringExpense(UUID.randomUUID().toString(), label, amount, dayOfMonth, categoryId, RecurringExpense.firstStart(today))
        dao.upsertRecurring(r.toEntity())
        onDataChanged()
    }

    suspend fun deleteRecurring(id: String) {
        dao.deleteRecurring(id)
        onDataChanged()
    }

    /** 支払日を迎えた決まった出費を、支出として自動で記録する。記録した件数を返す */
    suspend fun materializeRecurring(today: LocalDate): Int {
        var count = 0
        for (r in loadRecurring()) {
            val from = r.lastRecorded?.plusDays(1) ?: r.startDate
            val dates = r.occurrences(from, today)
            dates.forEach { date ->
                val now = clock()
                dao.insertExpense(Expense(UUID.randomUUID().toString(), r.amount, r.categoryId, r.label, date, now, now).toEntity())
            }
            if (dates.isNotEmpty() || r.lastRecorded == null || r.lastRecorded.isBefore(today)) {
                dao.upsertRecurring(r.copy(lastRecorded = maxOf(today, r.startDate.minusDays(1))).toEntity())
            }
            count += dates.size
        }
        if (count > 0) onDataChanged()
        return count
    }

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
        dao.getSettings(), dao.getExpenses(), dao.getPresets(), dao.getPlanned(), dao.getCategories(), dao.getRecurring(),
    )

    /** バックアップを読み込んで、今のデータと置き換える */
    suspend fun importBackup(json: String) {
        val b = Backup.decode(json)
        dao.replaceAll(b.settings, b.expenses, b.presets, b.planned, b.categories, b.recurring)
        ensureCategories()
        onDataChanged()
    }

    suspend fun loadExpenses(): List<Expense> = dao.getExpenses().map { it.toDomain() }

    suspend fun completeOnboarding(monthlyBudget: Int, closingDay: Int, today: LocalDate) {
        val base = loadSettings() ?: UserSettings()
        saveSettings(base.copy(monthlyBudget = monthlyBudget, closingDay = closingDay, onboarded = true, startDate = today))
        seedPresetsIfNeeded()
    }

    /** 初回設定の内容（よく使う金額・通知を含む）を保存する */
    suspend fun completeOnboarding(choices: OnboardingChoices, today: LocalDate) {
        dao.deleteAllPresets()
        choices.presets.take(MAX_PRESETS).forEachIndexed { i, (label, amount, cat) ->
            dao.insertPreset(PresetEntity(UUID.randomUUID().toString(), label, amount, cat, i))
        }
        val base = loadSettings() ?: UserSettings()
        saveSettings(
            base.copy(
                monthlyBudget = choices.monthlyBudget,
                closingDay = choices.closingDay,
                onboarded = true,
                startDate = today,
                presetsSeeded = true,
                firstPeriodBudget = choices.firstPeriodBudget,
                morningNotify = choices.morningNotify,
                eveningNotify = choices.eveningNotify,
                // 初回設定で聞いたので、ホームでは改めて聞かない
                notifyPromptDismissed = true,
            ),
        )
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
        // 収入は「前回のカテゴリ」として覚えない
        val current = dao.getSettings()
        if (current != null && categoryId != Categories.INCOME.id && current.lastCategoryId != categoryId) {
            dao.upsertSettings(current.copy(lastCategoryId = categoryId, updatedAt = now.toString()))
        }
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
        ensureCategories()
        onDataChanged()
    }
}
