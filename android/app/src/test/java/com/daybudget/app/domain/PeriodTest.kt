package com.daybudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun p(start: String, end: String) = Period(d(start), d(end))

    @Test fun `締め日25日 - 9月27日は9月26日〜10月25日（設計書の例）`() {
        assertEquals(p("2026-09-26", "2026-10-25"), Period.of(d("2026-09-27"), 25))
        assertEquals(30, Period.of(d("2026-09-27"), 25).days)
    }

    @Test fun `締め日25日 - 9月10日は8月26日〜9月25日（設計書の例）`() {
        assertEquals(p("2026-08-26", "2026-09-25"), Period.of(d("2026-09-10"), 25))
    }

    @Test fun `締め日当日はその月度の最終日`() {
        assertEquals(d("2026-09-25"), Period.of(d("2026-09-25"), 25).end)
        assertEquals(d("2026-09-26"), Period.of(d("2026-09-26"), 25).start)
    }

    @Test fun `月末締めはカレンダーどおり（閏年を含む）`() {
        assertEquals(p("2026-09-01", "2026-09-30"), Period.of(d("2026-09-27"), 31))
        assertEquals(p("2026-02-01", "2026-02-28"), Period.of(d("2026-02-10"), 31))
        assertEquals(p("2028-02-01", "2028-02-29"), Period.of(d("2028-02-29"), 31))
    }

    @Test fun `締め日30日 - 2月は末日に丸める`() {
        assertEquals(p("2026-03-01", "2026-03-30"), Period.of(d("2026-03-01"), 30))
        assertEquals(p("2026-01-31", "2026-02-28"), Period.of(d("2026-02-15"), 30))
        assertEquals(p("2026-03-31", "2026-04-30"), Period.of(d("2026-03-31"), 30))
    }

    @Test fun `年をまたぐ`() {
        assertEquals(p("2026-12-26", "2027-01-25"), Period.of(d("2026-12-28"), 25))
        assertEquals(p("2026-12-26", "2027-01-25"), Period.of(d("2027-01-05"), 25))
    }

    @Test fun `締め日1日`() {
        assertEquals(p("2026-08-02", "2026-09-01"), Period.of(d("2026-09-01"), 1))
        assertEquals(p("2026-09-02", "2026-10-01"), Period.of(d("2026-09-02"), 1))
    }

    @Test fun `月度の名前は終了日の月`() {
        assertEquals("10月度", Period.of(d("2026-09-27"), 25).name)
        assertEquals("9月度", Period.of(d("2026-09-27"), 31).name)
    }
}
