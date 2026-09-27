package com.daybudget.app.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daybudget.app.domain.Categories
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class Phase2DataTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    private fun db() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
    private fun d(s: String) = LocalDate.parse(s)

    // v2 のデータは v3 でも残り、週末ブーストはオフ、カテゴリは最初の6つが入る
    @Test
    fun migrateV2ToV3() = runTest {
        helper.createDatabase("m3.db", 2).apply {
            execSQL(
                "INSERT INTO user_settings (id, monthly_budget, closing_day, carryover_mode, theme, widget_theme, is_pro, onboarded, last_category_id, start_date, created_at, updated_at) " +
                    "VALUES (1, 40000, 25, 'distribute', 'dark', 'neon', 1, 1, 'food', '2026-09-01', 'x', 'x')",
            )
            execSQL("INSERT INTO expenses VALUES ('e1', 800, 'food', 'ランチ', '2026-09-20', '2026-09-20T00:00:00Z', '2026-09-20T00:00:00Z')")
            close()
        }
        helper.runMigrationsAndValidate("m3.db", 3, true)
        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, "m3.db").allowMainThreadQueries().build()
        val repo = BudgetRepository(db.dao())
        val s = repo.loadSettings()!!
        assertEquals(40_000, s.monthlyBudget)
        assertEquals(100, s.weekendBoostPct)
        assertEquals(setOf(6, 7), s.weekendDays)
        repo.ensureCategories()
        assertEquals(Categories.DEFAULTS.map { it.id }, repo.loadCategories().map { it.id })
        assertEquals(1, repo.loadExpenses().size)
        db.close()
    }

    // 決まった出費は支払日を迎えたら1回だけ記録される（何度呼んでも増えない）
    @Test
    fun recurringIsRecordedOnce() = runTest {
        val db = db()
        val repo = BudgetRepository(db.dao())
        repo.addRecurring("Netflix", 990, 25, "fun", today = d("2026-09-10"))
        assertEquals(0, repo.materializeRecurring(d("2026-09-24")))
        assertEquals(1, repo.materializeRecurring(d("2026-09-25")))
        assertEquals(0, repo.materializeRecurring(d("2026-09-25")))
        assertEquals(0, repo.materializeRecurring(d("2026-10-01")))
        // しばらく開かなくても、過ぎた分はまとめて記録される
        assertEquals(2, repo.materializeRecurring(d("2026-11-30")))
        val recorded = repo.loadExpenses().filter { it.memo == "Netflix" }.map { it.date }
        assertEquals(listOf(d("2026-09-25"), d("2026-10-25"), d("2026-11-25")), recorded)
        // 計算用の予約にも入る
        assertTrue(repo.loadAllPlanned(d("2026-12-01")).any { it.date == d("2026-12-25") && it.amount == 990 })
        db.close()
    }

    // 追加した日が支払日なら、その日の分は数えない（翌月から）
    @Test
    fun recurringStartsAfterToday() = runTest {
        val db = db()
        val repo = BudgetRepository(db.dao())
        repo.addRecurring("ジム", 3000, 10, "health", today = d("2026-09-10"))
        assertEquals(0, repo.materializeRecurring(d("2026-09-10")))
        assertEquals(1, repo.materializeRecurring(d("2026-10-10")))
        db.close()
    }

    @Test
    fun categoryEditing() = runTest {
        val db = db()
        val repo = BudgetRepository(db.dao())
        repo.ensureCategories()
        val oshi = repo.addCategory("推し活", "heart", 0xFFE06A9A)
        assertEquals("推し活", Categories.of(oshi.id).label)
        repo.moveCategory(oshi.id, up = true)
        assertEquals(oshi.id, repo.loadCategories().sortedBy { it.order }[5].id)
        repo.saveCategory(Categories.of("food").copy(label = "ごはん", hidden = true))
        assertEquals("ごはん", Categories.of("food").label)
        assertTrue(Categories.entries.none { it.id == "food" })
        // 最初からあるカテゴリは消せない、作ったものは消せる（記録は「その他」扱い）
        repo.deleteCategory("food")
        repo.deleteCategory(oshi.id)
        assertEquals(Categories.DEFAULTS.map { it.id }.toSet(), repo.loadCategories().map { it.id }.toSet())
        assertEquals("other", Categories.of(oshi.id).id)
        db.close()
    }

    @Test
    fun backupKeepsCategoriesAndRecurring() = runTest {
        val db = db()
        val repo = BudgetRepository(db.dao())
        repo.completeOnboarding(50_000, 31, d("2026-09-01"))
        repo.updateSettings { it.copy(weekendBoostPct = 150, weekendDays = setOf(5, 6, 7)) }
        repo.ensureCategories()
        repo.addCategory("推し活", "heart", 0xFFE06A9A)
        repo.addRecurring("Netflix", 990, 25, "fun", d("2026-09-01"))
        repo.addExpense(-3000, "income", "返金", d("2026-09-02"))
        val json = repo.exportBackup()
        repo.resetAll()
        repo.importBackup(json)
        assertEquals(150, repo.loadSettings()!!.weekendBoostPct)
        assertEquals(setOf(5, 6, 7), repo.loadSettings()!!.weekendDays)
        assertTrue(repo.loadCategories().any { it.label == "推し活" })
        assertEquals("Netflix", repo.loadRecurring().single().label)
        assertEquals(-3000, repo.loadExpenses().single().amount)
        db.close()
    }
}
