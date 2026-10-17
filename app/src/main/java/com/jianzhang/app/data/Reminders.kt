package com.jianzhang.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jianzhang.app.JzApplication
import com.jianzhang.app.MainActivity
import com.jianzhang.app.R
import com.jianzhang.app.model.AppSettings
import com.jianzhang.app.model.ReminderItem
import com.jianzhang.app.model.ReminderKind
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val PERIODIC_CHECK = "jianzhang-reminder-check"
    const val CADENCE_CHECK = "jianzhang-cadence-check"

    fun sync(context: Context, settings: AppSettings) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(PERIODIC_CHECK)
        wm.cancelUniqueWork(CADENCE_CHECK)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED)
            .build()
        val recurringCheck = PeriodicWorkRequestBuilder<ReminderCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        wm.enqueueUniquePeriodicWork(PERIODIC_CHECK, ExistingPeriodicWorkPolicy.UPDATE, recurringCheck)

        if (!settings.systemNotify) return
        nextCadence(settings)?.let { next ->
            val delay = Duration.between(LocalDateTime.now(), next).toMillis().coerceAtLeast(0L)
            val request = OneTimeWorkRequestBuilder<CadenceReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(CADENCE_CHECK, ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun nextCadence(settings: AppSettings): LocalDateTime? {
        if (!settings.dailyReminder && !settings.weeklyReminder) return null
        val time = ReminderEngine.parseTime(settings.dailyReminderTime)
        val now = LocalDateTime.now()
        val candidates = mutableListOf<LocalDateTime>()
        if (settings.dailyReminder) {
            var daily = LocalDateTime.of(LocalDate.now(), time)
            if (!daily.isAfter(now)) daily = daily.plusDays(1)
            candidates += daily
        }
        if (settings.weeklyReminder) {
            val targetDow = if (settings.weeklyReminderWeekday == 0) DayOfWeek.SUNDAY
            else DayOfWeek.of(settings.weeklyReminderWeekday.coerceIn(1, 7))
            val date = LocalDate.now().with(TemporalAdjusters.nextOrSame(targetDow))
            var weekly = LocalDateTime.of(date, time)
            if (!weekly.isAfter(now)) weekly = weekly.plusWeeks(1)
            candidates += weekly
        }
        return candidates.minOrNull()
    }
}

class ReminderCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as JzApplication
        val settings = app.settingsStore.load()
        runCatching { app.repository.runRecurring(autoOnly = true) }.getOrElse { return Result.retry() }
        if (!settings.systemNotify) return Result.success()
        val items = runCatching {
            ReminderEngine.build(app.repository, settings)
                .filter { it.kind == ReminderKind.BUDGET || it.kind == ReminderKind.BILL }
        }.getOrElse { return Result.retry() }
        NotificationCenter.notifyNew(applicationContext, items)
        return Result.success()
    }
}

class CadenceReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as JzApplication
        val settings = app.settingsStore.load()
        if (!settings.systemNotify) return Result.success()
        val items = runCatching {
            ReminderEngine.build(app.repository, settings)
                .filter { it.kind == ReminderKind.DAILY || it.kind == ReminderKind.WEEKLY }
        }.getOrElse { return Result.retry() }
        NotificationCenter.notifyNew(applicationContext, items)
        ReminderScheduler.sync(applicationContext, settings)
        return Result.success()
    }
}

object NotificationCenter {
    private const val CHANNEL_ID = "jianzhang-reminders"
    private const val PREFS = "jz_notification_state"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "简账提醒",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "预算、周期账单与记账提醒"
        }
        manager.createNotificationChannel(channel)
    }

    fun notifyNew(context: Context, items: List<ReminderItem>) {
        if (items.isEmpty()) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        createChannel(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        items.take(5).forEach { item ->
            val stateKey = "sent_${item.key}"
            if (prefs.getString(stateKey, null) == today) return@forEach
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("reminder_route", item.route)
            }
            val pending = PendingIntent.getActivity(
                context,
                item.key.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(item.title)
                .setContentText(item.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
                .setContentIntent(pending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context).notify(item.key.hashCode(), notification)
            prefs.edit().putString(stateKey, today).apply()
        }
    }
}
