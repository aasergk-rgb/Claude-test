package com.daybudget.app.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.daybudget.app.ui.components.RollingText
import com.daybudget.app.ui.theme.DisplayFamily
import java.text.NumberFormat
import java.util.Locale

/** 「¥2,450」の巨大表示。桁が増えたら画面幅に収まるよう小さくし、変化は1桁ずつ入れ替える */
@Composable
fun BigAmount(value: Int, color: Color, modifier: Modifier = Modifier, maxSize: Int = 84) {
    val digits = NumberFormat.getIntegerInstance(Locale.JAPAN).format(kotlin.math.abs(value.toLong()))
    val previous = remember { mutableIntStateOf(value) }
    val increasing = value >= previous.intValue
    SideEffect { previous.intValue = value }

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val widthSp = with(density) { maxWidth.toSp().value }
        val chars = digits.length + (if (value < 0) 1 else 0) + 1
        val size = minOf(maxSize.toFloat(), widthSp / (chars * 0.62f))
        val big = TextStyle(color = color, fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = size.sp, letterSpacing = (-size * 0.03f).sp)
        val yenTop = with(density) { (size * 0.2f).sp.toDp() }
        Row(verticalAlignment = Alignment.Top) {
            if (value < 0) Text("−", style = big)
            Text("¥", style = big.copy(fontSize = (size * 0.46f).sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp), modifier = Modifier.padding(top = yenTop))
            RollingText(digits, big, increasing)
        }
    }
}
