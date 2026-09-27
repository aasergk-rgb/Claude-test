package com.daybudget.app.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** 画面の演出を「一度だけ見せた」ことを覚えておくための小さな保存先（記録データとは別） */
class UiPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("ui", Context.MODE_PRIVATE)

    /** 節約ボーナスの演出を見せた日（YYYY-MM-DD） */
    var bonusShownDate: String?
        get() = prefs.getString("bonus_shown_date", null)
        set(v) = prefs.edit().putString("bonus_shown_date", v).apply()

    /** お祝いした連続記録（「連続が始まった日:節目」） */
    var celebratedStreak: String?
        get() = prefs.getString("celebrated_streak", null)
        set(v) = prefs.edit().putString("celebrated_streak", v).apply()

    /** 「予定」欄を開いているか */
    var plansExpanded: Boolean
        get() = prefs.getBoolean("plans_expanded", false)
        set(v) = prefs.edit().putBoolean("plans_expanded", v).apply()
}

/** 端末で「アニメーションを削除」がオンなら true（演出を省く） */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
