package com.jianzhang.app

import android.app.Application
import com.jianzhang.app.data.AppDatabase
import com.jianzhang.app.data.JzRepository
import com.jianzhang.app.data.AuthStore
import com.jianzhang.app.data.SettingsStore

class JzApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: JzRepository by lazy { JzRepository(database) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val authStore: AuthStore by lazy { AuthStore(this) }
}