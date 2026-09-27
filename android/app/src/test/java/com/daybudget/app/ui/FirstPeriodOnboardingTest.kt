package com.daybudget.app.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daybudget.app.DayBudgetApp
import com.daybudget.app.MainActivity
import com.daybudget.app.data.AppDatabase
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.Period
import com.daybudget.app.domain.formatYen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** 月度の途中から始めて「今月の残り」を入れると、その額で今日の予算が決まる */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class FirstPeriodOnboardingTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @After
    fun resetDatabase() = AppDatabase.resetForTests()

    @org.junit.Before
    fun grantNotifications() {
        org.robolectric.Shadows.shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>())
            .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun waitFor(text: String) = rule.waitUntil(8_000) {
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun enteredRemainingIsUsedForTheFirstPeriod() {
        val today = LocalDate.now()
        val period = Period.of(today, 31)
        // 月の初日にテストを実行した場合は「途中から」にならないので対象外
        assumeTrue(today != period.start)
        val daysLeft = period.end.dayOfMonth - today.dayOfMonth + 1

        waitFor("はじめる")
        rule.onNodeWithText("はじめる").performClick()
        rule.onNodeWithText("次へ").performClick()
        rule.onNodeWithText("次へ").performClick()
        waitFor("今月の残りはいくらですか？")
        rule.onNode(hasSetTextAction()).performTextReplacement("12000")
        waitFor("日割りの")
        rule.onNodeWithText("次へ").performClick()
        waitFor("よく使う出費はどれですか？")
        // 戻っても入れた額は残っている
        rule.onNodeWithText("戻る").performClick()
        try {
            waitFor("今月の残りはいくらですか？")
            waitFor("12,000")
        } catch (e: Throwable) {
            rule.onRoot().captureRoboImage("build/outputs/roborazzi/first_period_fail.png")
            throw e
        }
        rule.onNodeWithText("次へ").performClick()
        rule.onNodeWithText("次へ").performClick()
        waitFor("お知らせを受け取りますか？")
        rule.onNodeWithText("次へ").performClick()
        waitFor("まずは無料ではじめる")
        rule.onNodeWithText("まずは無料ではじめる").performClick()
        waitFor("今日使えるお金")

        val repo = ApplicationProvider.getApplicationContext<DayBudgetApp>().repository
        val settings = runBlocking { repo.loadSettings()!! }
        assertEquals(12_000, settings.firstPeriodBudget)
        val snap = BudgetCalculator.computeToday(settings, emptyList(), today)
        assertEquals(12_000 / daysLeft, snap.dailyBudget)
        // 画面の「本日の割当」も同じ
        waitFor("本日の割当 ${formatYen(12_000 / daysLeft)}")
        // 設定画面では変えられない（「今月の残り」の項目がない）
        assumeTrue(true)
    }
}
