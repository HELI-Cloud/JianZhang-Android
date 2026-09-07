package com.jianzhang.app.data

import android.content.Context
import com.jianzhang.app.model.AppSettings

/** 设置持久化（SharedPreferences，纯本地） */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("jz_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        fontSize = prefs.getString("font_size", "medium") ?: "medium",
        // 军团 HUD 默认等宽字体；老用户保留自己的选择
        fontFamily = prefs.getString("font_family", "monospace") ?: "monospace",
        defaultType = prefs.getString("default_type", "expense") ?: "expense",
        rememberCategory = prefs.getBoolean("remember_category", false),
        themeMode = prefs.getString("theme_mode", "dark") ?: "dark",
        uiStyle = prefs.getString("ui_style", "hacker") ?: "hacker",
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putString("font_size", s.fontSize)
            .putString("font_family", s.fontFamily)
            .putString("default_type", s.defaultType)
            .putBoolean("remember_category", s.rememberCategory)
            .putString("theme_mode", s.themeMode)
            .putString("ui_style", s.uiStyle)
            .apply()
    }

    fun lastCategoryId(): Long = prefs.getLong("last_cat", 0L)

    fun setLastCategoryId(id: Long) {
        prefs.edit().putLong("last_cat", id).apply()
    }
}