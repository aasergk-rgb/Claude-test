package com.daybudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BudgetCalculatorTest {
    private var n = 0
    private fun d(s: String) = LocalDate.parse(s)
    private fun ex(date: String, amount: Int, cat: String = "food") =
        Expense((++n).toString(), amount, cat, null, d(date), Instant.parse("${date}T03:00:00Z"), Instant.parse("${date}T03:00:00Z"))

    // 9/1〜9/26 に計 38,000 円、今日 9/27 に 550 円使った状態
    private val past = listOf(1200, 1850, 980, 2400, 3100, 0, 1520, 1340, 760, 2100, 1890, 2620, 650, 1100, 1480, 1720, 900, 2650, 1300, 0, 1150, 1640, 980, 2210, 1560, 900)
    private val sample = past.mapIndexedNotNull { i, a -> if (a > 0) ex("2026-09-%02d".format(i + 1), a) else null } +
        listOf(ex("2026-09-27", 330, "cafe"), ex("2026-09-27", 220, "daily"))

    private val distribute = UserSettings(monthlyBudget = 50_000, closingDay = 31)

    @Test fun `状態は使用率で4段階`() {
        assertEquals(Status.GREAT, Status.of(1499, 3000))
        assertEquals(Status.HEALTHY, Status.of(1500, 3000))
        assertEquals(Status.WARNING, Status.of(2400, 3000))
        assertEquals(Status.WARNING, Status.of(3000, 3000))
        assertEquals(Status.OVER, Status.of(3001, 3000))
        assertEquals(Status.WARNING, Status.of(0, 0))
        assertEquals(Status.OVER, Status.of(1, 0))
    }

    @Test fun `設計書の例 - 割当3000円・支出550円で残り2450円`() {
        val s = BudgetCalculator.computeToday(distribute, sample, d("2026-09-27"))
        assertEquals(3000, s.dailyBudget)
        assertEquals(550, s.todaySpent)
        assertEquals(2450, s.todayAvailable)
        assertEquals(0.183, s.progressRate, 0.001)
        assertEquals(Status.GREAT, s.status)
        assertEquals(4, s.remainingDays)
        assertEquals(11_450, s.periodRemaining)
        assertEquals(3816, s.tomorrowBudget)
        assertEquals(45_000 - 38_550, s.pace)
    }

    @Test fun `初日は予算÷日数（切り捨て）`() {
        assertEquals(1666, BudgetCalculator.computeToday(distribute, emptyList(), d("2026-09-01")).dailyBudget)
    }

    @Test fun `節約すると翌日以降が増え、使いすぎると減る`() {
        val spend = BudgetCalculator.computeToday(distribute, listOf(ex("2026-09-01", 5000)), d("2026-09-02"))
        assertEquals(45_000 / 29, spend.dailyBudget)
        assertEquals(50_000 / 29, BudgetCalculator.computeToday(distribute, emptyList(), d("2026-09-02")).dailyBudget)
    }

    @Test fun `予算を使い切ったら割当は0円で、超えた分はマイナス`() {
        val s = BudgetCalculator.computeToday(distribute, listOf(ex("2026-09-01", 60_000), ex("2026-09-10", 500)), d("2026-09-10"))
        assertEquals(0, s.dailyBudget)
        assertEquals(-500, s.todayAvailable)
        assertEquals(Status.OVER, s.status)
    }

    @Test fun `最終日は明日の見込みなし・月度外の支出は数えない`() {
        assertNull(BudgetCalculator.computeToday(distribute, emptyList(), d("2026-09-30")).tomorrowBudget)
        val s = BudgetCalculator.computeToday(distribute, listOf(ex("2026-08-31", 9999), ex("2026-10-01", 9999)), d("2026-09-27"))
        assertEquals(0, s.periodSpent)
    }

    @Test fun `各日の予算内・超過を判定し、今日の割当と一致する`() {
        val days = BudgetCalculator.simulatePeriod(Period.of(d("2026-09-27"), 31), distribute, sample, d("2026-09-27"))
        assertEquals(30, days.size)
        assertEquals(DaySummary(d("2026-09-01"), 1666, 1200, DayState.UNDER), days[0])
        assertEquals(DaySummary(d("2026-09-02"), 1682, 1850, DayState.OVER), days[1])
        assertEquals(DaySummary(d("2026-09-27"), 3000, 550, DayState.TODAY), days[26])
        assertEquals(DayState.FUTURE, days[27].state)
    }

    @Test fun `貯金プール - 割当は固定、余りが貯金になる（超過日は0）`() {
        val settings = UserSettings(monthlyBudget = 30_000, closingDay = 31, carryoverMode = CarryoverMode.SAVINGS)
        val expenses = listOf(ex("2026-09-01", 400), ex("2026-09-02", 1500), ex("2026-09-04", 200))
        val s = BudgetCalculator.computeToday(settings, expenses, d("2026-09-04"))
        assertEquals(1000, s.dailyBudget)
        assertEquals(800, s.todayAvailable)
        assertEquals(600 + 0 + 1000, s.savingsAmount)
        assertEquals(1000, s.tomorrowBudget)
    }

    @Test fun `月度の途中から始めたら最初の月度は按分`() {
        val settings = distribute.copy(startDate = d("2026-09-27"))
        val s = BudgetCalculator.computeToday(settings, listOf(ex("2026-09-10", 9999)), d("2026-09-27"))
        assertEquals(50_000 * 4 / 30, s.periodBudget)
        assertEquals(6666 / 4, s.dailyBudget)
        assertEquals(0, s.periodSpent)
        assertEquals(1, s.dayNumber)
        val days = BudgetCalculator.simulatePeriod(s.period, settings, emptyList(), d("2026-09-27"))
        assertEquals(DayState.INACTIVE, days[9].state)
        // 次の月度からは満額
        assertEquals(50_000, BudgetCalculator.computeToday(settings, emptyList(), d("2026-10-01")).periodBudget)
        // 月度の初日に始めたら按分しない
        assertEquals(50_000, BudgetCalculator.computeToday(distribute.copy(startDate = d("2026-09-01")), emptyList(), d("2026-09-01")).periodBudget)
    }
}
