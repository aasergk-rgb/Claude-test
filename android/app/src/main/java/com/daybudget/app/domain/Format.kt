package com.daybudget.app.domain

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.JAPAN)
private val weekdays = arrayOf("月", "火", "水", "木", "金", "土", "日")

fun formatNumber(n: Int): String = (if (n < 0) "−" else "") + numberFormat.format(kotlin.math.abs(n.toLong()))

fun formatYen(n: Int): String = (if (n < 0) "−¥" else "¥") + numberFormat.format(kotlin.math.abs(n.toLong()))

fun formatSignedYen(n: Int): String = (if (n >= 0) "+" else "") + formatYen(n)

fun LocalDate.weekdayJa(): String = weekdays[dayOfWeek.value - 1]

/** 9/27 */
fun LocalDate.md(): String = "$monthValue/$dayOfMonth"

/** 9月27日(日) */
fun LocalDate.longJa(): String = "${monthValue}月${dayOfMonth}日(${weekdayJa()})"

fun closingLabel(day: Int): String = if (day >= CLOSING_END_OF_MONTH) "月末" else "${day}日"
