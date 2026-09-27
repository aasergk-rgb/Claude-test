package com.daybudget.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.daybudget.app.R

/** 数字の表示用（Bricolage Grotesque） */
val DisplayFamily = FontFamily(
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
)

/** 金額の一覧用の等幅（IBM Plex Mono） */
val MonoFamily = FontFamily(Font(R.font.plex_mono_medium, FontWeight.Medium))

val DbTypography = Typography(
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold),
)

val MonoStyle = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp)
