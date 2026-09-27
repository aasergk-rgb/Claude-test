package com.daybudget.app.domain

enum class Categories(val id: String, val label: String, val color: Long) {
    FOOD("food", "食費", 0xFFE9803A),
    CAFE("cafe", "カフェ・軽食", 0xFFA86B3C),
    DAILY("daily", "日用品", 0xFF2F9A97),
    TRANSPORT("transport", "交通費", 0xFF4F6BD8),
    FUN("fun", "娯楽", 0xFFC454A8),
    OTHER("other", "その他", 0xFF7B8494);

    companion object {
        fun of(id: String): Categories = entries.firstOrNull { it.id == id } ?: OTHER
    }
}
