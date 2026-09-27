package com.daybudget.app.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** 締め日の「月末」を表す値。短い月では末日に丸める */
const val CLOSING_END_OF_MONTH = 31

/** 月度（開始日〜終了日、両端を含む） */
data class Period(val start: LocalDate, val end: LocalDate) {
    val days: Int get() = daysBetween(start, end) + 1

    operator fun contains(date: LocalDate) = !date.isBefore(start) && !date.isAfter(end)

    fun dates(): List<LocalDate> = (0 until days).map { start.plusDays(it.toLong()) }

    /** 月度の名前。終了日の月で呼ぶ（9/26〜10/25 → 10月度） */
    val name: String get() = "${end.monthValue}月度"

    companion object {
        /**
         * 締め日から、指定日が含まれる月度を求める。
         * 例: 締め日 25 → 9/27 は 9/26〜10/25、9/10 は 8/26〜9/25。
         */
        fun of(today: LocalDate, closingDay: Int): Period {
            val ym = YearMonth.from(today)
            val thisClose = closeDate(ym, closingDay)
            return if (!today.isAfter(thisClose)) {
                Period(closeDate(ym.minusMonths(1), closingDay).plusDays(1), thisClose)
            } else {
                Period(thisClose.plusDays(1), closeDate(ym.plusMonths(1), closingDay))
            }
        }

        private fun closeDate(ym: YearMonth, closingDay: Int): LocalDate =
            ym.atDay(minOf(closingDay, ym.lengthOfMonth()))
    }
}

/** b - a の日数 */
fun daysBetween(a: LocalDate, b: LocalDate): Int = ChronoUnit.DAYS.between(a, b).toInt()
