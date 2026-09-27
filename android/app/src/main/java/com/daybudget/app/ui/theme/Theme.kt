package com.daybudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.daybudget.app.domain.AppTheme
import com.daybudget.app.domain.Status

/** Web版デモと同じデザイントークン */
@Immutable
data class DbColors(
    val appBg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color = Color(0xFFFFC53D),
    val accentPress: Color = Color(0xFFF0B11C),
    val accentInk: Color = Color(0xFF241A02),
    val great: Color,
    val healthy: Color,
    val warning: Color,
    val over: Color,
    val isDark: Boolean,
) {
    fun status(s: Status) = when (s) {
        Status.GREAT -> great
        Status.HEALTHY -> healthy
        Status.WARNING -> warning
        Status.OVER -> over
    }
}

val LightColors = DbColors(
    appBg = Color(0xFFF7F8FB), surface = Color.White, surface2 = Color(0xFFEEF1F6), line = Color(0xFFDDE2EB),
    ink = Color(0xFF131A2B), muted = Color(0xFF5F6980), faint = Color(0xFF98A1B4),
    great = Color(0xFF11905C), healthy = Color(0xFF2B63DB), warning = Color(0xFFB56C00), over = Color(0xFFD5392F),
    isDark = false,
)

val DarkColors = DbColors(
    appBg = Color(0xFF0F131C), surface = Color(0xFF181D29), surface2 = Color(0xFF212736), line = Color(0xFF2B3243),
    ink = Color(0xFFEDF0F7), muted = Color(0xFF9CA5B7), faint = Color(0xFF687185),
    great = Color(0xFF3CC98A), healthy = Color(0xFF6F9DFF), warning = Color(0xFFF2A93B), over = Color(0xFFFF6B5E),
    isDark = true,
)

val LocalDbColors = staticCompositionLocalOf { LightColors }

object Db {
    val colors: DbColors
        @Composable get() = LocalDbColors.current
}

@Composable
fun DayBudgetTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val c = if (dark) DarkColors else LightColors
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = c.ink,
        onPrimary = c.appBg,
        secondary = c.accent,
        onSecondary = c.accentInk,
        background = c.appBg,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        surfaceVariant = c.surface2,
        onSurfaceVariant = c.muted,
        surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface,
        surfaceContainerHighest = c.surface2,
        surfaceContainerLow = c.surface,
        outline = c.line,
        outlineVariant = c.line,
        error = c.over,
    )
    CompositionLocalProvider(LocalDbColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = DbTypography, content = content)
    }
}
