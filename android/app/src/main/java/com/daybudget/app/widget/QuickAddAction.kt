package com.daybudget.app.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.daybudget.app.DayBudgetApp
import com.daybudget.app.domain.formatYen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** ウィジェットの「よく使う金額」ボタン。アプリを開かずに今日の支出として記録する */
class QuickAddAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[PRESET_ID] ?: return
        val app = context.applicationContext as DayBudgetApp
        val added = app.repository.quickAdd(id, LocalDate.now()) ?: return
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "${added.memo ?: ""} ${formatYen(added.amount)} を記録しました", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        val PRESET_ID = ActionParameters.Key<String>("preset_id")
    }
}
