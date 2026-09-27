package com.daybudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class Phase2Test {
    private var n = 0
    private fun d(s: String) = LocalDate.parse(s)
    private fun ex(date: String, amount: Int, cat: String = "food") =
        Expense((++n).toString(), amount, cat, null, d(date), Instant.parse("${date}T03:00:00Z"), Instant.parse("${date}T03:00:00Z"))

    // 2026年9月: 1日は火曜。土日は 5,6,12,13,19,20,26,27 の8日、平日22日
    private val base = UserSettings(monthlyBudget = 30_000, closingDay = 31)
    private val boost = base.copy(weekendBoostPct = 150)

    @Test fun `週末ブースト - 週末は平日の1_5倍を割り当てる`() {
        // 重みの合計 = 22×100 + 8×150 = 3400
        val tue = BudgetCalculator.computeToday(boost, emptyList(), d("2026-09-01"))
        assertEquals(30_000L * 100 / 3400, tue.dailyBudget.toLong())
        val sat = BudgetCalculator.computeToday(boost, emptyList(), d("2026-09-05"))
        // 9/5 以降: 平日18日・週末8日 → 1800+1200=3000
        assertEquals(30_000L * 150 / 3000, sat.dailyBudget.toLong())
        assertEquals(DayOfWeek.SATURDAY, d("2026-09-05").dayOfWeek)
    }

    @Test fun `週末ブースト - 毎日割当どおり使うと予算を使い切る`() {
        val spent = mutableListOf<Expense>()
        for (day in Period.of(d("2026-09-01"), 31).dates()) {
            spent += ex(day.toString(), BudgetCalculator.computeToday(boost, spent, day).dailyBudget)
        }
        assertTrue(spent.sumOf { it.amount } in 29_970..30_000)
    }

    @Test fun `週末ブースト - 金曜も週末にできる、貯金プールにも効く`() {
        val fri = boost.copy(weekendDays = setOf(5, 6, 7))
        assertTrue(BudgetCalculator.computeToday(fri, emptyList(), d("2026-09-04")).dailyBudget > BudgetCalculator.computeToday(fri, emptyList(), d("2026-09-03")).dailyBudget)
        val savings = boost.copy(carryoverMode = CarryoverMode.SAVINGS)
        assertEquals(30_000L * 150 / 3400, BudgetCalculator.computeToday(savings, emptyList(), d("2026-09-05")).dailyBudget.toLong())
    }

    @Test fun `週末に割当が増えても節約ボーナスとは数えない`() {
        // 金曜(9/4)に割当ちょうど使い、土曜を迎える → ボーナスはほぼ 0
        val spent = mutableListOf<Expense>()
        for (day in listOf("2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04")) {
            spent += ex(day, BudgetCalculator.computeToday(boost, spent, d(day)).dailyBudget)
        }
        assertTrue(BudgetCalculator.savingsBonus(boost, spent, d("2026-09-05")) <= 2)
    }

    @Test fun `収入・返金はマイナスの支出として予算を増やす`() {
        val refund = ex("2026-09-10", -3_000, "income")
        val s = BudgetCalculator.computeToday(base, listOf(ex("2026-09-10", 500), refund), d("2026-09-10"))
        assertEquals(-2_500, s.todaySpent)
        assertEquals(s.dailyBudget + 2_500, s.todayAvailable)
        assertTrue(refund.isIncome)
        // 翌日以降にも回る
        val next = BudgetCalculator.computeToday(base, listOf(refund), d("2026-09-11"))
        assertEquals((30_000 + 3_000) / 20, next.dailyBudget)
    }

    @Test fun `決まった出費 - 月末日への丸めと開始日`() {
        val r = RecurringExpense("r", "家賃の一部", 1_000, 31, "daily", startDate = d("2026-09-10"))
        assertEquals(listOf(d("2026-09-30"), d("2026-10-31"), d("2026-11-30")), r.occurrences(d("2026-09-01"), d("2026-11-30")))
        val netflix = RecurringExpense("n", "Netflix", 990, 25, "fun", startDate = RecurringExpense.firstStart(d("2026-09-25")))
        assertEquals(listOf(d("2026-10-25")), netflix.occurrences(d("2026-09-01"), d("2026-10-31")))
        assertTrue(netflix.asPlanned(d("2026-10-01"), d("2026-10-31")).single().isRecurring)
    }

    @Test fun `決まった出費は予約として取り分けられる`() {
        val r = RecurringExpense("n", "Netflix", 990, 25, "fun", startDate = d("2026-09-01"))
        val planned = r.asPlanned(d("2026-09-01"), d("2026-09-30"))
        val s = BudgetCalculator.computeToday(base, emptyList(), d("2026-09-01"), planned)
        assertEquals((30_000 - 990) / 30, s.dailyBudget)
        assertEquals(990, s.plannedAhead)
    }

    @Test fun `カテゴリ別の合計（多い順・収入は除く・消したカテゴリはその他）`() {
        val totals = BudgetCalculator.categoryTotals(
            listOf(ex("2026-09-01", 500, "cafe"), ex("2026-09-02", 1_200, "food"), ex("2026-09-03", 300, "cafe"), ex("2026-09-03", -5_000, "income"), ex("2026-09-04", 100, "c_deleted")),
            d("2026-09-01"), d("2026-09-30"),
        )
        assertEquals(listOf("food" to 1_200, "cafe" to 800, "other" to 100), totals.map { it.first.id to it.second })
    }
}
