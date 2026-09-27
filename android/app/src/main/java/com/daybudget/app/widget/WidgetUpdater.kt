package com.daybudget.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

object WidgetUpdater {
    suspend fun updateAll(context: Context) {
        runCatching { DayBudgetWidget().updateAll(context) }
    }

    private fun hasWidgets(context: Context): Boolean {
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, DayBudgetWidgetReceiver::class.java))
        return ids.isNotEmpty()
    }

    /** 日付が変わったら「今日」の値に切り替えるため、次の0時に更新を予約する */
    fun scheduleMidnightRefresh(context: Context) {
        if (!hasWidgets(context)) return
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 5_000
        val pi = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, MidnightReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // 正確なアラームの権限は不要（数分ずれても問題ない）
        alarm.setAndAllowWhileIdle(AlarmManager.RTC, next, pi)
    }
}

class DayBudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = DayBudgetWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        WidgetUpdater.scheduleMidnightRefresh(context)
    }
}

/** 自分で予約した0時のアラームでウィジェットを翌日の表示に切り替える */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                (context.applicationContext as com.daybudget.app.DayBudgetApp).repository.materializeRecurring(java.time.LocalDate.now())
                WidgetUpdater.updateAll(context)
                WidgetUpdater.scheduleMidnightRefresh(context)
            } finally {
                pending.finish()
            }
        }
    }

}
