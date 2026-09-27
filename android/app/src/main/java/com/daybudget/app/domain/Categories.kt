package com.daybudget.app.domain

/** カテゴリ（名前・色・アイコン・並び順はユーザーが変えられる） */
data class Category(
    val id: String,
    val label: String,
    val color: Long,
    /** アイコンの種類（DbIcons.category で絵にする） */
    val icon: String,
    val order: Int = 0,
    val hidden: Boolean = false,
    /** 最初から入っているカテゴリ（削除はできず、非表示にできる） */
    val builtIn: Boolean = false,
)

/**
 * いま使えるカテゴリの一覧。データベースの内容で [update] される。
 * 支出の保存先には id だけを持つので、消したカテゴリの支出は「その他」として表示する。
 */
object Categories {
    val FOOD = Category("food", "食費", 0xFFE9803A, "food", 0, builtIn = true)
    val CAFE = Category("cafe", "カフェ・軽食", 0xFFA86B3C, "cafe", 1, builtIn = true)
    val DAILY = Category("daily", "日用品", 0xFF2F9A97, "daily", 2, builtIn = true)
    val TRANSPORT = Category("transport", "交通費", 0xFF4F6BD8, "transport", 3, builtIn = true)
    val FUN = Category("fun", "娯楽", 0xFFC454A8, "fun", 4, builtIn = true)
    val OTHER = Category("other", "その他", 0xFF7B8494, "other", 5, builtIn = true)

    /** 収入・返金（金額をマイナスで保存する特別なカテゴリ。一覧には出さない） */
    val INCOME = Category("income", "収入・返金", 0xFF11905C, "income", 999, builtIn = true)

    val DEFAULTS = listOf(FOOD, CAFE, DAILY, TRANSPORT, FUN, OTHER)

    /** 選べる色 */
    val PALETTE = listOf(0xFFE9803A, 0xFFA86B3C, 0xFF2F9A97, 0xFF4F6BD8, 0xFFC454A8, 0xFF7B8494, 0xFFD94A4A, 0xFFE0A21B, 0xFF3E9B4F, 0xFF7A5AD8, 0xFFE06A9A, 0xFF2D7FA8)

    /** 選べるアイコン */
    val ICONS = listOf("food", "cafe", "daily", "transport", "fun", "other", "heart", "gift", "book", "beauty", "health", "phone", "pet", "home", "drink", "shirt")

    @Volatile private var current: List<Category> = DEFAULTS

    fun update(list: List<Category>) {
        current = list.ifEmpty { DEFAULTS }
    }

    val all: List<Category> get() = current.sortedBy { it.order }

    /** 入力画面に並べるカテゴリ（非表示を除く） */
    val entries: List<Category> get() = all.filter { !it.hidden }

    fun of(id: String): Category =
        if (id == INCOME.id) INCOME else current.firstOrNull { it.id == id } ?: DEFAULTS.firstOrNull { it.id == id } ?: OTHER
}
