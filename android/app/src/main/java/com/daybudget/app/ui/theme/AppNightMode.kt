package com.daybudget.app.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.daybudget.app.domain.AppTheme

/**
 * アプリのテーマ設定を OS に伝える（Android 12 以降）。
 * 起動時のスプラッシュ画面はアプリが動く前に OS が描くため、これをしないと
 * 端末のダークモード設定に合わせて表示されてしまう。
 */
object AppNightMode {
    fun apply(context: Context, theme: AppTheme) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val ui = context.getSystemService(UiModeManager::class.java) ?: return
        val mode = when (theme) {
            AppTheme.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
            AppTheme.LIGHT -> UiModeManager.MODE_NIGHT_NO
            AppTheme.DARK -> UiModeManager.MODE_NIGHT_YES
        }
        runCatching { ui.setApplicationNightMode(mode) }
    }
}
