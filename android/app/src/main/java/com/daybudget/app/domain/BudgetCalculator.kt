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

/** 月度のふりかえり */
data class PeriodRecap(
    val period: Period,
    val budget: Int,
    val spent: Int,
    /** 予算 − 支出（マイナスなら使いすぎ） */
    val saved: Int,
    val underDays: Int,
    val activeDays: Int,
    val longestStreak: Int,
    val topCategory: Category?,
    val topCategoryAmount: Int,
    val days: List<DaySummary>,
)

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
    /** 今日に予約してある出費（割当に含まれている） */
    val plannedToday: Int = 0,
    /** 明日以降に予約してある出費（割当から差し引かれている） */
    val plannedAhead: Int = 0,
)

object BudgetCalculator {

    /** 日ごとの重み（週末ブーストがオンなら週末だけ重い） */
    fun weightOf(settings: UserSettings, date: LocalDate): Long =
        if (settings.weekendBoostPct > 100 && date.dayOfWeek.value in settings.weekendDays) settings.weekendBoostPct.toLong() else 100L

    private fun weightSum(settings: UserSettings, from: LocalDate, to: LocalDate): Long {
        var sum = 0L
        var d = from
        while (!d.isAfter(to)) {
            sum += weightOf(settings, d)
            d = d.plusDays(1)
        }
        return sum
    }

    /** 貯金プールモードの、その日の固定の割当 */
    private fun fixedBudget(settings: UserSettings, period: Period, date: LocalDate): Int =
        Math.floorDiv(settings.monthlyBudget.toLong() * weightOf(settings, date), weightSum(settings, period.start, period.end)).toInt()

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
        planned: List<PlannedExpense> = emptyList(),
    ): List<DaySummary> {
        val totals = totalsByDate(expenses)
        val range = activeRange(period, settings.monthlyBudget, settings.startDate)
        val reserves = reservesByDate(planned, range.from, period.end)
        var remaining = range.budget
        return period.dates().map { date ->
            if (date.isBefore(range.from)) return@map DaySummary(date, 0, 0, DayState.INACTIVE)
            val spent = if (date.isAfter(today)) 0 else totals[date] ?: 0
            val budget = if (settings.carryoverMode == CarryoverMode.SAVINGS) {
                fixedBudget(settings, period, date)
            } else {
                distributeBudget(settings, remaining, date, period.end, reserves)
            }
            remaining -= spent
            val state = when {
                date.isAfter(today) -> DayState.FUTURE
                date == today -> DayState.TODAY
                spent <= budget -> DayState.UNDER
                else -> DayState.OVER
            }
            DaySummary(date, budget, spent, state)
        }
    }

    /**
     * 予約した出費を考慮した、その日の割当。
     * 先の日の予約はあらかじめ取り分けておき、予約日当日の割当にだけ上乗せする。
     */
    private fun distributeBudget(settings: UserSettings, remaining: Int, date: LocalDate, end: LocalDate, reserves: Map<LocalDate, Int>): Int {
        val onDay = reserves[date] ?: 0
        val ahead = reserves.filterKeys { it.isAfter(date) }.values.sum()
        val regular = (remaining - ahead - onDay).toLong()
        val share = Math.floorDiv(regular * weightOf(settings, date), weightSum(settings, date, end))
        return maxOf(0, share.toInt()) + onDay
    }

    private fun reservesByDate(planned: List<PlannedExpense>, from: LocalDate, to: LocalDate): Map<LocalDate, Int> =
        planned.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            .groupingBy { it.date }.fold(0) { acc, p -> acc + p.amount }

    /** 昨日までで、予算内に収まった日が何日続いているか（前の月度にもさかのぼる） */
    fun underBudgetStreak(settings: UserSettings, expenses: List<Expense>, today: LocalDate, planned: List<PlannedExpense> = emptyList()): Int {
        var streak = 0
        var period = Period.of(today, settings.closingDay)
        repeat(12) {
            val days = simulatePeriod(period, settings, expenses, today, planned)
                .filter { it.date.isBefore(today) && it.state != DayState.INACTIVE }
                .asReversed()
            for (d in days) {
                if (d.state == DayState.UNDER) streak++ else return streak
            }
            if (settings.startDate == null || !settings.startDate.isBefore(period.start)) return streak
            period = Period.of(period.start.minusDays(1), settings.closingDay)
        }
        return streak
    }

    /**
     * 昨日予算内で終えたおかげで、今日の割当が昨日より増えた額（均等配分モードのみ）。
     * 予約した出費の上乗せ分は除く。増えていなければ 0。
     */
    fun savingsBonus(settings: UserSettings, expenses: List<Expense>, today: LocalDate, planned: List<PlannedExpense> = emptyList()): Int {
        if (settings.carryoverMode != CarryoverMode.DISTRIBUTE) return 0
        val period = Period.of(today, settings.closingDay)
        val days = simulatePeriod(period, settings, expenses, today, planned)
        val yesterday = days.firstOrNull { it.date == today.minusDays(1) } ?: return 0
        if (yesterday.state != DayState.UNDER) return 0
        val todayDay = days.first { it.date == today }
        fun reserved(d: LocalDate) = planned.filter { it.date == d }.sumOf { it.amount }
        // 週末ブーストで割当が変わる分は除き、今日の重みに換算して比べる
        val todayRegular = (todayDay.budget - reserved(today)).toLong()
        val yesterdayRegular = (yesterday.budget - reserved(yesterday.date)).toLong() * weightOf(settings, today) / weightOf(settings, yesterday.date)
        return maxOf(0, (todayRegular - yesterdayRegular).toInt())
    }

    fun recap(period: Period, settings: UserSettings, expenses: List<Expense>, today: LocalDate, planned: List<PlannedExpense> = emptyList()): PeriodRecap {
        val range = activeRange(period, settings.monthlyBudget, settings.startDate)
        val days = simulatePeriod(period, settings, expenses, today, planned)
        val counted = days.filter { it.state == DayState.UNDER || it.state == DayState.OVER || it.state == DayState.TODAY }
        var longest = 0
        var run = 0
        counted.forEach { d -> if (d.spent <= d.budget) { run++; longest = maxOf(longest, run) } else run = 0 }
        val byCategory = expenses.filter { !it.date.isBefore(range.from) && !it.date.isAfter(period.end) }
            .filter { !it.isIncome }
            .groupingBy { it.categoryId }.fold(0) { acc, e -> acc + e.amount }
            .maxByOrNull { it.value }
        val spent = days.sumOf { it.spent }
        return PeriodRecap(
            period = period,
            budget = range.budget,
            spent = spent,
            saved = range.budget - spent,
            underDays = counted.count { it.spent <= it.budget },
            activeDays = counted.size,
            longestStreak = longest,
            topCategory = byCategory?.let { Categories.of(it.key) },
            topCategoryAmount = byCategory?.value ?: 0,
            days = days,
        )
    }

    /** 期間中のカテゴリ別の支出（多い順。収入・返金は含めない） */
    fun categoryTotals(expenses: List<Expense>, from: LocalDate, to: LocalDate): List<Pair<Category, Int>> =
        expenses.filter { !it.isIncome && !it.date.isBefore(from) && !it.date.isAfter(to) }
            .groupingBy { Categories.of(it.categoryId).id }.fold(0) { acc, e -> acc + e.amount }
            .map { (id, total) -> Categories.of(id) to total }
            .sortedByDescending { it.second }

    fun computeToday(
        settings: UserSettings,
        expenses: List<Expense>,
        today: LocalDate,
        planned: List<PlannedExpense> = emptyList(),
    ): TodaySnapshot {
        val period = Period.of(today, settings.closingDay)
        val range = activeRange(period, settings.monthlyBudget, settings.startDate)
        val inPeriod = expenses.filter { !it.date.isBefore(range.from) && !it.date.isAfter(today) }
        val periodSpent = inPeriod.sumOf { it.amount }
        val todaySpent = inPeriod.filter { it.date == today }.sumOf { it.amount }
        val remainingDays = daysBetween(today, period.end) + 1
        val dayNumber = daysBetween(range.from, today) + 1
        val remainingAtStart = range.budget - (periodSpent - todaySpent)
        val reserves = reservesByDate(planned, range.from, period.end)
        val plannedToday = reserves[today] ?: 0
        val plannedAhead = reserves.filterKeys { it.isAfter(today) }.values.sum()

        val dailyBudget: Int
        val tomorrowBudget: Int?
        var savingsAmount = 0
        if (settings.carryoverMode == CarryoverMode.SAVINGS) {
            dailyBudget = fixedBudget(settings, period, today)
            tomorrowBudget = if (remainingDays > 1) fixedBudget(settings, period, today.plusDays(1)) else null
            simulatePeriod(period, settings, expenses, today, planned)
                .filter { it.state == DayState.UNDER || it.state == DayState.OVER }
                .forEach { savingsAmount += maxOf(0, it.budget - it.spent) }
        } else {
            dailyBudget = distributeBudget(settings, remainingAtStart, today, period.end, reserves)
            tomorrowBudget = if (remainingDays > 1) distributeBudget(settings, remainingAtStart - todaySpent, today.plusDays(1), period.end, reserves) else null
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
            plannedToday = if (settings.carryoverMode == CarryoverMode.SAVINGS) 0 else plannedToday,
            plannedAhead = if (settings.carryoverMode == CarryoverMode.SAVINGS) 0 else plannedAhead,
        )
    }

}
