package com.daybudget.app.domain

import java.time.Instant
import java.time.LocalDate

data class Expense(
    val id: String,
    /** 支出金額（正の整数、円） */
    val amount: Int,
    val categoryId: String,
    val memo: String?,
    val date: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class CarryoverMode(val key: String) {
    /** 使わなかった分を翌日以降に均等配分する */
    DISTRIBUTE("distribute"),

    /** 毎日の予算は固定。余りは貯金として貯める（Pro） */
    SAVINGS("savings");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: DISTRIBUTE
    }
}

enum class AppTheme(val key: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

enum class WidgetTheme(val key: String, val label: String) {
    DARK("dark", "ダーク"), WHITE("white", "ホワイト"), NEON("neon", "ネオン");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: DARK
    }
}

data class UserSettings(
    val monthlyBudget: Int = 50_000,
    /** 1〜31。31 は「月末」 */
    val closingDay: Int = CLOSING_END_OF_MONTH,
    val carryoverMode: CarryoverMode = CarryoverMode.DISTRIBUTE,
    val theme: AppTheme = AppTheme.SYSTEM,
    val widgetTheme: WidgetTheme = WidgetTheme.DARK,
    val isPro: Boolean = false,
    val onboarded: Boolean = false,
    val lastCategoryId: String = Categories.FOOD.id,
    /** 使い始めた日。最初の月度の予算按分に使う */
    val startDate: LocalDate? = null,
)
