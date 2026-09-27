package com.daybudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** 月度の途中から始めて「今月の残り」を自分で入れた場合 */
class FirstPeriodBudgetTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun ex(date: String, amount: Int) = Expense(date + amount, amount, "food", null, d(date), Instant.EPOCH, Instant.EPOCH)

    // 9/27 に始めて、今月の残りは 12,000 円（9/27〜9/30 の4日）
    private val s = UserSettings(monthlyBudget = 50_000, closingDay = 31, startDate = d("2026-09-27"), firstPeriodBudget = 12_000)

    @Test fun `入れた残りを残り日数で割る`() {
        val snap = BudgetCalculator.computeToday(s, emptyList(), d("2026-09-27"))
        assertEquals(12_000, snap.periodBudget)
        assertEquals(3_000, snap.dailyBudget)
        assertEquals(12_000, snap.periodRemaining)
    }

    @Test fun `使えば翌日以降に反映される`() {
        val snap = BudgetCalculator.computeToday(s, listOf(ex("2026-09-27", 1_500)), d("2026-09-28"))
        assertEquals((12_000 - 1_500) / 3, snap.dailyBudget)
    }

    @Test fun `次の月度からは月の予算に戻る`() {
        assertEquals(50_000, BudgetCalculator.computeToday(s, emptyList(), d("2026-10-01")).periodBudget)
    }

    @Test fun `入れていなければ按分・月度の初日に始めたら使わない`() {
        assertEquals(50_000 * 4 / 30, BudgetCalculator.computeToday(s.copy(firstPeriodBudget = null), emptyList(), d("2026-09-27")).periodBudget)
        val fromFirstDay = s.copy(startDate = d("2026-09-01"))
        assertEquals(50_000, BudgetCalculator.computeToday(fromFirstDay, emptyList(), d("2026-09-01")).periodBudget)
    }

    @Test fun `残り0円でも落ちない`() {
        val snap = BudgetCalculator.computeToday(s.copy(firstPeriodBudget = 0), listOf(ex("2026-09-27", 300)), d("2026-09-27"))
        assertEquals(0, snap.dailyBudget)
        assertEquals(-300, snap.todayAvailable)
    }

    @Test fun `振り返りとカレンダーも同じ予算を使う`() {
        val recap = BudgetCalculator.recap(Period.of(d("2026-09-27"), 31), s, emptyList(), d("2026-10-01"))
        assertEquals(12_000, recap.budget)
        val days = BudgetCalculator.simulatePeriod(Period.of(d("2026-09-27"), 31), s, emptyList(), d("2026-09-27"))
        assertEquals(3_000, days.single { it.date == d("2026-09-27") }.budget)
    }
}
