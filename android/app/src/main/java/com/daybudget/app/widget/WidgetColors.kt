package com.daybudget.app.widget

import com.daybudget.app.domain.Status
import com.daybudget.app.domain.WidgetTheme

/** ウィジェットの配色（テーマごとに固定。端末のダークモードには追従しない） */
data class WidgetColors(
    val background: Long,
    val text: Long,
    val sub: Long,
    val number: Long,
    val track: Long,
    val divider: Long,
    private val great: Long,
    private val healthy: Long,
    private val warning: Long,
    private val over: Long,
) {
    fun status(s: Status): Long = when (s) {
        Status.GREAT -> great
        Status.HEALTHY -> healthy
        Status.WARNING -> warning
        Status.OVER -> over
    }

    companion object {
        private val DARK = WidgetColors(0xFF121726, 0xFFF1F3F9, 0xFF9CA5B7, 0xFFF1F3F9, 0xFF262D40, 0xFF262D40, 0xFF3CC98A, 0xFF6F9DFF, 0xFFF2A93B, 0xFFFF6B5E)
        private val WHITE = WidgetColors(0xFFFFFFFF, 0xFF131A2B, 0xFF5F6980, 0xFF131A2B, 0xFFEDF0F5, 0xFFE4E8EF, 0xFF11905C, 0xFF2B63DB, 0xFFB56C00, 0xFFD5392F)
        private val NEON = WidgetColors(0xFF0D0620, 0xFFF7EDFF, 0xFFB9A6D9, 0xFFFF5FDC, 0xFF251A45, 0xFF2B1E52, 0xFF39E6FF, 0xFF39E6FF, 0xFFFFD23F, 0xFFFF4F7B)

        fun of(theme: WidgetTheme) = when (theme) {
            WidgetTheme.DARK -> DARK
            WidgetTheme.WHITE -> WHITE
            WidgetTheme.NEON -> NEON
        }
    }
}
