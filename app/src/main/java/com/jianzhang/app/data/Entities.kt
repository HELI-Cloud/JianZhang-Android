package com.jianzhang.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["type", "name"], unique = true)],
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val name: String,
    val icon: String = "more-horizontal",
    val sort: Int = 0,
    val builtin: Boolean = false,
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["date"]), Index(value = ["account_id"])],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "account_id") val accountId: Long? = null,
    val note: String = "",
    val date: String,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
    @ColumnInfo(name = "recurring_id") val recurringId: Long? = null,
)

/** 月度预算：categoryId 为 null 表示"月度总预算"（与桌面版一致） */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
)

@Entity(
    tableName = "accounts",
    indices = [Index(value = ["name"], unique = true)],
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String = "other",
    val icon: String = "wallet",
    val color: String = "#1fd0ff",
    @ColumnInfo(name = "initial_cents") val initialCents: Long = 0,
    val sort: Int = 0,
    val archived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["date"]),
        Index(value = ["from_account_id"]),
        Index(value = ["to_account_id"]),
    ],
)
data class Transfer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "from_account_id") val fromAccountId: Long,
    @ColumnInfo(name = "to_account_id") val toAccountId: Long,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "fee_cents") val feeCents: Long = 0,
    val note: String = "",
    val date: String,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: String = "#1fd0ff",
    val sort: Int = 0,
)

@Entity(
    tableName = "transaction_tags",
    primaryKeys = ["transaction_id", "tag_id"],
    indices = [Index(value = ["tag_id"])],
)
data class TransactionTag(
    @ColumnInfo(name = "transaction_id") val transactionId: Long,
    @ColumnInfo(name = "tag_id") val tagId: Long,
)

@Entity(
    tableName = "recurring_rules",
    indices = [Index(value = ["next_date"])],
)
data class RecurringRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "account_id") val accountId: Long? = null,
    val note: String = "",
    val frequency: String,
    @ColumnInfo(name = "interval_count") val intervalCount: Int = 1,
    val weekday: Int? = null,
    @ColumnInfo(name = "day_of_month") val dayOfMonth: Int? = null,
    @ColumnInfo(name = "month_of_year") val monthOfYear: Int? = null,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String? = null,
    @ColumnInfo(name = "next_date") val nextDate: String,
    @ColumnInfo(name = "auto_create") val autoCreate: Boolean = true,
    val enabled: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

@Entity(tableName = "reminder_dismissals")
data class ReminderDismissal(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "dismissed_at") val dismissedAt: String,
)
