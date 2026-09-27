package com.daybudget.app.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.daybudget.app.DayBudgetApp
import com.daybudget.app.MainActivity
import com.daybudget.app.data.AppDatabase
import com.daybudget.app.domain.DEFAULT_PRESETS
import com.daybudget.app.domain.OnboardingChoices
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.random.Random

/**
 * 実際のアプリでランダムな操作を続け、毎回「落ちない・画面の件数とデータが合う・行がずれない」を確かめる。
 * 失敗したら seed と直前の操作がメッセージに出る。
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class MonkeyTest {
    @get:Rule val rule = createEmptyComposeRule()

    @After
    fun resetDatabase() = AppDatabase.resetForTests()

    private val app get() = ApplicationProvider.getApplicationContext<DayBudgetApp>()
    private fun todayCount() = runBlocking { app.repository.loadExpenses().count { it.date == LocalDate.now() } }
    private fun has(text: String) = rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    private fun settle() {
        rule.mainClock.advanceTimeBy(900)
        rule.waitForIdle()
    }

    private fun SemanticsNodeInteractionCollection.count() = fetchSemanticsNodes().size

    @Test fun monkeySeedA() = monkey(seed = 1, steps = 45)

    @Test fun monkeySeedB() = monkey(seed = 42, steps = 45)

    @Test fun monkeySeedC() = monkey(seed = 7, steps = 45)

    @Test fun monkeySeedD() = monkey(seed = 2026, steps = 45)

    private fun monkey(seed: Int, steps: Int) {
        runBlocking {
            app.repository.completeOnboarding(OnboardingChoices(60_000, 31, DEFAULT_PRESETS, morningNotify = false, eveningNotify = false), LocalDate.now().minusDays(2))
        }
        val rnd = Random(seed)
        val log = mutableListOf<String>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            rule.waitUntil(8_000) { has("今日使えるお金") }
            fun back() = scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            fun toDashboard() {
                repeat(3) { if (!has("今日使えるお金")) { back(); settle() } }
            }

            repeat(steps) { step ->
                toDashboard()
                val action = rnd.nextInt(9)
                log += "$step:$action"
                val rows = rule.onAllNodesWithContentDescription("削除")
                // 画面に見えている行だけを操作する
                val visible = rows.fetchSemanticsNodes().indexOfFirst { it.boundsInRoot.width > 0f }
                when (action) {
                    0 -> { // 支出を記録
                        rule.onNodeWithText("支出を記録").performClick(); settle()
                        repeat(rnd.nextInt(1, 5)) { rule.onNode(hasContentDescription("${rnd.nextInt(1, 10)}")).performClick() }
                        rule.onNodeWithText("決定").performClick()
                    }
                    1 -> { // 収入・返金
                        rule.onNodeWithText("支出を記録").performClick(); settle()
                        rule.onNode(hasText("収入・返金") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)).performClick()
                        rule.onNode(hasContentDescription("${rnd.nextInt(1, 10)}")).performClick()
                        rule.onNode(hasContentDescription("00")).performClick()
                        rule.onNodeWithText("決定").performClick()
                    }
                    2 -> if (has("コーヒー ¥150")) rule.onNodeWithText("コーヒー ¥150").performClick()
                    3 -> if (visible >= 0) { // 左スワイプで削除（半分は元に戻す）
                        rows[visible].performTouchInput { swipeLeft(startX = right - 5f, endX = left) }
                        settle()
                        if (rnd.nextBoolean() && has("元に戻す")) rule.onNodeWithText("元に戻す").performClick()
                    }
                    4 -> if (visible >= 0) { // 右スワイプでもう一度記録（半分は元に戻す）
                        rows[visible].performTouchInput { swipeRight(startX = left + 5f, endX = right) }
                        settle()
                        if (rnd.nextBoolean() && has("元に戻す")) rule.onNodeWithText("元に戻す").performClick()
                    }
                    5 -> if (visible >= 0) { // ゴミ箱ボタン（半分は元に戻す）
                        rows[visible].performClick()
                        settle()
                        if (rnd.nextBoolean() && has("元に戻す")) rule.onNodeWithText("元に戻す").performClick()
                    }
                    6 -> { // 履歴を開いて戻る
                        rule.onNodeWithContentDescription("履歴").performClick(); settle()
                        assertTrue("履歴が開かない $log", has("累計支出"))
                        back()
                    }
                    7 -> { // テーマを切り替えて戻る
                        rule.onNodeWithContentDescription("設定").performClick(); settle()
                        val theme = listOf("自動", "ライト").random(rnd)
                        rule.onAllNodesWithText(theme)[0].performClick()
                        back()
                    }
                    else -> { // シートを開いて閉じるだけ
                        rule.onNodeWithText("支出を記録").performClick(); settle()
                        rule.onNodeWithContentDescription("閉じる").performClick()
                    }
                }
                settle()
                rule.mainClock.advanceTimeBy(1_500)
                rule.waitForIdle()
                toDashboard()

                // 画面の「今日の支出 N件」とデータが一致する
                // DB の書き込みは別スレッドで進むので、待つ間は毎回データを読み直す
                var n = todayCount()
                try {
                    rule.waitUntil(5_000) { n = todayCount(); has("${n}件") }
                } catch (e: Throwable) {
                    rule.onRoot().captureRoboImage("build/outputs/roborazzi/monkey_fail_seed$seed.png")
                    val rowsInDb = runBlocking { app.repository.loadExpenses() }.map { "${it.date} ${it.amount} ${it.categoryId} ${it.memo} ${it.createdAt}" }
                    throw AssertionError("画面の件数がデータ($n 件)と合わない / DB $rowsInDb / 今日 ${LocalDate.now()} / 操作 $log", e)
                }
                // 見えている行が横にずれていない（ゴミ箱ボタンの位置がそろっている）
                val lefts = rule.onAllNodesWithContentDescription("削除").fetchSemanticsNodes()
                    .map { it.boundsInRoot }.filter { it.width > 0f && it.height > 0f }.map { it.left }
                assertTrue("行がずれている $lefts / 操作 $log", lefts.isEmpty() || lefts.max() - lefts.min() < 1f)
            }
        }
        println("MONKEY seed=$seed steps=$steps ok, today=${todayCount()} log=$log")
    }
}
