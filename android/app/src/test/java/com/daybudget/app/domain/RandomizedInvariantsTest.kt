package com.daybudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.random.Random

/**
 * ランダムな使い方を大量に作り、計算がいつも食い違わないことを確かめる。
 * （失敗したら seed と条件がメッセージに出るので、そのまま再現できる）
 */
class RandomizedInvariantsTest {

    private data class Case(val today: LocalDate, val s: UserSettings, val e: List<Expense>, val p: List<PlannedExpense>)

    private fun randomCase(rnd: Random): Case {
        val today = LocalDate.of(2026, 1, 1).plusDays(rnd.nextLong(0, 900))
        val settings = UserSettings(
            monthlyBudget = rnd.nextInt(1_000, 300_000),
            closingDay = rnd.nextInt(1, 32),
            carryoverMode = if (rnd.nextInt(4) == 0) CarryoverMode.SAVINGS else CarryoverMode.DISTRIBUTE,
            weekendBoostPct = listOf(100, 125, 150, 200).random(rnd),
            weekendDays = listOf(setOf(6, 7), setOf(5, 6, 7), setOf(7)).random(rnd),
            startDate = if (rnd.nextBoolean()) today.minusDays(rnd.nextLong(0, 70)) else null,
            onboarded = true,
        )
        var n = 0
        val expenses = List(rnd.nextInt(0, 60)) {
            val d = today.minusDays(rnd.nextLong(0, 60))
            val amount = if (rnd.nextInt(10) == 0) -rnd.nextInt(1, 20_000) else rnd.nextInt(1, 15_000)
            Expense("${n++}", amount, listOf("food", "cafe", "fun", "income").random(rnd), null, d, Instant.EPOCH, Instant.EPOCH)
        }
        val planned = List(rnd.nextInt(0, 4)) {
            PlannedExpense("p${n++}", "予定", rnd.nextInt(100, 30_000), today.plusDays(rnd.nextLong(-20, 40)))
        }
        return Case(today, settings, expenses, planned)
    }

    @Test
    fun dashboardCalendarAndSummariesAgree() {
        val rnd = Random(20260927)
        repeat(3_000) { i ->
            val (today, s, e, p) = randomCase(rnd)
            val ctx = "case #$i settings=$s today=$today"
            val snap = BudgetCalculator.computeToday(s, e, today, p)
            val period = Period.of(today, s.closingDay)

            // 月度: 今日を含み、28〜31日
            assertTrue(ctx, today in period && snap.period == period && period.days in 28..31)
            // 次の月度とすき間なくつながる
            assertEquals(ctx, period.end.plusDays(1), Period.of(period.end.plusDays(1), s.closingDay).start)

            assertTrue("割当がマイナス $ctx", snap.dailyBudget >= 0)
            assertEquals(ctx, snap.dailyBudget - snap.todaySpent, snap.todayAvailable)
            assertEquals(ctx, snap.periodBudget - snap.periodSpent, snap.periodRemaining)

            // ダッシュボードの「今日の割当」と、カレンダーの今日の予算が同じ
            val days = BudgetCalculator.simulatePeriod(period, s, e, today, p)
            val todayCell = days.single { it.date == today }
            assertEquals("ダッシュボードとカレンダーが食い違う $ctx", snap.dailyBudget, todayCell.budget)
            assertEquals(ctx, snap.todaySpent, todayCell.spent)

            // 明日の見込みは、今日使った額が変わらない前提でのカレンダーの明日と同じ
            if (snap.tomorrowBudget != null && s.carryoverMode == CarryoverMode.DISTRIBUTE) {
                val tomorrow = BudgetCalculator.simulatePeriod(period, s, e, today.plusDays(1), p).single { it.date == today.plusDays(1) }
                val expensesTomorrow = e.filter { it.date == today.plusDays(1) }
                if (expensesTomorrow.isEmpty()) assertEquals("明日の見込みが食い違う $ctx", snap.tomorrowBudget, tomorrow.budget)
            }

            assertTrue(ctx, BudgetCalculator.savingsBonus(s, e, today, p) >= 0)
            val streak = BudgetCalculator.underBudgetStreak(s, e, today, p)
            assertTrue(ctx, streak >= 0)
            s.startDate?.let { assertTrue("連続日数が使い始めより長い $ctx", streak <= java.time.temporal.ChronoUnit.DAYS.between(it, today)) }

            val recap = BudgetCalculator.recap(period, s, e, today, p)
            assertTrue(ctx, recap.underDays in 0..recap.activeDays && recap.longestStreak <= recap.activeDays)
            assertEquals(ctx, recap.budget - recap.spent, recap.saved)

            val totals = BudgetCalculator.categoryTotals(e, period.start, period.end)
            assertTrue("カテゴリ別に収入が混ざっている $ctx", totals.none { it.first.id == "income" } && totals.all { it.second > 0 })
        }
    }

    @Test
    fun spendingExactlyTheAllowanceNeverOverspends() {
        val rnd = Random(7)
        repeat(300) { i ->
            val (_, s0, _, p) = randomCase(rnd)
            val s = s0.copy(carryoverMode = CarryoverMode.DISTRIBUTE, startDate = null)
            val period = Period.of(LocalDate.of(2026, 3, 15).plusDays(rnd.nextLong(0, 300)), s.closingDay)
            val spent = mutableListOf<Expense>()
            period.dates().forEach { d ->
                val b = BudgetCalculator.computeToday(s, spent, d, p).dailyBudget
                spent += Expense("$d", b, "food", null, d, Instant.EPOCH, Instant.EPOCH)
            }
            val total = spent.sumOf { it.amount }
            assertTrue("case #$i 予算を超えた total=$total settings=$s planned=$p", total <= s.monthlyBudget)
            assertTrue("case #$i 使い残しが多すぎる total=$total settings=$s", s.monthlyBudget - total <= period.days + 1)
        }
    }

    @Test
    fun recurringOccurrencesAreMonthlyAndInRange() {
        val rnd = Random(3)
        repeat(2_000) {
            val day = rnd.nextInt(1, 32)
            val start = LocalDate.of(2026, 1, 1).plusDays(rnd.nextLong(0, 400))
            val r = RecurringExpense("r", "x", 100, day, "fun", start)
            val from = start.minusDays(rnd.nextLong(0, 60))
            val to = from.plusDays(rnd.nextLong(0, 400))
            val occ = r.occurrences(from, to)
            assertTrue(occ.all { !it.isBefore(start) && !it.isBefore(from) && !it.isAfter(to) })
            assertTrue(occ.all { it.dayOfMonth == minOf(day, it.lengthOfMonth()) })
            assertEquals(occ.size, occ.map { java.time.YearMonth.from(it) }.distinct().size)
        }
    }
}
