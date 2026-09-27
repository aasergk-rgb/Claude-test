package com.daybudget.app.data

import com.daybudget.app.domain.AppTheme
import com.daybudget.app.domain.CarryoverMode
import com.daybudget.app.domain.Category
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.RecurringExpense
import com.daybudget.app.domain.PlannedExpense
import com.daybudget.app.domain.QuickPreset
import com.daybudget.app.domain.UserSettings
import com.daybudget.app.domain.WidgetTheme
import java.time.Instant
import java.time.LocalDate

fun SettingsEntity.toDomain() = UserSettings(
    monthlyBudget = monthlyBudget,
    closingDay = closingDay,
    carryoverMode = CarryoverMode.fromKey(carryoverMode),
    theme = AppTheme.fromKey(theme),
    widgetTheme = WidgetTheme.fromKey(widgetTheme),
    isPro = isPro,
    onboarded = onboarded,
    lastCategoryId = lastCategoryId,
    startDate = startDate?.let(LocalDate::parse),
    morningNotify = morningNotify,
    morningTime = morningTime,
    eveningNotify = eveningNotify,
    eveningTime = eveningTime,
    notifyPromptDismissed = notifyPromptDismissed,
    lastRecapEnd = lastRecapEnd?.let(LocalDate::parse),
    presetsSeeded = presetsSeeded,
    weekendBoostPct = weekendBoostPct,
    weekendDays = weekendDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet(),
    firstPeriodBudget = firstPeriodBudget,
)

fun UserSettings.toEntity(createdAt: String, updatedAt: String) = SettingsEntity(
    monthlyBudget = monthlyBudget,
    closingDay = closingDay,
    carryoverMode = carryoverMode.key,
    theme = theme.key,
    widgetTheme = widgetTheme.key,
    isPro = isPro,
    onboarded = onboarded,
    lastCategoryId = lastCategoryId,
    startDate = startDate?.toString(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    morningNotify = morningNotify,
    morningTime = morningTime,
    eveningNotify = eveningNotify,
    eveningTime = eveningTime,
    notifyPromptDismissed = notifyPromptDismissed,
    lastRecapEnd = lastRecapEnd?.toString(),
    presetsSeeded = presetsSeeded,
    weekendBoostPct = weekendBoostPct,
    weekendDays = weekendDays.sorted().joinToString(","),
    firstPeriodBudget = firstPeriodBudget,
)

fun CategoryEntity.toDomain() = Category(id, name, color, icon, order, hidden, builtIn)

fun Category.toEntity() = CategoryEntity(id, label, icon, color, order, hidden, builtIn)

fun RecurringEntity.toDomain() = RecurringExpense(id, label, amount, dayOfMonth, categoryId, LocalDate.parse(startDate), lastRecorded?.let(LocalDate::parse))

fun RecurringExpense.toEntity() = RecurringEntity(id, label, amount, dayOfMonth, categoryId, startDate.toString(), lastRecorded?.toString())

fun PresetEntity.toDomain() = QuickPreset(id, label, amount, categoryId, order)

fun QuickPreset.toEntity() = PresetEntity(id, label, amount, categoryId, order)

fun PlannedEntity.toDomain() = PlannedExpense(id, label, amount, LocalDate.parse(date))

fun PlannedExpense.toEntity() = PlannedEntity(id, label, amount, date.toString())

fun ExpenseEntity.toDomain() = Expense(
    id = id,
    amount = amount,
    categoryId = categoryId,
    memo = memo,
    date = LocalDate.parse(date),
    createdAt = Instant.parse(createdAt),
    updatedAt = Instant.parse(updatedAt),
)

fun Expense.toEntity() = ExpenseEntity(
    id = id,
    amount = amount,
    categoryId = categoryId,
    memo = memo,
    date = date.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
