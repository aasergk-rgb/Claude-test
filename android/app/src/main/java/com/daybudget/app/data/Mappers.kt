package com.daybudget.app.data

import com.daybudget.app.domain.AppTheme
import com.daybudget.app.domain.CarryoverMode
import com.daybudget.app.domain.Expense
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
)

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
