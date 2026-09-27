package com.daybudget.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class AmountLabelTest {
    private fun e(amount: Int) = Expense("x", amount, "food", null, LocalDate.of(2026, 9, 27), Instant.EPOCH, Instant.EPOCH)

    @Test fun incomeIsShownWithPlus() = assertEquals("+¥200", e(-200).amountLabel())

    @Test fun expenseIsShownPlain() = assertEquals("¥1,500", e(1500).amountLabel())
}
