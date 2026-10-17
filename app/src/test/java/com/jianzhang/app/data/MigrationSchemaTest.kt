package com.jianzhang.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 回归测试：手写迁移 SQL 必须与 Room 导出的期望 schema 完全一致，
 * 否则老版本用户升级后首次读库会抛 "Migration didn't properly handle" 而闪退。
 */
class MigrationSchemaTest {

    private fun normalize(sql: String, tableName: String): String =
        sql.replace("`", "")
            .replace("\${TABLE_NAME}", tableName)
            .filter { !it.isWhitespace() }
            .uppercase()

    /** 从 Room 导出的 schema JSON 中提取指定表的 createSql */
    private fun createSqlFromSchema(tableName: String): String {
        val dir = File("schemas/com.jianzhang.app.data.AppDatabase")
        val file = dir.listFiles()?.maxByOrNull { it.name.removeSuffix(".json").toIntOrNull() ?: 0 }
            ?: error("schema 目录不存在：${dir.absolutePath}")
        val json = file.readText()
        val tableIdx = json.indexOf("\"tableName\": \"$tableName\"")
        assertTrue("schema 中找不到表 $tableName", tableIdx >= 0)
        val createIdx = json.indexOf("\"createSql\": \"", tableIdx)
        assertTrue("schema 中找不到 createSql", createIdx >= 0)
        val start = createIdx + "\"createSql\": \"".length
        val end = json.indexOf("\"", start)
        return json.substring(start, end)
    }

    @Test
    fun migrationCreatesExactRoomSchema() {
        val tables = mapOf(
            "budgets" to CREATE_BUDGETS_SQL,
            "accounts" to CREATE_ACCOUNTS_SQL,
            "transfers" to CREATE_TRANSFERS_SQL,
            "tags" to CREATE_TAGS_SQL,
            "transaction_tags" to CREATE_TRANSACTION_TAGS_SQL,
            "recurring_rules" to CREATE_RECURRING_RULES_SQL,
            "reminder_dismissals" to CREATE_REMINDER_DISMISSALS_SQL,
        )
        tables.forEach { (table, sql) ->
            assertEquals(
                "迁移 SQL 与 Room schema 不一致：$table",
                normalize(createSqlFromSchema(table), table),
                normalize(sql, table),
            )
        }
    }

    @Test
    fun migrationSqlHasNoForeignKeysOrCheckConstraints() {
        listOf(
            CREATE_BUDGETS_SQL,
            CREATE_ACCOUNTS_SQL,
            CREATE_TRANSFERS_SQL,
            CREATE_TAGS_SQL,
            CREATE_TRANSACTION_TAGS_SQL,
            CREATE_RECURRING_RULES_SQL,
            CREATE_REMINDER_DISMISSALS_SQL,
        ).forEach {
            assertFalse(it.contains("REFERENCES"))
            assertFalse(it.contains("CHECK"))
        }
    }
}
