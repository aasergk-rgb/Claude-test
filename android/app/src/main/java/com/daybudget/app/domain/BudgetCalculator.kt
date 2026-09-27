package com.daybudget.app.domain

import java.time.LocalDate

enum class Status(val label: String, val short: String) {
    GREAT("超順調！", "超順調"),
    HEALTHY("計画通り", "計画通り"),
    WARNING("使いすぎ注意", "注意"),
    OVER("予算オーバー", "オーバー");

    companion object {
        /** 今日の予算に対する使用率で状態を決める（<50% / <80% / ≤100% / >100%） */
        fun of(spent: Int, budget: Int): Status {
            if (budget <= 0) return if (spent > 0) OVER else WARNING
            val r = spent.toDouble() / budget
            return when {
                r < 0.5 -> GREAT
                r < 0.8 -> HEALTHY
                r <= 1.0 -> WARNING
                else -> OVER
            }
        }
    }
}

enum class DayState { UNDER, OVER, TODAY, FUTURE, INACTIVE }

data class DaySummary(
    val date: LocalDate,
    /** その日の割当予算（未来日は「今日以降使わなかった場合」の見込み） */
    val budget: Int,
    val spent: Int,
    val state: DayState,
)

/** 実際に集計する範囲。月度の途中から使い始めた場合は予算を按分する */
data class ActiveRange(val from: LocalDate, val days: Int, val budget: Int)

data class TodaySnapshot(
    val period: Period,
    /** この月度に使える予算（途中開始なら按分済み） */
    val periodBudget: Int,
    /** 今日を含む残り日数 */
    val remainingDays: Int,
    /** 使い始めてから今日が何日目か（1始まり） */
    val dayNumber: Int,
    val dailyBudget: Int,
    val todaySpent: Int,
    val todayAvailable: Int,
    val progressRate: Double,
    val status: Status,
    /** 月度内の累計支出（今日を含む） */
    val periodSpent: Int,
    /** 月度の残予算（今日の支出を差し引いた後） */
    val periodRemaining: Int,
    /** 今日このあと使わなかった場合の明日の割当。最終日は null */
    val tomorrowBudget: Int?,
    /** 貯金プールモードで貯まった額（昨日まで） */
    val savingsAmount: Int,
    /** 経過日数ぶんの予算ペースと実績の差（プラスなら節約できている） */
    val pace: Int,
)

object BudgetCalculator {

    fun activeRange(period: Period, monthlyBudget: Int, startDate: LocalDate?): ActiveRange {
        if (startDate == null || !startDate.isAfter(period.start) || startDate.isAfter(period.end)) {
            return ActiveRange(period.start, period.days, monthlyBudget)
        }
        val days = daysBetween(startDate, period.end) + 1
        return ActiveRange(startDate, days, (monthlyBudget.toLong() * days / period.days).toInt())
    }

    fun totalsByDate(expenses: List<Expense>): Map<LocalDate, Int> =
        expenses.groupingBy { it.date }.fold(0) { acc, e -> acc + e.amount }

    /**
     * 月度内の各日の割当予算と実績を求める。
     * 均等配分モードでは割当が日ごとに変わるため、月度の初日から順に計算する。
     */
    fun simulatePeriod(
        period: Period,
        settings: UserSettings,
        expenses: List<Expense>,
        today: LocalDate,
    ): List<DaySummary> {
        val totals = totalsByDate(expenses)
        val range = activeRange(period, settings.monthlyBudget, settings.startDate)
        val fixed = settings.monthlyBudget / period.days
        var remaining = range.budget
        var left = range.days
        return period.dates().map { date ->
            if (date.isBefore(range.from)) return@map DaySummary(date, 0, 0, DayState.INACTIVE)
            val spent = if (date.isAfter(today)) 0 else totals[date] ?: 0
            val budget = if (settings.carryoverMode == CarryoverMode.SAVINGS) fixed else maxOf(0, floorDiv(remaining, left))
            remaining -= spent
            left -= 1
            val state = when {
                date.isAfter(today) -> DayState.FUTURE
                date == today -> DayState.TODAY
                spent <= budget -> DayState.UNDER
                else -> DayState.OVER
            }
            DaySummary(date, budget, spent, state)
        }
    }

    fun computeToday(settings: UserSettings, expenses: List<Expense>, today: LocalDate): TodaySnapshot {
        val period = Period.of(today, settings.closingDay)
        val range = activeRange(period, settings.monthlyBudget, settings.startDate)
        val inPeriod = expenses.filter { !it.date.isBefore(range.from) && !it.date.isAfter(today) }
        val periodSpent = inPeriod.sumOf { it.amount }
        val todaySpent = inPeriod.filter { it.date == today }.sumOf { it.amount }
        val remainingDays = daysBetween(today, period.end) + 1
        val dayNumber = daysBetween(range.from, today) + 1
        val remainingAtStart = range.budget - (periodSpent - todaySpent)

        val dailyBudget: Int
        val tomorrowBudget: Int?
        var savingsAmount = 0
        if (settings.carryoverMode == CarryoverMode.SAVINGS) {
            dailyBudget = settings.monthlyBudget / period.days
            tomorrowBudget = if (remainingDays > 1) dailyBudget else null
            simulatePeriod(period, settings, expenses, today)
                .filter { it.state == DayState.UNDER || it.state == DayState.OVER }
                .forEach { savingsAmount += maxOf(0, it.budget - it.spent) }
        } else {
            dailyBudget = maxOf(0, floorDiv(remainingAtStart, remainingDays))
            tomorrowBudget = if (remainingDays > 1) maxOf(0, floorDiv(remainingAtStart - todaySpent, remainingDays - 1)) else null
        }

        return TodaySnapshot(
            period = period,
            periodBudget = range.budget,
            remainingDays = remainingDays,
            dayNumber = dayNumber,
            dailyBudget = dailyBudget,
            todaySpent = todaySpent,
            todayAvailable = dailyBudget - todaySpent,
            progressRate = if (dailyBudget > 0) todaySpent.toDouble() / dailyBudget else if (todaySpent > 0) 1.0 else 0.0,
            status = Status.of(todaySpent, dailyBudget),
            periodSpent = periodSpent,
            periodRemaining = range.budget - periodSpent,
            tomorrowBudget = tomorrowBudget,
            savingsAmount = savingsAmount,
            pace = (range.budget.toLong() * dayNumber / range.days).toInt() - periodSpent,
        )
    }

    private fun floorDiv(a: Int, b: Int) = Math.floorDiv(a, b)
}
