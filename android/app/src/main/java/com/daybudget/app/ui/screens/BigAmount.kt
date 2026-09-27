package com.daybudget.app.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.daybudget.app.ui.theme.DisplayFamily
import java.text.NumberFormat
import java.util.Locale

/** 「¥2,450」の巨大表示。桁が増えたら画面幅に収まるよう小さくする */
@Composable
fun BigAmount(value: Int, color: Color, modifier: Modifier = Modifier, maxSize: Int = 84) {
    val digits = NumberFormat.getIntegerInstance(Locale.JAPAN).format(kotlin.math.abs(value.toLong()))
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthSp = with(LocalDensity.current) { maxWidth.toSp().value }
        val chars = digits.length + (if (value < 0) 1 else 0) + 1
        val size = minOf(maxSize.toFloat(), widthSp / (chars * 0.62f))
        Text(
            buildAnnotatedString {
                if (value < 0) append("−")
                withStyle(SpanStyle(fontSize = (size * 0.46f).sp, fontWeight = FontWeight.Bold, baselineShift = BaselineShift(0.35f))) { append("¥") }
                append(digits)
            },
            Modifier.fillMaxWidth(),
            color = color,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = size.sp,
            letterSpacing = (-size * 0.03f).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
