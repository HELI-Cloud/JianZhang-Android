package com.jianzhang.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.jianzhang.app.JzApplication
import com.jianzhang.app.data.AuthStore
import com.jianzhang.app.data.JzRepository
import com.jianzhang.app.data.ReminderEngine
import com.jianzhang.app.data.ReminderScheduler
import com.jianzhang.app.data.SettingsStore
import com.jianzhang.app.data.Transaction
import com.jianzhang.app.data.Transfer
import com.jianzhang.app.model.AppSettings
import com.jianzhang.app.model.AuthStatus
import com.jianzhang.app.model.ReminderItem
import com.jianzhang.app.util.currentMonth
import com.jianzhang.app.util.shiftMonth

class JzViewModel(app: Application) : AndroidViewModel(app) {
    val repository: JzRepository = (app as JzApplication).repository
    private val settingsStore: SettingsStore = (app as JzApplication).settingsStore
    private val authStore: AuthStore = (app as JzApplication).authStore

    var month by mutableStateOf(currentMonth())
        private set

    var refreshKey by mutableStateOf(0)
        private set

    var route by mutableStateOf("overview")
        private set

    var reminders by mutableStateOf<List<ReminderItem>>(emptyList())
        private set

    var reminderCount by mutableStateOf(0)
        private set

    var settings by mutableStateOf(settingsStore.load())
        private set

    var editing by mutableStateOf<Transaction?>(null)
        private set

    var sheetOpen by mutableStateOf(false)
        private set

    var editingTransfer by mutableStateOf<Transfer?>(null)
        private set

    var transferOpen by mutableStateOf(false)
        private set

    var authStatus by mutableStateOf(if (authStore.hasPassword()) AuthStatus.LOCKED else AuthStatus.SETUP)
        private set

    fun changeMonth(delta: Int) {
        month = shiftMonth(month, delta)
    }

    fun goCurrentMonth() {
        month = currentMonth()
    }

    fun refresh() {
        refreshKey++
    }

    fun navigate(next: String) {
        route = next
    }

    suspend fun refreshReminders() {
        val items = runCatching { ReminderEngine.build(repository, settings) }.getOrDefault(emptyList())
        val dismissed = runCatching { repository.dismissedReminderKeys() }.getOrDefault(emptySet())
        reminders = items
        reminderCount = items.count { it.key !in dismissed }
    }

    suspend fun dismissReminder(key: String) {
        repository.dismissReminder(key)
        refreshReminders()
    }

    suspend fun clearReminderDismissals() {
        repository.clearDismissedReminders()
        refreshReminders()
    }

    suspend fun runRecurringNow(): Int {
        val count = repository.runRecurring(autoOnly = false)
        refresh()
        refreshReminders()
        return count
    }

    fun saveSettings(s: AppSettings) {
        settings = s
        settingsStore.save(s)
        ReminderScheduler.sync(getApplication(), s)
    }

    fun openAdd() {
        editing = null
        sheetOpen = true
    }

    fun openEdit(tx: Transaction) {
        editing = tx
        sheetOpen = true
    }

    fun closeSheet() {
        sheetOpen = false
        editing = null
    }

    fun openTransfer(transfer: Transfer? = null) {
        editingTransfer = transfer
        transferOpen = true
    }

    fun closeTransfer() {
        transferOpen = false
        editingTransfer = null
    }

    fun rememberLastCategory(id: Long) {
        settingsStore.setLastCategoryId(id)
    }

    fun lastCategoryId(): Long = settingsStore.lastCategoryId()

    // ---------- 密码锁 ----------

    /** 解锁：校验通过返回 true 并进入主界面 */
    fun unlock(password: String): Boolean {
        val ok = authStore.verify(password)
        if (ok) authStatus = AuthStatus.UNLOCKED
        return ok
    }

    /** 首次设置密码（或修改密码后落盘） */
    fun setPassword(password: String) {
        authStore.setPassword(password)
        authStatus = AuthStatus.UNLOCKED
    }

    /** 修改密码：先验证当前密码 */
    fun changePassword(current: String, next: String): Boolean {
        if (!authStore.verify(current)) return false
        authStore.setPassword(next)
        return true
    }

    fun lockNow() {
        authStatus = AuthStatus.LOCKED
    }

    /** 忘记密码：清空数据并重置密码 */
    suspend fun resetAll() {
        repository.resetAllData()
        authStore.clear()
        authStatus = AuthStatus.SETUP
    }
}
