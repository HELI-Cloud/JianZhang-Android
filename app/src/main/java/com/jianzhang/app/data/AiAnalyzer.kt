package com.jianzhang.app.data

import com.jianzhang.app.model.CsvRow
import com.jianzhang.app.model.MonthAgg
import com.jianzhang.app.model.TxType
import com.jianzhang.app.util.lastNMonths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

enum class AiRange { MONTH, SIX_MONTHS, ALL }
enum class AiFocus { HEALTH, STRUCTURE, BUDGET, CUSTOM }

class AiAnalyzer {

    suspend fun run(
        repository: JzRepository,
        config: AiConfig,
        range: AiRange,
        focus: AiFocus,
        customQuestion: String,
        selectedMonth: String,
    ): String = withContext(Dispatchers.IO) {
        val context = buildContext(repository, range, selectedMonth)
        val messages = buildMessages(context, focus, customQuestion)
        val body = JSONObject()
            .put("model", config.model.trim())
            .put("messages", messages)
            .put("temperature", config.temperature.toDouble())
            .put("stream", false)
        requestChat(normalizeEndpoint(config.endpoint), config.apiKey.trim(), config.timeoutSecs, body)
    }

    suspend fun buildContext(
        repository: JzRepository,
        range: AiRange,
        selectedMonth: String,
    ): JSONObject {
        val months = lastNMonths(6, selectedMonth)
        val summary = repository.monthSummary(selectedMonth)
        val expenseCats = repository.categoryTotals(selectedMonth, TxType.EXPENSE)
        val incomeCats = repository.categoryTotals(selectedMonth, TxType.INCOME)
        val daily = repository.dailyTotals(selectedMonth)
        val budgets = repository.budgetsForMonth(selectedMonth)
        val categories = repository.categories()
        val allRows = repository.csvRows()

        val catName = categories.associate { it.id to it.name }

        val monthlyTrend: List<MonthAgg> = if (range == AiRange.ALL) {
            trendFromCsv(allRows)
        } else {
            repository.trend(months)
        }

        val trendJson = JSONArray()
        monthlyTrend.sortedBy { it.ym }.groupBy { it.ym }.forEach { (month, rows) ->
            trendJson.put(
                JSONObject()
                    .put("month", month)
                    .put("income_yuan", yuan(rows.firstOrNull { it.type == "income" }?.totalCents ?: 0L))
                    .put("expense_yuan", yuan(rows.firstOrNull { it.type == "expense" }?.totalCents ?: 0L)),
            )
        }

        val recent = recentRows(allRows, range, selectedMonth)
        val recentJson = JSONArray()
        recent.forEach { row ->
            recentJson.put(
                JSONObject()
                    .put("date", row.date)
                    .put("type", if (row.type == "expense") "支出" else "收入")
                    .put("category", row.category)
                    .put("amount_yuan", yuan(row.amountCents))
                    .put("note", row.note),
            )
        }

        val budgetsJson = JSONArray()
        budgets.forEach { budget ->
            budgetsJson.put(
                JSONObject()
                    .put("category", budget.categoryId?.let { catName[it] } ?: "总预算")
                    .put("amount_yuan", yuan(budget.amountCents)),
            )
        }

        val periodLabel = when (range) {
            AiRange.MONTH -> "本月（$selectedMonth）"
            AiRange.SIX_MONTHS -> "近 6 个月（截至 $selectedMonth）"
            AiRange.ALL -> "全部历史记录"
        }

        return JSONObject()
            .put("period_label", periodLabel)
            .put("selected_month", selectedMonth)
            .put(
                "summary",
                JSONObject()
                    .put("income_yuan", yuan(summary.incomeCents))
                    .put("expense_yuan", yuan(summary.expenseCents))
                    .put("balance_yuan", yuan(summary.incomeCents - summary.expenseCents)),
            )
            .put("category_expense", categoryJson(expenseCats))
            .put("category_income", categoryJson(incomeCats))
            .put(
                "daily_expense",
                JSONArray().apply {
                    daily.forEach { day ->
                        put(JSONObject().put("day", day.day).put("amount_yuan", yuan(day.totalCents)))
                    }
                },
            )
            .put("monthly_trend", trendJson)
            .put("budgets", budgetsJson)
            .put("recent_transactions", recentJson)
    }

    private fun categoryJson(items: List<com.jianzhang.app.model.CategoryTotal>): JSONArray =
        JSONArray().apply {
            items.forEach { cat ->
                put(JSONObject().put("name", cat.name).put("amount_yuan", yuan(cat.totalCents)))
            }
        }

    private fun trendFromCsv(rows: List<CsvRow>): List<MonthAgg> {
        val grouped = rows.groupBy { it.date.take(7) }
        return grouped.map { (month, list) ->
            val income = list.filter { it.type == "income" }.sumOf { it.amountCents }
            val expense = list.filter { it.type == "expense" }.sumOf { it.amountCents }
            listOf(
                MonthAgg(month, "income", income),
                MonthAgg(month, "expense", expense),
            )
        }.flatten()
    }

    private fun recentRows(rows: List<CsvRow>, range: AiRange, selectedMonth: String): List<CsvRow> =
        when (range) {
            AiRange.MONTH -> rows.filter { it.date.startsWith(selectedMonth) }.take(160)
            AiRange.SIX_MONTHS -> {
                val start = lastNMonths(6, selectedMonth).first()
                rows.filter { it.date.take(7) in start..selectedMonth }.take(220)
            }
            AiRange.ALL -> rows.take(260)
        }

    private fun buildMessages(context: JSONObject, focus: AiFocus, customQuestion: String): JSONArray {
        val system = "你是“简账”内置的本地财务分析助手。只依据用户提供的账本数据回答，数据不足时直接说明。使用中文，输出简洁 Markdown，用小标题和短段落，避免空话、营销腔、表情和过度免责声明。金额保留两位小数，单位统一为人民币元。"
        val focusText = when (focus) {
            AiFocus.HEALTH -> "综合财务健康诊断，重点看收入、支出、结余、储蓄倾向和异常波动。"
            AiFocus.STRUCTURE -> "消费结构分析，重点识别主要支出项、集中度、非必要支出和可优化空间。"
            AiFocus.BUDGET -> "预算执行与风险分析，重点对比实际支出和预算，指出超支风险、剩余日均额度和建议。"
            AiFocus.CUSTOM -> "按用户补充的问题展开分析，并给出可执行的建议。"
        }
        val parts = mutableListOf(
            "分析范围：${context.optString("period_label")}",
            "分析重点：$focusText",
        )
        if (customQuestion.isNotBlank()) parts.add("用户补充：${customQuestion.trim()}")
        parts.add("")
        parts.add("账本数据（JSON）：")
        parts.add(context.toString(2))
        val user = parts.joinToString("\n")
        return JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", user))
    }

    private fun requestChat(endpoint: String, apiKey: String, timeoutSecs: Int, body: JSONObject): String {
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = timeoutSecs * 1000
            connection.readTimeout = timeoutSecs * 1000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            if (apiKey.isNotBlank()) {
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
            }
            connection.outputStream.use { stream ->
                stream.write(body.toString().toByteArray(StandardCharsets.UTF_8))
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(StandardCharsets.UTF_8)?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                throw IllegalArgumentException("AI 请求失败（HTTP $code）：${raw.take(300)}")
            }
            val root = JSONObject(raw)
            val choices = root.optJSONArray("choices")
            val first = choices?.optJSONObject(0) ?: throw IllegalArgumentException("AI 返回格式不正确：缺少 choices")
            val message = first.optJSONObject("message") ?: throw IllegalArgumentException("AI 返回格式不正确：缺少 message")
            val content = message.optString("content").ifBlank {
                first.optString("text").ifBlank {
                    throw IllegalArgumentException("AI 返回格式不正确：缺少回答内容")
                }
            }
            return content.trim()
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeEndpoint(raw: String): String {
        var value = raw.trim().trimEnd('/')
        if (value.isBlank()) return value
        if (!value.startsWith("http://") && !value.startsWith("https://")) return value
        if (value.contains("/chat/completions")) return value
        if (value.endsWith("/v1") || value.endsWith("/api")) return "$value/chat/completions"
        return "$value/chat/completions"
    }

    private fun yuan(cents: Long): Double =
        BigDecimal(cents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toDouble()
}
