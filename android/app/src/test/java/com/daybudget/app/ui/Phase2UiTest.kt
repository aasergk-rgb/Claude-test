package com.daybudget.app.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daybudget.app.DayBudgetApp
import com.daybudget.app.MainActivity
import com.daybudget.app.data.AppDatabase
import com.daybudget.app.domain.OnboardingChoices
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class Phase2UiTest {
    @get:Rule val rule = createEmptyComposeRule()

    @After
    fun resetDatabase() = AppDatabase.resetForTests()

    private val app get() = ApplicationProvider.getApplicationContext<DayBudgetApp>()

    private fun waitFor(text: String) = rule.waitUntil(8_000) {
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_500)
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun incomeWeekendBoostCategoriesAndBreakdown() {
        val today = LocalDate.now()
        runBlocking {
            val repo = app.repository
            repo.completeOnboarding(OnboardingChoices(50_000, 31, emptyList(), morningNotify = false, eveningNotify = false), today.minusDays(3))
            repo.addExpense(1_200, "food", "ランチ", today.minusDays(2))
            repo.addExpense(600, "cafe", "カフェ", today.minusDays(1))
            repo.addExpense(2_400, "fun", "映画", today.minusDays(1))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("今日使えるお金")

            // 収入・返金を記録 → 金額はマイナスで保存され、緑の「+¥」で表示される
            rule.onNodeWithText("支出を記録").performClick()
            waitFor("収入・返金")
            rule.onNodeWithText("収入・返金").performClick()
            listOf("3", "0", "0", "0").forEach { rule.onNode(hasContentDescription(it)).performClick() }
            shot("20_income_sheet")
            rule.onNodeWithText("決定").performClick()
            waitFor("+¥3,000 を予算に足しました")
            val saved = runBlocking { app.repository.loadExpenses().single { it.date == today } }
            assertEquals(-3_000, saved.amount)
            assertEquals("income", saved.categoryId)
            shot("21_dashboard_income")

            // 設定: 週末ブーストと、カテゴリ編集
            rule.onNodeWithContentDescription("設定").performClick()
            waitFor("週末ブースト")
            rule.onNodeWithText("1.5倍").performScrollTo().performClick()
            waitFor("この月度なら 平日")
            assertEquals(150, runBlocking { app.repository.loadSettings()!!.weekendBoostPct })
            rule.onNodeWithText("週末ブースト").performScrollTo()
            shot("22_settings_weekend")
            rule.onNodeWithText("カテゴリを編集").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
            waitFor("タップで名前・色・アイコンを変えられます")
            shot("23_categories")
            rule.onNodeWithContentDescription("戻る").performClick()
            rule.onNodeWithContentDescription("戻る").performScrollTo().performClick()

            // 履歴: カテゴリ別の合計
            waitFor("今日使えるお金")
            rule.onNodeWithContentDescription("履歴").performClick()
            waitFor("カテゴリ別")
            shot("24_history_breakdown")
        }
    }
}
