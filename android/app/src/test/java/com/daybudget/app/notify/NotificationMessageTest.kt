package com.daybudget.app.notify

import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.PlannedExpense
import com.daybudget.app.domain.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class NotificationMessageTest {
    private val today = LocalDate.parse("2026-09-10")
    private val s = UserSettings(monthlyBudget = 30_000, closingDay = 31, onboarded = true)
    private fun ex(date: String, amount: Int) = Expense(date + amount, amount, "food", null, LocalDate.parse(date), Instant.EPOCH, Instant.EPOCH)

    @Test fun `朝 - 昨日節約したら増えた額を伝える`() {
        val m = DailyNotifications.message(DailyNotifications.Kind.MORNING, s, listOf(ex("2026-09-09", 0)), emptyList(), today)
        assertEquals("今日は ¥1,428 使えます", m.title)
        // 昨日の割当 30000/22=1363 → 今日 30000/21=1428
        assertEquals("昨日の節約で +¥65 増えました", m.body)
    }

    @Test fun `朝 - 予定日は内容を伝える`() {
        val m = DailyNotifications.message(DailyNotifications.Kind.MORNING, s, emptyList(), listOf(PlannedExpense("p", "飲み会", 5_000, today)), today)
        assertEquals("今日は予定していた「飲み会」の日。¥5,000 を含んでいます", m.body)
    }

    @Test fun `夜 - 記録がなければ記録を促す、あれば残りと明日の見込み`() {
        assertEquals("今日の支出は記録しましたか？", DailyNotifications.message(DailyNotifications.Kind.EVENING, s, emptyList(), emptyList(), today).title)
        val m = DailyNotifications.message(DailyNotifications.Kind.EVENING, s, listOf(ex("2026-09-10", 400)), emptyList(), today)
        assertEquals("今日の残りは ¥1,028", m.title)
        assertEquals("このまま使わなければ、明日は ¥1,480", m.body)
    }

    @Test fun `次の通知時刻 - 過ぎていたら翌日`() {
        val zone = ZoneId.systemDefault()
        val at = { t: Long -> Instant.ofEpochMilli(t).atZone(zone).toLocalDateTime() }
        assertEquals(LocalDateTime.parse("2026-09-10T08:00"), at(DailyNotifications.nextTrigger(480, LocalDateTime.parse("2026-09-10T07:59"))))
        assertEquals(LocalDateTime.parse("2026-09-11T08:00"), at(DailyNotifications.nextTrigger(480, LocalDateTime.parse("2026-09-10T08:00"))))
    }
}
