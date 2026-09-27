package com.daybudget.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** ユーザー設定（常に id = 1 の1行だけ） */
@Entity(tableName = "user_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "monthly_budget") val monthlyBudget: Int,
    @ColumnInfo(name = "closing_day", defaultValue = "31") val closingDay: Int,
    @ColumnInfo(name = "carryover_mode", defaultValue = "distribute") val carryoverMode: String,
    @ColumnInfo(defaultValue = "system") val theme: String,
    @ColumnInfo(name = "widget_theme", defaultValue = "dark") val widgetTheme: String,
    @ColumnInfo(name = "is_pro", defaultValue = "0") val isPro: Boolean,
    @ColumnInfo(defaultValue = "0") val onboarded: Boolean,
    @ColumnInfo(name = "last_category_id", defaultValue = "food") val lastCategoryId: String,
    /** YYYY-MM-DD */
    @ColumnInfo(name = "start_date") val startDate: String?,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)

@Entity(tableName = "expenses", indices = [Index(value = ["date"], name = "idx_expenses_date")])
data class ExpenseEntity(
    /** UUID v4 */
    @PrimaryKey val id: String,
    val amount: Int,
    @ColumnInfo(name = "category_id") val categoryId: String,
    val memo: String?,
    /** YYYY-MM-DD */
    val date: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
)
