package com.daybudget.app.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MigrationAndBackupTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    // v1 の記録と設定は v2 に移行しても残る
    fun migrationKeepsV1Data() = runTest {
        helper.createDatabase("migrate.db", 1).apply {
            execSQL(
                "INSERT INTO user_settings (id, monthly_budget, closing_day, carryover_mode, theme, widget_theme, is_pro, onboarded, last_category_id, start_date, created_at, updated_at) " +
                    "VALUES (1, 50000, 31, 'distribute', 'system', 'dark', 0, 1, 'cafe', '2026-09-27', '2026-09-27T00:00:00Z', '2026-09-27T00:00:00Z')",
            )
            execSQL("INSERT INTO expenses VALUES ('e1', 550, 'cafe', 'コーヒー', '2026-09-27', '2026-09-27T00:00:00Z', '2026-09-27T00:00:00Z')")
            close()
        }
        helper.runMigrationsAndValidate("migrate.db", 2, true)

        val db = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, "migrate.db").allowMainThreadQueries().build()
        val settings = db.dao().getSettings()!!.toDomain()
        assertEquals(50_000, settings.monthlyBudget)
        assertEquals(LocalDate.parse("2026-09-27"), settings.startDate)
        assertFalse(settings.morningNotify)
        assertFalse(settings.presetsSeeded)
        assertEquals(1, db.dao().getExpenses().size)
        assertTrue(db.dao().getPresets().isEmpty())
        db.close()
    }

    @Test
    // 既存ユーザーにも「よく使う金額」の例が1回だけ入る
    fun presetsAreSeededOnlyOnce() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = BudgetRepository(db.dao())
        repo.completeOnboarding(50_000, 31, LocalDate.parse("2026-09-27"))
        assertEquals(3, repo.loadPresets().size)
        repo.loadPresets().forEach { repo.deletePreset(it.id) }
        repo.seedPresetsIfNeeded()
        assertTrue("全部消した人には入れ直さない", repo.loadPresets().isEmpty())

        repo.addPreset("ランチ", 800, "food")
        val added = repo.quickAdd(repo.loadPresets().single().id, LocalDate.parse("2026-09-27"))!!
        assertEquals(800, added.amount)
        assertEquals("ランチ", added.memo)
        db.close()
    }

    @Test
    // バックアップを書き出して読み込むと元どおりになる
    fun backupRoundTrip() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).allowMainThreadQueries().build()
        val repo = BudgetRepository(db.dao())
        repo.completeOnboarding(80_000, 25, LocalDate.parse("2026-09-27"))
        repo.addExpense(1200, "food", "ランチ, \"大盛り\"", LocalDate.parse("2026-09-27"))
        repo.addPlanned("飲み会", 5000, LocalDate.parse("2026-10-03"))
        val json = repo.exportBackup()

        repo.resetAll()
        assertEquals(null, repo.loadSettings())
        repo.importBackup(json)

        assertEquals(80_000, repo.loadSettings()!!.monthlyBudget)
        assertEquals(25, repo.loadSettings()!!.closingDay)
        assertEquals("ランチ, \"大盛り\"", repo.loadExpenses().single().memo)
        assertEquals("飲み会", repo.loadPlanned().single().label)
        assertEquals(3, repo.loadPresets().size)
        db.close()
    }

    @Test(expected = IllegalArgumentException::class)
    // ほかのファイルは読み込まない
    fun rejectsForeignFiles() {
        Backup.decode("""{"hello": "world"}""")
    }
}
