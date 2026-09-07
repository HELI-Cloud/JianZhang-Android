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
    indices = [Index(value = ["date"])],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val note: String = "",
    val date: String,
    @ColumnInfo(name = "created_at") val createdAt: String = "",
)

/** 月度预算：categoryId 为 null 表示"月度总预算"（与桌面版一致） */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val month: String,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
)
