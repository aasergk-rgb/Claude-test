package com.daybudget.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.daybudget.app.DayBudgetApp
import com.daybudget.app.MainActivity
import com.daybudget.app.R
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.CarryoverMode
import com.daybudget.app.domain.DayState
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.PlannedExpense
import com.daybudget.app.domain.UserSettings
import com.daybudget.app.domain.formatYen
import com.daybudget.app.ui.Routes
import com.daybudget.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** 朝のお知らせ（今日使える額）と夜のリマインド */
object DailyNotifications {
    private const val CHANNEL = "daily"
    const val ACTION_MORNING = "com.daybudget.app.action.MORNING"
    const val ACTION_EVENING = "com.daybudget.app.action.EVENING"

    enum class Kind(val action: String, val id: Int) { MORNING(ACTION_MORNING, 1), EVENING(ACTION_EVENING, 2) }

    data class Message(val title: String, val body: String)

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "毎日のお知らせ", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "朝の「今日使える額」と夜のリマインド"
            },
        )
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** 設定に合わせて次の通知を予約（オフなら取り消し）する */
    fun schedule(context: Context, settings: UserSettings) {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        listOf(
            Triple(Kind.MORNING, settings.morningNotify, settings.morningTime),
            Triple(Kind.EVENING, settings.eveningNotify, settings.eveningTime),
        ).forEach { (kind, enabled, minutes) ->
            val pi = pendingIntent(context, kind)
            if (enabled && settings.onboarded) {
                // 正確なアラームの権限は不要。10分の幅の中で届けばよい
                alarm.setWindow(AlarmManager.RTC_WAKEUP, nextTrigger(minutes, LocalDateTime.now()), 10 * 60_000L, pi)
            } else {
                alarm.cancel(pi)
            }
        }
    }

    fun nextTrigger(minutes: Int, now: LocalDateTime): Long {
        val time = LocalTime.of(minutes / 60, minutes % 60)
        var at = now.toLocalDate().atTime(time)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun pendingIntent(context: Context, kind: Kind) = PendingIntent.getBroadcast(
        context, kind.id,
        Intent(context, NotificationReceiver::class.java).setAction(kind.action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** 通知の文面（端末に依存しないのでテストできる） */
    fun message(kind: Kind, settings: UserSettings, expenses: List<Expense>, planned: List<PlannedExpense>, today: LocalDate): Message {
        val snap = BudgetCalculator.computeToday(settings, expenses, today, planned)
        return when (kind) {
            Kind.MORNING -> {
                val title = "今日は ${formatYen(snap.dailyBudget)} 使えます"
                val body = when {
                    settings.carryoverMode == CarryoverMode.SAVINGS -> "今月の貯金は ${formatYen(snap.savingsAmount)}。今日も予算内でいきましょう"
                    snap.plannedToday > 0 -> {
                        val label = planned.filter { it.date == today }.joinToString("・") { it.label }
                        "今日は予定していた「$label」の日。${formatYen(snap.plannedToday)} を含んでいます"
                    }
                    else -> {
                        val yesterday = BudgetCalculator.simulatePeriod(snap.period, settings, expenses, today, planned)
                            .firstOrNull { it.date == today.minusDays(1) && it.state != DayState.INACTIVE }
                        val diff = if (yesterday == null) 0 else snap.dailyBudget - yesterday.budget
                        when {
                            diff > 0 -> "昨日の節約で +${formatYen(diff)} 増えました"
                            diff < 0 -> "昨日の使いすぎで ${formatYen(diff)}。今日は少し控えめに"
                            else -> "今日も予算内でいきましょう"
                        }
                    }
                }
                Message(title, body)
            }
            Kind.EVENING -> if (snap.todaySpent == 0) {
                Message("今日の支出は記録しましたか？", "使わなかった日は記録しなくて大丈夫。そのぶん明日の予算が増えます")
            } else {
                Message(
                    "今日の残りは ${formatYen(snap.todayAvailable)}",
                    snap.tomorrowBudget?.let { "このまま使わなければ、明日は ${formatYen(it)}" } ?: "今日で${snap.period.name}はおしまいです",
                )
            }
        }
    }

    fun post(context: Context, kind: Kind, message: Message) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val open = PendingIntent.getActivity(
            context, 100 + kind.id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (kind == Kind.EVENING) putExtra(MainActivity.EXTRA_ROUTE, Routes.ADD)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_yen)
            .setColor(0xFFFFC53D.toInt())
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(kind.id, n) }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = DailyNotifications.Kind.entries.firstOrNull { it.action == intent.action } ?: return
        val pending = goAsync()
        val app = context.applicationContext as DayBudgetApp
        app.appScope.launch {
            try {
                val repo = app.repository
                repo.materializeRecurring(LocalDate.now())
                repo.loadCategories()
                val settings = repo.loadSettings() ?: return@launch
                val enabled = if (kind == DailyNotifications.Kind.MORNING) settings.morningNotify else settings.eveningNotify
                if (enabled && settings.onboarded) {
                    val msg = DailyNotifications.message(kind, settings, repo.loadExpenses(), repo.loadAllPlanned(LocalDate.now()), LocalDate.now())
                    DailyNotifications.post(context, kind, msg)
                }
                DailyNotifications.schedule(context, settings)
            } finally {
                pending.finish()
            }
        }
    }
}

/** 再起動・時刻変更のあとに、通知とウィジェット更新の予約をやり直す */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as DayBudgetApp
                app.repository.loadSettings()?.let { DailyNotifications.schedule(context, it) }
                WidgetUpdater.updateAll(context)
                WidgetUpdater.scheduleMidnightRefresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val ACTIONS = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED)
    }
}
