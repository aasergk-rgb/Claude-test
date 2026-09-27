package com.daybudget.app.ui

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daybudget.app.MainActivity
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 実際のアプリ（MainActivity・Room）を Robolectric 上で動かし、
 * 初回設定 → 支出の記録 → 履歴 → 設定 の流れを操作しながら各画面を撮影する。
 * 撮影: ./gradlew recordRoborazziDebug（画像は app/build/outputs/roborazzi/）
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class AppFlowScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String) {
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(1_000)
        rule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun waitFor(text: String) = rule.waitUntil(5_000) {
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun tapKey(k: String) = rule.onNode(hasContentDescription(k)).performClick()

    @Test
    fun onboardingToDashboardFlow() {
        waitFor("はじめる")
        shot("01_onboarding_intro")
        rule.onNodeWithText("はじめる").performClick()
        shot("02_onboarding_budget")
        rule.onNodeWithText("次へ").performClick()
        shot("03_onboarding_closing")
        rule.onNodeWithText("この設定ではじめる").performClick()

        waitFor("今日使えるお金")
        shot("04_dashboard_empty")

        rule.onNodeWithText("支出を記録").performClick()
        waitFor("決定")
        listOf("5", "5", "0").forEach(::tapKey)
        rule.onNodeWithText("カフェ・軽食").performClick()
        shot("05_expense_sheet")
        rule.onNodeWithText("決定").performClick()
        waitFor("今日の支出")
        rule.mainClock.advanceTimeBy(2_000)
        shot("06_dashboard_after_add")

        rule.onNodeWithContentDescription("履歴").performClick()
        waitFor("この日に支出を追加")
        shot("07_history")
        rule.onNodeWithContentDescription("戻る").performClick()

        rule.onNodeWithContentDescription("設定").performClick()
        waitFor("月の予算")
        shot("08_settings")
        rule.onAllNodesWithText("ダーク")[0].performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodes(hasText("ダーク") and androidx.compose.ui.test.isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("戻る").performClick()
        waitFor("今日使えるお金")
        shot("09_dashboard_dark")
    }
}
