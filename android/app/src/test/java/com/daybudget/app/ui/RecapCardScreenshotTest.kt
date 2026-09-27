package com.daybudget.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daybudget.app.domain.AppTheme
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.Period
import com.daybudget.app.domain.UserSettings
import com.daybudget.app.ui.screens.RecapCard
import com.daybudget.app.ui.screens.RecapStyle
import com.daybudget.app.ui.theme.DayBudgetTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class RecapCardScreenshotTest {
    @get:Rule val rule = createComposeRule()

    private val past = listOf(1200, 1850, 980, 2400, 3100, 0, 1520, 1340, 760, 2100, 1890, 2620, 650, 1100, 1480, 1720, 900, 2650, 1300, 0, 1150, 1640, 980, 2210, 1560, 900, 700, 1100, 800, 600)
    private val expenses = past.mapIndexedNotNull { i, a ->
        if (a == 0) null else Expense("$i", a, if (i % 3 == 0) "cafe" else "food", null, LocalDate.of(2026, 9, i + 1), Instant.EPOCH, Instant.EPOCH)
    }
    private val recap = BudgetCalculator.recap(Period.of(LocalDate.parse("2026-09-30"), 31), UserSettings(monthlyBudget = 50_000), expenses, LocalDate.parse("2026-10-01"))

    @Test
    fun cards() {
        var style by mutableStateOf(RecapStyle.NIGHT)
        var hide by mutableStateOf(false)
        rule.setContent {
            DayBudgetTheme(AppTheme.LIGHT) { Column(Modifier.padding(16.dp)) { RecapCard(recap, hide, style) } }
        }
        RecapStyle.entries.forEach { s ->
            listOf(false, true).forEach { h ->
                style = s
                hide = h
                rule.waitForIdle()
                rule.onRoot().captureRoboImage("build/outputs/roborazzi/recap_${s.name.lowercase()}${if (h) "_hidden" else ""}.png")
            }
        }
    }
}
