package com.daybudget.app.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/** 端末内だけに保存する SQLite データベース。外部とは通信しない */
@Database(
    entities = [SettingsEntity::class, ExpenseEntity::class, PresetEntity::class, PlannedEntity::class, CategoryEntity::class, RecurringEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): BudgetDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** テスト用: テストごとに新しいデータベースを使う */
        @androidx.annotation.VisibleForTesting
        fun resetForTests() = synchronized(this) {
            instance?.close()
            instance = null
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "daybudget.db")
                .build()
                .also { instance = it }
        }
    }
}
