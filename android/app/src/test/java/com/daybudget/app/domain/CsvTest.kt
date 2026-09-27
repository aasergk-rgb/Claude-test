package com.daybudget.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class CsvTest {
    @Test fun `日付順・BOM付き・カンマや引用符をエスケープ`() {
        val t = Instant.parse("2026-09-27T00:00:00Z")
        val csv = Csv.fromExpenses(
            listOf(
                Expense("b", 550, "cafe", "コーヒー, \"大\"", LocalDate.parse("2026-09-27"), t, t),
                Expense("a", 1200, "food", null, LocalDate.parse("2026-09-01"), t, t),
            ),
        )
        assertTrue(csv.startsWith("﻿日付,カテゴリ,金額,メモ\r\n"))
        assertTrue(csv.contains("2026-09-01,食費,1200,\r\n2026-09-27,カフェ・軽食,550,\"コーヒー, \"\"大\"\"\"\r\n"))
    }
}
