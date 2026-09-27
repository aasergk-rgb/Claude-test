package com.daybudget.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettings(): SettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: SettingsEntity)

    @Query("SELECT * FROM expenses ORDER BY date, created_at")
    fun observeExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY date, created_at")
    suspend fun getExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpense(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: String)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()

    @Query("DELETE FROM user_settings")
    suspend fun deleteSettings()

    @Query("SELECT * FROM quick_presets ORDER BY order_num")
    fun observePresets(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM quick_presets ORDER BY order_num")
    suspend fun getPresets(): List<PresetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Query("DELETE FROM quick_presets WHERE id = :id")
    suspend fun deletePreset(id: String)

    @Query("DELETE FROM quick_presets")
    suspend fun deleteAllPresets()

    @Query("SELECT * FROM planned_expenses ORDER BY date")
    fun observePlanned(): Flow<List<PlannedEntity>>

    @Query("SELECT * FROM planned_expenses ORDER BY date")
    suspend fun getPlanned(): List<PlannedEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanned(planned: PlannedEntity)

    @Query("DELETE FROM planned_expenses WHERE id = :id")
    suspend fun deletePlanned(id: String)

    @Query("DELETE FROM planned_expenses")
    suspend fun deleteAllPlanned()

    @Query("SELECT * FROM categories ORDER BY order_num")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY order_num")
    suspend fun getCategories(): List<CategoryEntity>

    @Upsert
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id AND built_in = 0")
    suspend fun deleteCategory(id: String)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM recurring_expenses ORDER BY day_of_month")
    fun observeRecurring(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_expenses ORDER BY day_of_month")
    suspend fun getRecurring(): List<RecurringEntity>

    @Upsert
    suspend fun upsertRecurring(recurring: RecurringEntity)

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurring(id: String)

    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurring()

    @Transaction
    suspend fun resetAll() {
        deleteAllExpenses()
        deleteAllPresets()
        deleteAllPlanned()
        deleteAllRecurring()
        deleteAllCategories()
        deleteSettings()
    }

    /** バックアップから丸ごと置き換える */
    @Transaction
    suspend fun replaceAll(
        settings: SettingsEntity,
        expenses: List<ExpenseEntity>,
        presets: List<PresetEntity>,
        planned: List<PlannedEntity>,
        categories: List<CategoryEntity>,
        recurring: List<RecurringEntity>,
    ) {
        resetAll()
        upsertSettings(settings)
        expenses.forEach { insertExpense(it) }
        presets.forEach { insertPreset(it) }
        planned.forEach { insertPlanned(it) }
        if (categories.isNotEmpty()) upsertCategories(categories)
        recurring.forEach { upsertRecurring(it) }
    }
}
