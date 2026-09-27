package com.daybudget.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.CLOSING_END_OF_MONTH
import com.daybudget.app.domain.closingLabel
import com.daybudget.app.domain.formatNumber
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.DisplayFamily

val BUDGET_PRESETS = listOf(30_000, 50_000, 80_000, 100_000)
const val MAX_BUDGET = 9_999_999

@Composable
fun BudgetInput(value: Int, showPresets: Boolean = true, onChange: (Int) -> Unit) {
    val c = Db.colors
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text("¥", fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, color = c.muted, modifier = Modifier.padding(bottom = 8.dp))
                BasicTextField(
                    value = if (value > 0) formatNumber(value) else "",
                    onValueChange = { s -> onChange(minOf(MAX_BUDGET, s.filter(Char::isDigit).take(7).toIntOrNull() ?: 0)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(c.accent),
                    textStyle = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, letterSpacing = (-2).sp, color = c.ink, textAlign = TextAlign.Center),
                    modifier = Modifier.weight(1f, fill = false),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (value == 0) Text("0", fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, color = c.faint)
                            inner()
                        }
                    },
                )
            }
            HorizontalDivider(thickness = 2.dp, color = c.ink)
        }
        if (showPresets) ChoiceGrid(BUDGET_PRESETS.map { it to "${it / 10_000}万" }, value, columns = 4, onSelect = onChange)
    }
}

@Composable
fun ClosingDayPicker(value: Int, showAll: Boolean, onChange: (Int) -> Unit) {
    if (showAll) {
        ChoiceGrid((1..CLOSING_END_OF_MONTH).map { it to closingLabel(it) }, value, columns = 7, compact = true, onSelect = onChange)
    } else {
        ChoiceGrid(listOf(5, 10, 15, 20, 25, CLOSING_END_OF_MONTH).map { it to closingLabel(it) }, value, columns = 3, onSelect = onChange)
    }
}

@Composable
fun <T> ChoiceGrid(options: List<Pair<T, String>>, selected: T, columns: Int, compact: Boolean = false, onSelect: (T) -> Unit) {
    val c = Db.colors
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
        options.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
                row.forEach { (v, label) ->
                    val on = v == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(if (compact) 40.dp else 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (on) c.ink else c.surface)
                            .border(1.5.dp, if (on) c.ink else c.line, RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { onSelect(v) }
                            .semantics { this.selected = on },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (on) c.appBg else c.ink, fontWeight = FontWeight.Bold, fontSize = if (compact) 13.sp else 14.sp, maxLines = 1)
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** ラベルと値の一覧（月度のプレビューなど） */
@Composable
fun PreviewBox(rows: List<Pair<String, String>>, background: Color = Db.colors.surface) {
    val c = Db.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(background).border(1.dp, c.line, RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { (k, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(k, color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(v, color = c.ink, fontSize = 13.sp, style = com.daybudget.app.ui.theme.MonoStyle)
            }
        }
    }
}
