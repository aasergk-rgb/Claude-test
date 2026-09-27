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
    DARK("dark", "ダーク"), WHITE("white", "ホワイト"), NEON("neon", "ネオン"), SAKURA("sakura", "サクラ"), MINT("mint", "ミント");

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
    /** 朝のお知らせ（今日使える額） */
    val morningNotify: Boolean = false,
    /** 0時からの分 */
    val morningTime: Int = 8 * 60,
    /** 夜のリマインド */
    val eveningNotify: Boolean = false,
    val eveningTime: Int = 21 * 60,
    /** ダッシュボードの「通知をオンにしますか？」を閉じたか */
    val notifyPromptDismissed: Boolean = false,
    /** 最後に振り返りカードを見せた月度の終了日 */
    val lastRecapEnd: LocalDate? = null,
    /** よく使う金額の初期値を入れたか（全部消した人に再度入れないため） */
    val presetsSeeded: Boolean = false,
)

/** よく使う金額（ウィジェットとアプリから1タップで記録） */
data class QuickPreset(
    val id: String,
    val label: String,
    val amount: Int,
    val categoryId: String,
    val order: Int,
)

/** 先の日付に予約しておく大きな出費（飲み会など） */
data class PlannedExpense(
    val id: String,
    val label: String,
    val amount: Int,
    val date: LocalDate,
)

val DEFAULT_PRESETS = listOf(
    Triple("コーヒー", 150, "cafe"),
    Triple("ランチ", 800, "food"),
    Triple("コンビニ", 500, "food"),
)
