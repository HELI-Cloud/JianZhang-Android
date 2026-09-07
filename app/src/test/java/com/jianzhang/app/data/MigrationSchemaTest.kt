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
        assertEquals(normalize(createSqlFromSchema("budgets"), "budgets"), normalize(CREATE_BUDGETS_SQL, "budgets"))
    }

    @Test
    fun migrationSqlHasNoForeignKeysOrCheckConstraints() {
        assertFalse(CREATE_BUDGETS_SQL.contains("REFERENCES"))
        assertFalse(CREATE_BUDGETS_SQL.contains("CHECK"))
    }
}