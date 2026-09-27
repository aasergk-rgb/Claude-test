package com.daybudget.app.domain

object Csv {
    private fun cell(v: Any): String {
        val s = v.toString()
        return if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    /** Excel で文字化けしないよう BOM 付きで出力する */
    fun fromExpenses(expenses: List<Expense>): String {
        val rows = expenses
            .sortedWith(compareBy<Expense> { it.date }.thenBy { it.createdAt })
            .map { e -> listOf(e.date, Categories.of(e.categoryId).label, e.amount, e.memo ?: "").joinToString(",") { cell(it) } }
        return "\uFEFF" + (listOf("日付,カテゴリ,金額,メモ") + rows).joinToString("\r\n") + "\r\n"
    }
}
