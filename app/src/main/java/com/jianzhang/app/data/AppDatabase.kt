package com.jianzhang.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration SQL is kept explicit so a v1/v2 database can be upgraded without data loss.
 * The statements intentionally match Room's generated schema; extra CHECK/foreign-key
 * constraints would be accepted by SQLite but rejected by Room's schema validator.
 */
const val CREATE_BUDGETS_SQL: String =
    "CREATE TABLE IF NOT EXISTS `budgets` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`month` TEXT NOT NULL, " +
        "`category_id` INTEGER, " +
        "`amount_cents` INTEGER NOT NULL)"

const val CREATE_ACCOUNTS_SQL: String =
    "CREATE TABLE IF NOT EXISTS `accounts` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`name` TEXT NOT NULL, " +
        "`kind` TEXT NOT NULL, " +
        "`icon` TEXT NOT NULL, " +
        "`color` TEXT NOT NULL, " +
        "`initial_cents` INTEGER NOT NULL, " +
        "`sort` INTEGER NOT NULL, " +
        "`archived` INTEGER NOT NULL, " +
        "`created_at` TEXT NOT NULL)"

const val SEED_DEFAULT_ACCOUNT_SQL: String =
    "INSERT OR IGNORE INTO accounts (name, kind, icon, color, initial_cents, sort, archived, created_at) " +
        "VALUES ('默认账户', 'other', 'wallet', '#1fd0ff', 0, 0, 0, datetime('now', 'localtime'))"

const val CREATE_TRANSFERS_SQL: String =
    "CREATE TABLE IF NOT EXISTS `transfers` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`from_account_id` INTEGER NOT NULL, " +
        "`to_account_id` INTEGER NOT NULL, " +
        "`amount_cents` INTEGER NOT NULL, " +
        "`fee_cents` INTEGER NOT NULL, " +
        "`note` TEXT NOT NULL, " +
        "`date` TEXT NOT NULL, " +
        "`created_at` TEXT NOT NULL)"

const val CREATE_TAGS_SQL: String =
    "CREATE TABLE IF NOT EXISTS `tags` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`name` TEXT NOT NULL, " +
        "`color` TEXT NOT NULL, " +
        "`sort` INTEGER NOT NULL)"

const val CREATE_TRANSACTION_TAGS_SQL: String =
    "CREATE TABLE IF NOT EXISTS `transaction_tags` (" +
        "`transaction_id` INTEGER NOT NULL, " +
        "`tag_id` INTEGER NOT NULL, " +
        "PRIMARY KEY(`transaction_id`, `tag_id`))"

const val CREATE_RECURRING_RULES_SQL: String =
    "CREATE TABLE IF NOT EXISTS `recurring_rules` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`type` TEXT NOT NULL, " +
        "`amount_cents` INTEGER NOT NULL, " +
        "`category_id` INTEGER, " +
        "`account_id` INTEGER, " +
        "`note` TEXT NOT NULL, " +
        "`frequency` TEXT NOT NULL, " +
        "`interval_count` INTEGER NOT NULL, " +
        "`weekday` INTEGER, " +
        "`day_of_month` INTEGER, " +
        "`month_of_year` INTEGER, " +
        "`start_date` TEXT NOT NULL, " +
        "`end_date` TEXT, " +
        "`next_date` TEXT NOT NULL, " +
        "`auto_create` INTEGER NOT NULL, " +
        "`enabled` INTEGER NOT NULL, " +
        "`created_at` TEXT NOT NULL)"

const val CREATE_REMINDER_DISMISSALS_SQL: String =
    "CREATE TABLE IF NOT EXISTS `reminder_dismissals` (" +
        "`key` TEXT NOT NULL, " +
        "`dismissed_at` TEXT NOT NULL, " +
        "PRIMARY KEY(`key`))"

@Database(
    entities = [
        Category::class,
        Transaction::class,
        Budget::class,
        Account::class,
        Transfer::class,
        Tag::class,
        TransactionTag::class,
        RecurringRule::class,
        ReminderDismissal::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun accountDao(): AccountDao
    abstract fun transferDao(): TransferDao
    abstract fun tagDao(): TagDao
    abstract fun recurringDao(): RecurringDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        /** 1 -> 2：新增 budgets 表（v0.3.0 预算功能），保留已有数据。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_BUDGETS_SQL)
            }
        }

        /** 2 -> 3：账户、转账、标签、周期记账和提醒记录，旧账目归入默认账户。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(CREATE_ACCOUNTS_SQL)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounts_name` ON `accounts` (`name`)")
                db.execSQL(SEED_DEFAULT_ACCOUNT_SQL)
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `account_id` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `recurring_id` INTEGER")
                db.execSQL(
                    "UPDATE `transactions` SET `account_id` = " +
                        "(SELECT `id` FROM `accounts` WHERE `name` = '默认账户' LIMIT 1) " +
                        "WHERE `account_id` IS NULL",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_account_id` ON `transactions` (`account_id`)")

                db.execSQL(CREATE_TRANSFERS_SQL)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_date` ON `transfers` (`date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_from_account_id` ON `transfers` (`from_account_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transfers_to_account_id` ON `transfers` (`to_account_id`)")

                db.execSQL(CREATE_TAGS_SQL)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
                db.execSQL(CREATE_TRANSACTION_TAGS_SQL)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_tags_tag_id` ON `transaction_tags` (`tag_id`)")

                db.execSQL(CREATE_RECURRING_RULES_SQL)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_rules_next_date` ON `recurring_rules` (`next_date`)")
                db.execSQL(CREATE_REMINDER_DISMISSALS_SQL)
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { instance = it }
        }
    }
}
