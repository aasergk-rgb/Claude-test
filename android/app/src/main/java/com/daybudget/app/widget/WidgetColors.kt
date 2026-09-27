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
        private val SAKURA = WidgetColors(0xFFFFF1F4, 0xFF3A1E28, 0xFF9A6B78, 0xFFD9467A, 0xFFFBD9E3, 0xFFF6D0DB, 0xFF2E9E6B, 0xFF3D6FD8, 0xFFC27A12, 0xFFD93B4C)
        private val MINT = WidgetColors(0xFFE9F7F1, 0xFF0F2E25, 0xFF5B7F72, 0xFF0E7C5A, 0xFFCBEBDD, 0xFFCFE9DE, 0xFF0E8F5E, 0xFF2F6FB8, 0xFFB7700C, 0xFFCF3B34)

        fun of(theme: WidgetTheme) = when (theme) {
            WidgetTheme.DARK -> DARK
            WidgetTheme.WHITE -> WHITE
            WidgetTheme.NEON -> NEON
            WidgetTheme.SAKURA -> SAKURA
            WidgetTheme.MINT -> MINT
        }
    }
}
