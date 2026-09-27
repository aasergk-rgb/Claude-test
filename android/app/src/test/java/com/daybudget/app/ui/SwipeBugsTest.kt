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
import com.daybudget.app.data.AppDatabase
import com.daybudget.app.domain.OnboardingChoices
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.math.abs

/** 報告されたスワイプの不具合（削除の「元に戻す」、もう一度記録の表示崩れ）の再発防止 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h851dp-xxhdpi")
class SwipeBugsTest {
    @get:Rule val rule = createEmptyComposeRule()

    @After
    fun resetDatabase() = AppDatabase.resetForTests()

    private val app get() = ApplicationProvider.getApplicationContext<DayBudgetApp>()
    private fun todayCount() = runBlocking { app.repository.loadExpenses().count { it.date == LocalDate.now() } }

    private fun waitFor(text: String) = rule.waitUntil(8_000) {
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun seed(vararg memos: String) {
        runBlocking {
            val repo = app.repository
            repo.completeOnboarding(OnboardingChoices(50_000, 31, emptyList(), morningNotify = false, eveningNotify = false), LocalDate.now())
            memos.forEach { repo.addExpense(300, "cafe", it, LocalDate.now()) }
        }
    }

    /** 行が左右にずれずに、ふつうの位置に表示されているか */
    private fun assertRowsAligned(text: String, expected: Int?) {
        val nodes = rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes()
        if (expected != null) assertEquals("「$text」の行の数", expected, nodes.size) else assertTrue(nodes.size >= 2)
        val lefts = nodes.map { it.boundsInRoot.left }
        val header = rule.onNodeWithText("今日の支出").fetchSemanticsNode().boundsInRoot.left
        lefts.forEach { assertTrue("行が横にずれている: $lefts / 見出し $header", abs(it - header) < 400f) }
        assertTrue("行どうしの位置がそろっていない: $lefts", lefts.max() - lefts.min() < 1f)
    }

    @Test
    fun undoAfterSwipeDeleteBringsTheRowBack() {
        seed("パン")
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("· パン")
            rule.onAllNodesWithText("· パン", substring = true)[0].performTouchInput { swipeLeft() }
            waitFor("を削除しました")
            rule.waitUntil(5_000) { todayCount() == 0 }

            rule.onNodeWithText("元に戻す").performClick()
            rule.waitUntil(5_000) { todayCount() == 1 }
            // 戻した直後にもう一度消えてしまわないか、しばらく待って確かめる
            rule.mainClock.advanceTimeBy(3_000)
            rule.waitForIdle()
            assertEquals(1, todayCount())
            waitFor("· パン")
            assertRowsAligned("· パン", 1)
            rule.onRoot().captureRoboImage("build/outputs/roborazzi/30_undo_after_swipe.png")
        }
    }

    @Test
    fun duplicateBySwipeShowsBothRowsProperly() {
        seed("パン")
        ActivityScenario.launch(MainActivity::class.java).use {
            waitFor("· パン")
            rule.onAllNodesWithText("· パン", substring = true)[0].performTouchInput { swipeRight() }
            waitFor("をもう一度記録しました")
            rule.waitUntil(5_000) { todayCount() == 2 }
            rule.mainClock.advanceTimeBy(2_000)
            rule.waitForIdle()
            rule.onRoot().captureRoboImage("build/outputs/roborazzi/31_after_duplicate.png")
            assertRowsAligned("· パン", 2)

            // 続けてもう一度スワイプしても崩れない
            rule.onAllNodesWithText("· パン", substring = true)[1].performTouchInput { swipeRight() }
            rule.waitUntil(5_000) { todayCount() >= 3 }
            rule.mainClock.advanceTimeBy(2_000)
            rule.waitForIdle()
            assertEquals(3, todayCount())
            // 3行目は画面の下に隠れるので、見えている行がずれていないことを確かめる
            assertRowsAligned("· パン", null)
        }
    }
}
