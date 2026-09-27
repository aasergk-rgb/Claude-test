package com.daybudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class GrowthFeaturesTest {
    private var n = 0
    private fun d(s: String) = LocalDate.parse(s)
    private fun ex(date: String, amount: Int, cat: String = "food") =
        Expense((++n).toString(), amount, cat, null, d(date), Instant.parse("${date}T03:00:00Z"), Instant.parse("${date}T03:00:00Z"))
    private fun plan(date: String, amount: Int) = PlannedExpense((++n).toString(), "予定", amount, d(date))

    private val s = UserSettings(monthlyBudget = 30_000, closingDay = 31)

    @Test fun `予約した出費は先に取り分け、当日の割当に上乗せする`() {
        val planned = listOf(plan("2026-09-11", 5_000))
        // 9/1: (30000 - 5000) / 30 = 833
        val first = BudgetCalculator.computeToday(s, emptyList(), d("2026-09-01"), planned)
        assertEquals(833, first.dailyBudget)
        assertEquals(5_000, first.plannedAhead)
        // 予約日当日: 通常分 + 5000
        val onDay = BudgetCalculator.computeToday(s, emptyList(), d("2026-09-11"), planned)
        assertEquals((30_000 - 5_000) / 20 + 5_000, onDay.dailyBudget)
        assertEquals(5_000, onDay.plannedToday)
        // 前日から見た明日の見込みにも上乗せされる
        val before = BudgetCalculator.computeToday(s, emptyList(), d("2026-09-10"), planned)
        assertEquals((30_000 - 5_000) / 20 + 5_000, before.tomorrowBudget)
    }

    @Test fun `予約日を過ぎたら通常に戻る・予算合計は変わらない`() {
        val planned = listOf(plan("2026-09-11", 5_000))
        val expenses = listOf(ex("2026-09-11", 5_000))
        val after = BudgetCalculator.computeToday(s, expenses, d("2026-09-12"), planned)
        assertEquals(0, after.plannedAhead)
        assertEquals((30_000 - 5_000) / 19, after.dailyBudget)
        // 毎日ちょうど割当どおりに使うと、予算をぴったり（切り捨て分だけ残して）使い切る
        val spentDaily = mutableListOf<Expense>()
        var onPlannedDay = 0
        for (day in Period.of(d("2026-09-01"), 31).dates()) {
            val budget = BudgetCalculator.computeToday(s, spentDaily, day, planned).dailyBudget
            if (day == d("2026-09-11")) onPlannedDay = budget
            spentDaily += ex(day.toString(), budget)
        }
        val total = spentDaily.sumOf { it.amount }
        assert(total in 29_970..30_000) { "total=$total" }
        assert(onPlannedDay >= 5_000)
    }

    @Test fun `貯金プールでは予約を使わない`() {
        val savings = s.copy(carryoverMode = CarryoverMode.SAVINGS)
        val snap = BudgetCalculator.computeToday(savings, emptyList(), d("2026-09-01"), listOf(plan("2026-09-11", 5_000)))
        assertEquals(1_000, snap.dailyBudget)
        assertEquals(0, snap.plannedAhead)
    }

    @Test fun `予算内の連続日数は昨日までを数え、前の月度にもさかのぼる`() {
        val settings = s.copy(startDate = d("2026-08-25"))
        // 8/31 に使いすぎ、9/1〜9/3 は予算内
        val expenses = listOf(ex("2026-08-31", 9_999), ex("2026-09-01", 100), ex("2026-09-02", 100))
        assertEquals(3, BudgetCalculator.underBudgetStreak(settings, expenses, d("2026-09-04")))
        // 8/31 まで遡ってもストップ
        assertEquals(0, BudgetCalculator.underBudgetStreak(settings, expenses, d("2026-09-01")))
        // 使いすぎがなければ使い始めた日まで
        assertEquals(10, BudgetCalculator.underBudgetStreak(settings, emptyList(), d("2026-09-04")))
    }

    @Test fun `ふりかえり - 節約額・予算内の日数・最長連続・よく使ったカテゴリ`() {
        val expenses = listOf(
            ex("2026-09-01", 500, "cafe"), ex("2026-09-02", 5_000, "fun"), ex("2026-09-03", 300, "cafe"),
            ex("2026-09-03", 400, "cafe"),
        )
        val r = BudgetCalculator.recap(Period.of(d("2026-09-10"), 31), s, expenses, d("2026-10-01"))
        assertEquals(30_000, r.budget)
        assertEquals(6_200, r.spent)
        assertEquals(23_800, r.saved)
        assertEquals(30, r.activeDays)
        assertEquals(29, r.underDays)
        assertEquals(28, r.longestStreak)
        assertEquals(Categories.FUN, r.topCategory)
        assertEquals(5_000, r.topCategoryAmount)
    }

    @Test fun `記録がない月度のふりかえり`() {
        val r = BudgetCalculator.recap(Period.of(d("2026-09-10"), 31), s, emptyList(), d("2026-10-01"))
        assertNull(r.topCategory)
        assertEquals(30_000, r.saved)
    }

    @Test fun `昨日予算内なら、今日増えた額が節約ボーナス`() {
        // 9/1 に 0 円 → 9/2 は 30000/29 = 1034、9/1 は 1000 → +34
        assertEquals(34, BudgetCalculator.savingsBonus(s, emptyList(), d("2026-09-02")))
        // 昨日使いすぎたらボーナスなし
        assertEquals(0, BudgetCalculator.savingsBonus(s, listOf(ex("2026-09-01", 2_000)), d("2026-09-02")))
        // 月度の初日は昨日がないのでなし
        assertEquals(0, BudgetCalculator.savingsBonus(s, emptyList(), d("2026-09-01")))
        // 予約の上乗せはボーナスに数えない
        // 9/1: (30000-3000)/30 = 900、9/2 の通常分: 27000/29 = 931 → +31（3000 は含めない）
        assertEquals(31, BudgetCalculator.savingsBonus(s, emptyList(), d("2026-09-02"), listOf(plan("2026-09-02", 3_000))))
        // 貯金プールではなし
        assertEquals(0, BudgetCalculator.savingsBonus(s.copy(carryoverMode = CarryoverMode.SAVINGS), emptyList(), d("2026-09-02")))
    }

    @Test fun `連続日数の節目`() {
        assertNull(reachedMilestone(2))
        assertEquals(3, reachedMilestone(3))
        assertEquals(7, reachedMilestone(9))
        assertEquals(100, reachedMilestone(150))
    }
}
