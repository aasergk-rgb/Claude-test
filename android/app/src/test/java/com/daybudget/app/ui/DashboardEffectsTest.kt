package com.daybudget.app.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daybudget.app.DayBudgetApp
import com.daybudget.app.MainActivity
import com.daybudget.app.domain.OnboardingChoices
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** 節約ボーナス・連続日数のお祝い・スワイプ操作を、実際のアプリで確かめる */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class DashboardEffectsTest {
    @get:Rule val rule = createEmptyComposeRule()

    @After
    fun resetDatabase() = com.daybudget.app.data.AppDatabase.resetForTests()

    private val app get() = ApplicationProvider.getApplicationContext<DayBudgetApp>()

    private fun waitFor(text: String) = rule.waitUntil(8_000) {
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun todayCount() = runBlocking { app.repository.loadExpenses().count { it.date == LocalDate.now() } }

    @Test
    fun streakCelebrationBonusAndSwipe() {
        val today = LocalDate.now()
        runBlocking {
            val repo = app.repository
            // 10日前から使い始め、毎日少しだけ使って予算内に収めてきた人
            repo.completeOnboarding(OnboardingChoices(50_000, 31, emptyList(), morningNotify = false, eveningNotify = false), today.minusDays(10))
            for (i in 1..10) repo.addExpense(300, "cafe", "コーヒー", today.minusDays(i.toLong()))
            repo.addExpense(450, "food", "パン", today)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            // 10日連続 → 7日の節目のお祝い
            waitFor("7日連続で予算内！")
            rule.onRoot().captureRoboImage("build/outputs/roborazzi/10_milestone.png")
            rule.onNodeWithText("やったね").performClick()

            // 節約ボーナスのコインが数字に飛び込み、「昨日の節約で +¥」として残る
            waitFor("昨日の節約で +¥")
            rule.onRoot().captureRoboImage("build/outputs/roborazzi/11_bonus_landed.png")

            // 右にスワイプ → もう一度記録（今日の件数が増える）
            rule.onNodeWithText("今日の支出").assertExists()
            rule.onAllNodesWithText("· パン", substring = true)[0].performTouchInput { swipeRight() }
            waitFor("をもう一度記録しました")
            rule.waitUntil(5_000) { todayCount() >= 2 }
            // 1回のスワイプで1件だけ増える
            rule.mainClock.advanceTimeBy(1_000)
            assertEquals(2, todayCount())

            // 左にスワイプ → 削除
            rule.onAllNodesWithText("· パン", substring = true)[0].performTouchInput { swipeLeft() }
            waitFor("を削除しました")
            rule.waitUntil(5_000) { todayCount() == 1 }
            rule.onRoot().captureRoboImage("build/outputs/roborazzi/12_after_swipes.png")
        }
        assertEquals(1, todayCount())
    }
}
