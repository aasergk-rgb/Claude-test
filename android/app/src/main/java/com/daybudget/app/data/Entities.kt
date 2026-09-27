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
    @ColumnInfo(name = "morning_notify", defaultValue = "0") val morningNotify: Boolean = false,
    @ColumnInfo(name = "morning_time", defaultValue = "480") val morningTime: Int = 480,
    @ColumnInfo(name = "evening_notify", defaultValue = "0") val eveningNotify: Boolean = false,
    @ColumnInfo(name = "evening_time", defaultValue = "1260") val eveningTime: Int = 1260,
    @ColumnInfo(name = "notify_prompt_dismissed", defaultValue = "0") val notifyPromptDismissed: Boolean = false,
    @ColumnInfo(name = "last_recap_end") val lastRecapEnd: String? = null,
    @ColumnInfo(name = "presets_seeded", defaultValue = "0") val presetsSeeded: Boolean = false,
)

@Entity(tableName = "quick_presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val label: String,
    val amount: Int,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "order_num") val order: Int,
)

@Entity(tableName = "planned_expenses", indices = [Index(value = ["date"], name = "idx_planned_date")])
data class PlannedEntity(
    @PrimaryKey val id: String,
    val label: String,
    val amount: Int,
    /** YYYY-MM-DD */
    val date: String,
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
