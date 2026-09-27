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
    /** 週末ブースト（%）。100 ならオフ。125 なら週末は平日の 1.25 倍を割り当てる */
    val weekendBoostPct: Int = 100,
    /** 週末として扱う曜日（ISO: 月=1 … 日=7） */
    val weekendDays: Set<Int> = setOf(6, 7),
)

/**
 * 毎月決まった日に出ていくお金（サブスクなど）。
 * その日までは予約として取り分け、当日になったら自動で支出として記録する。
 */
data class RecurringExpense(
    val id: String,
    val label: String,
    val amount: Int,
    /** 1〜31。月の日数を超える場合は末日 */
    val dayOfMonth: Int,
    val categoryId: String,
    /** この日以降の分から記録する */
    val startDate: LocalDate,
    /** 最後に自動記録した日 */
    val lastRecorded: LocalDate? = null,
) {
    /** from〜to（両端を含む）に来る支払日 */
    fun occurrences(from: LocalDate, to: LocalDate): List<LocalDate> {
        val begin = maxOf(from, startDate)
        if (begin.isAfter(to)) return emptyList()
        val out = mutableListOf<LocalDate>()
        var ym = java.time.YearMonth.from(begin)
        val last = java.time.YearMonth.from(to)
        while (!ym.isAfter(last)) {
            val d = ym.atDay(minOf(dayOfMonth, ym.lengthOfMonth()))
            if (!d.isBefore(begin) && !d.isAfter(to)) out += d
            ym = ym.plusMonths(1)
        }
        return out
    }

    /** 計算で使う予約（id は「rec:元のid:日付」） */
    fun asPlanned(from: LocalDate, to: LocalDate): List<PlannedExpense> =
        occurrences(from, to).map { PlannedExpense("rec:$id:$it", label, amount, it) }

    companion object {
        /** 追加した日の翌日以降の支払日から数える */
        fun firstStart(today: LocalDate): LocalDate = today.plusDays(1)
    }
}

/** 決まった出費から作った予約か */
val PlannedExpense.isRecurring: Boolean get() = id.startsWith("rec:")

/** 収入・返金（マイナスの支出）か */
val Expense.isIncome: Boolean get() = amount < 0

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

/** 初回設定で選べる「よく使う金額」の候補 */
val PRESET_SUGGESTIONS = DEFAULT_PRESETS + listOf(
    Triple("飲み物", 130, "cafe"),
    Triple("お菓子", 200, "cafe"),
    Triple("電車", 200, "transport"),
    Triple("日用品", 300, "daily"),
    Triple("夕飯", 1000, "food"),
)

const val MAX_PRESETS = 6

/** 初回設定で選んだ内容 */
data class OnboardingChoices(
    val monthlyBudget: Int,
    val closingDay: Int,
    val presets: List<Triple<String, Int, String>>,
    val morningNotify: Boolean,
    val eveningNotify: Boolean,
)

/** お祝いする連続日数の節目 */
val STREAK_MILESTONES = listOf(3, 7, 14, 30, 60, 100)

/** 今の連続日数で到達している一番大きな節目（なければ null） */
fun reachedMilestone(streak: Int): Int? = STREAK_MILESTONES.lastOrNull { it <= streak }
