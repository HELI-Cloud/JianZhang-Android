package com.jianzhang.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


/**
 * budgets 表创建 SQL，与 Room 期望 schema 完全一致（v1→v2 升级时 Room 会校验，
 * 多外键/CHECK、少 NOT NULL 都会抛 "Migration didn't properly handle" 导致闪退）。
 */
const val CREATE_BUDGETS_SQL: String =
    "CREATE TABLE IF NOT EXISTS `budgets` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`month` TEXT NOT NULL, " +
        "`category_id` INTEGER, " +
        "`amount_cents` INTEGER NOT NULL)"

@Database(
    entities = [Category::class, Transaction::class, Budget::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        /** 1 -> 2：新增 budgets 表（v0.3.0 预算功能），保留已有数据 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_BUDGETS_SQL)
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "jianzhang.db",
            )
                .addCallback(SeedCallback())
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                .also { instance = it }
        }
    }
}
