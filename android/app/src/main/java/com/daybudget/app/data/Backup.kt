package com.daybudget.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * バックアップ（JSON）。ユーザーが選んだ保存先（端末・Google ドライブなど）に
 * 自分で書き出すだけで、アプリから外部には送信しない。
 */
object Backup {
    const val VERSION = 2

    class Contents(
        val settings: SettingsEntity,
        val expenses: List<ExpenseEntity>,
        val presets: List<PresetEntity>,
        val planned: List<PlannedEntity>,
        val categories: List<CategoryEntity> = emptyList(),
        val recurring: List<RecurringEntity> = emptyList(),
    )

    fun encode(
        settings: SettingsEntity?,
        expenses: List<ExpenseEntity>,
        presets: List<PresetEntity>,
        planned: List<PlannedEntity>,
        categories: List<CategoryEntity> = emptyList(),
        recurring: List<RecurringEntity> = emptyList(),
    ): String {
        val root = JSONObject()
            .put("app", "DayBudget")
            .put("version", VERSION)
            .put("settings", settings?.let(::settingsJson))
            .put("expenses", JSONArray(expenses.map { JSONObject().put("id", it.id).put("amount", it.amount).put("category_id", it.categoryId).put("memo", it.memo ?: JSONObject.NULL).put("date", it.date).put("created_at", it.createdAt).put("updated_at", it.updatedAt) }))
            .put("presets", JSONArray(presets.map { JSONObject().put("id", it.id).put("label", it.label).put("amount", it.amount).put("category_id", it.categoryId).put("order", it.order) }))
            .put("planned", JSONArray(planned.map { JSONObject().put("id", it.id).put("label", it.label).put("amount", it.amount).put("date", it.date) }))
            .put("categories", JSONArray(categories.map { JSONObject().put("id", it.id).put("name", it.name).put("icon", it.icon).put("color", it.color).put("order", it.order).put("hidden", it.hidden).put("built_in", it.builtIn) }))
            .put("recurring", JSONArray(recurring.map { JSONObject().put("id", it.id).put("label", it.label).put("amount", it.amount).put("day_of_month", it.dayOfMonth).put("category_id", it.categoryId).put("start_date", it.startDate).put("last_recorded", it.lastRecorded ?: JSONObject.NULL) }))
        return root.toString(2)
    }

    private fun settingsJson(s: SettingsEntity) = JSONObject()
        .put("monthly_budget", s.monthlyBudget).put("closing_day", s.closingDay).put("carryover_mode", s.carryoverMode)
        .put("theme", s.theme).put("widget_theme", s.widgetTheme).put("is_pro", s.isPro).put("onboarded", s.onboarded)
        .put("last_category_id", s.lastCategoryId).put("start_date", s.startDate ?: JSONObject.NULL)
        .put("created_at", s.createdAt).put("updated_at", s.updatedAt)
        .put("morning_notify", s.morningNotify).put("morning_time", s.morningTime)
        .put("evening_notify", s.eveningNotify).put("evening_time", s.eveningTime)
        .put("notify_prompt_dismissed", s.notifyPromptDismissed).put("last_recap_end", s.lastRecapEnd ?: JSONObject.NULL)
        .put("presets_seeded", s.presetsSeeded)
        .put("weekend_boost_pct", s.weekendBoostPct).put("weekend_days", s.weekendDays)
        .put("first_period_budget", s.firstPeriodBudget ?: JSONObject.NULL)

    /** 不正なファイルなら IllegalArgumentException */
    fun decode(json: String): Contents {
        val root = runCatching { JSONObject(json) }.getOrElse { throw IllegalArgumentException("DayBudget のバックアップファイルではありません") }
        require(root.optString("app") == "DayBudget") { "DayBudget のバックアップファイルではありません" }
        require(root.optInt("version") in 1..VERSION) { "このバージョンのアプリでは読み込めないバックアップです" }
        val s = root.getJSONObject("settings")
        fun JSONObject.str(k: String) = if (isNull(k)) null else getString(k)
        val settings = SettingsEntity(
            monthlyBudget = s.getInt("monthly_budget"), closingDay = s.getInt("closing_day"), carryoverMode = s.getString("carryover_mode"),
            theme = s.getString("theme"), widgetTheme = s.getString("widget_theme"), isPro = s.getBoolean("is_pro"), onboarded = s.getBoolean("onboarded"),
            lastCategoryId = s.getString("last_category_id"), startDate = s.str("start_date"),
            createdAt = s.getString("created_at"), updatedAt = s.getString("updated_at"),
            morningNotify = s.optBoolean("morning_notify"), morningTime = s.optInt("morning_time", 480),
            eveningNotify = s.optBoolean("evening_notify"), eveningTime = s.optInt("evening_time", 1260),
            notifyPromptDismissed = s.optBoolean("notify_prompt_dismissed"), lastRecapEnd = if (s.has("last_recap_end")) s.str("last_recap_end") else null,
            presetsSeeded = s.optBoolean("presets_seeded", true),
            weekendBoostPct = s.optInt("weekend_boost_pct", 100),
            weekendDays = s.optString("weekend_days", "6,7"),
            firstPeriodBudget = if (s.has("first_period_budget") && !s.isNull("first_period_budget")) s.getInt("first_period_budget") else null,
        )
        fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
        return Contents(
            settings = settings,
            expenses = root.getJSONArray("expenses").objects().map {
                ExpenseEntity(it.getString("id"), it.getInt("amount"), it.getString("category_id"), it.str("memo"), it.getString("date"), it.getString("created_at"), it.getString("updated_at"))
            },
            presets = root.optJSONArray("presets")?.objects()?.map {
                PresetEntity(it.getString("id"), it.getString("label"), it.getInt("amount"), it.getString("category_id"), it.getInt("order"))
            } ?: emptyList(),
            planned = root.optJSONArray("planned")?.objects()?.map {
                PlannedEntity(it.getString("id"), it.getString("label"), it.getInt("amount"), it.getString("date"))
            } ?: emptyList(),
            categories = root.optJSONArray("categories")?.objects()?.map {
                CategoryEntity(it.getString("id"), it.getString("name"), it.getString("icon"), it.getLong("color"), it.getInt("order"), it.optBoolean("hidden"), it.optBoolean("built_in"))
            } ?: emptyList(),
            recurring = root.optJSONArray("recurring")?.objects()?.map {
                RecurringEntity(it.getString("id"), it.getString("label"), it.getInt("amount"), it.getInt("day_of_month"), it.getString("category_id"), it.getString("start_date"), it.str("last_recorded"))
            } ?: emptyList(),
        )
    }
}
