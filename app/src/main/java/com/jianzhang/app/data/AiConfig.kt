package com.jianzhang.app.data

import android.content.Context

data class AiConfig(
    val endpoint: String = "https://api.deepseek.com/chat/completions",
    val apiKey: String = "",
    val model: String = "deepseek-chat",
    val temperature: Float = 0.35f,
    val timeoutSecs: Int = 90,
)

class AiConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("jz_ai_config", Context.MODE_PRIVATE)

    fun load(): AiConfig = AiConfig(
        endpoint = prefs.getString("endpoint", "https://api.deepseek.com/chat/completions")
            ?: "https://api.deepseek.com/chat/completions",
        apiKey = prefs.getString("api_key", "") ?: "",
        model = prefs.getString("model", "deepseek-chat") ?: "deepseek-chat",
        temperature = prefs.getFloat("temperature", 0.35f),
        timeoutSecs = prefs.getInt("timeout_secs", 90),
    )

    fun save(config: AiConfig) {
        prefs.edit()
            .putString("endpoint", config.endpoint.trim())
            .putString("api_key", config.apiKey.trim())
            .putString("model", config.model.trim())
            .putFloat("temperature", config.temperature)
            .putInt("timeout_secs", config.timeoutSecs)
            .apply()
    }

    fun maskApiKey(key: String): String = when {
        key.isBlank() -> "未配置"
        key.length <= 8 -> "••••••••"
        else -> "${key.take(4)}••••${key.takeLast(4)}"
    }
}
