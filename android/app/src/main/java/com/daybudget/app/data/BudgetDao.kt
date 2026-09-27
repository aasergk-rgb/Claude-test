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

    @Transaction
    suspend fun resetAll() {
        deleteAllExpenses()
        deleteAllPresets()
        deleteAllPlanned()
        deleteSettings()
    }

    /** バックアップから丸ごと置き換える */
    @Transaction
    suspend fun replaceAll(settings: SettingsEntity, expenses: List<ExpenseEntity>, presets: List<PresetEntity>, planned: List<PlannedEntity>) {
        resetAll()
        upsertSettings(settings)
        expenses.forEach { insertExpense(it) }
        presets.forEach { insertPreset(it) }
        planned.forEach { insertPlanned(it) }
    }
}
