package com.daybudget.app.widget

import com.daybudget.app.domain.CarryoverMode
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.Status
import com.daybudget.app.domain.UserSettings
import com.daybudget.app.domain.WidgetTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class WidgetDataTest {
    private val today = LocalDate.parse("2026-09-27")
    private fun ex(id: String, amount: Int, time: String, date: LocalDate = today) =
        Expense(id, amount, "cafe", "メモ$id", date, Instant.parse(time), Instant.parse(time))

    private val settings = UserSettings(monthlyBudget = 30_000, closingDay = 31, onboarded = true, widgetTheme = WidgetTheme.NEON)

    @Test fun `今日の値と直近2件（新しい順）を出す`() {
        val expenses = listOf(
            ex("a", 100, "2026-09-27T00:00:00Z"),
            ex("b", 200, "2026-09-27T01:00:00Z"),
            ex("c", 300, "2026-09-27T02:00:00Z"),
            ex("old", 999, "2026-09-26T02:00:00Z", today.minusDays(1)),
        )
        val d = WidgetData.from(settings, expenses, today)
        assertEquals(listOf("c", "b"), d.recent.map { it.id })
        assertEquals(d.dailyBudget - 600, d.todayAvailable)
        assertEquals(Status.GREAT, d.status)
    }

    @Test fun `着せ替えは Pro 版のみ（無料版はダーク固定）`() {
        assertEquals(WidgetTheme.DARK, WidgetData.from(settings, emptyList(), today).theme)
        assertEquals(WidgetTheme.NEON, WidgetData.from(settings.copy(isPro = true), emptyList(), today).theme)
    }

    @Test fun `貯金プールでは節約ペースの代わりに貯金額を出す`() {
        val d = WidgetData.from(settings.copy(carryoverMode = CarryoverMode.SAVINGS), listOf(ex("a", 400, "2026-09-26T00:00:00Z", today.minusDays(1))), today)
        assertEquals("今月の貯金", d.paceLabel)
        assertEquals(1000 - 400 + 25 * 1000, d.paceValue)
    }

    @Test fun `未設定なら設定を促す`() {
        assertFalse(WidgetData.from(null, emptyList(), today).onboarded)
    }
}
