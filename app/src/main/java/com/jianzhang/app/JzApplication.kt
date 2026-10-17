package com.jianzhang.app

import android.app.Application
import com.jianzhang.app.data.AppDatabase
import com.jianzhang.app.data.JzRepository
import com.jianzhang.app.data.AuthStore
import com.jianzhang.app.data.SettingsStore
import com.jianzhang.app.data.NotificationCenter
import com.jianzhang.app.data.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JzApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val repository: JzRepository by lazy { JzRepository(database) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }
    val authStore: AuthStore by lazy { AuthStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationCenter.createChannel(this)
        appScope.launch {
            runCatching { repository.runRecurring(autoOnly = true) }
        }
        ReminderScheduler.sync(this, settingsStore.load())
    }
}
