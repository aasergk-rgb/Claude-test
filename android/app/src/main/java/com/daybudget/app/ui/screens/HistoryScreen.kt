package com.daybudget.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.DayState
import com.daybudget.app.domain.DaySummary
import com.daybudget.app.domain.Period
import com.daybudget.app.domain.daysBetween
import com.daybudget.app.domain.formatNumber
import com.daybudget.app.domain.formatSignedYen
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.longJa
import com.daybudget.app.domain.md
import com.daybudget.app.ui.AppState
import com.daybudget.app.ui.components.DbIconButton
import com.daybudget.app.ui.components.EmptyBox
import com.daybudget.app.ui.components.Panel
import com.daybudget.app.ui.components.SheetTarget
import com.daybudget.app.ui.components.TopBar
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.MonoFamily
import com.daybudget.app.ui.theme.MonoStyle
import java.time.LocalDate

private val WEEKDAYS = listOf("日", "月", "火", "水", "木", "金", "土")

@Composable
fun HistoryScreen(state: AppState.Ready, openSheet: (SheetTarget) -> Unit, onBack: () -> Unit) {
    val c = Db.colors
    val settings = state.settings
    var anchor by rememberSaveable(state.today) { mutableStateOf(state.today.toString()) }
    var selected by rememberSaveable(state.today) { mutableStateOf(state.today.toString()) }

    val period = remember(anchor, settings.closingDay) { Period.of(LocalDate.parse(anchor), settings.closingDay) }
    val isCurrent = state.today in period
    val range = BudgetCalculator.activeRange(period, settings.monthlyBudget, settings.startDate)
    val days = remember(period, state) { BudgetCalculator.simulatePeriod(period, settings, state.expenses, state.today) }
    val spent = days.sumOf { it.spent }
    val third = if (isCurrent) {
        val elapsed = daysBetween(range.from, state.today) + 1
        "ペースより" to ((range.budget.toLong() * elapsed / range.days).toInt() - spent)
    } else {
        "残った額" to (range.budget - spent)
    }
    val sel = LocalDate.parse(selected).let { s ->
        if (s in period && !s.isAfter(state.today) && !s.isBefore(range.from)) s else if (isCurrent) state.today else period.end
    }
    val dayInfo = days.firstOrNull { it.date == sel }
    val dayItems = remember(state, sel) { state.expenses.filter { it.date == sel }.sortedBy { it.createdAt } }
    val hasOlder = settings.startDate?.isBefore(period.start) == true

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 40.dp),
    ) {
        item { TopBar("履歴", left = { DbIconButton(DbIcons.Left, "戻る", onClick = onBack) }) }
        item {
            Panel(Modifier.padding(top = 8.dp)) {
                Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    DbIconButton(DbIcons.Left, "前の月度", onClick = { anchor = period.start.minusDays(1).toString() }, enabled = hasOlder)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(period.name, color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${period.start.md()}〜${period.end.md()}", color = c.muted, fontSize = 12.sp)
                    }
                    DbIconButton(DbIcons.Right, "次の月度", onClick = { anchor = period.end.plusDays(1).toString() }, enabled = !isCurrent)
                }
            }
        }
        item {
            Panel(Modifier.padding(top = 12.dp, bottom = 18.dp)) {
                Row {
                    Stat(if (range.budget < settings.monthlyBudget) "予算（日割り）" else "今月の予算", formatYen(range.budget), c.ink, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(62.dp).background(c.line))
                    Stat("累計支出", formatYen(spent), c.ink, Modifier.weight(1f))
                    Box(Modifier.width(1.dp).height(62.dp).background(c.line))
                    Stat(third.first, formatSignedYen(third.second), if (third.second >= 0) c.great else c.over, Modifier.weight(1f))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                WEEKDAYS.forEachIndexed { i, w ->
                    Text(
                        w,
                        Modifier.weight(1f),
                        color = when (i) { 0 -> c.over.copy(alpha = 0.75f); 6 -> c.healthy.copy(alpha = 0.75f); else -> c.faint },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
        item {
            val leading = period.start.dayOfWeek.value % 7
            val cells: List<DaySummary?> = List(leading) { null } + days
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                cells.chunked(7).forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        week.forEach { d ->
                            Box(Modifier.weight(1f)) {
                                if (d != null) DayCell(d, isFirst = d.date == period.start, today = state.today, selected = d.date == sel) { selected = d.date.toString() }
                            }
                        }
                        repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.End)) {
                Legend(c.great, "予算内")
                Legend(c.over, "超過")
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
                Text(sel.longJa(), color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (dayInfo != null) Text("${formatYen(dayInfo.spent)} / 予算 ${formatYen(dayInfo.budget)}", color = c.muted, fontSize = 12.sp)
            }
        }
        if (dayItems.isEmpty()) item { EmptyBox("この日の支出はありません") }
        items(dayItems, key = { it.id }) { e ->
            ExpenseItem(
                e,
                subtitle = e.memo ?: " ",
                onClick = { openSheet(SheetTarget.Edit(e)) },
                actionIcon = DbIcons.Right,
                actionDescription = "編集",
                onAction = { openSheet(SheetTarget.Edit(e)) },
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        item {
            Row(
                Modifier.padding(top = 4.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, c.line, RoundedCornerShape(14.dp)).clickable(role = Role.Button) { openSheet(SheetTarget.Add(sel)) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DbIcons.Plus, null, tint = c.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("この日に支出を追加", color = c.muted, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
        Text(label, color = Db.colors.muted, fontSize = 11.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, color = color, fontSize = 14.sp, style = MonoStyle, maxLines = 1)
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, color = Db.colors.muted, fontSize = 11.sp)
    }
}

@Composable
private fun DayCell(d: DaySummary, isFirst: Boolean, today: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val c = Db.colors
    val future = d.state == DayState.FUTURE
    val off = d.state == DayState.INACTIVE
    val enabled = !future && !off
    val bg = when {
        selected -> c.ink
        future || off -> Color.Transparent
        else -> c.surface
    }
    val border = when {
        d.date == today -> c.accent
        selected -> c.ink
        off -> Color.Transparent
        else -> c.line
    }
    val label = if (isFirst || d.date.dayOfMonth == 1) d.date.md() else d.date.dayOfMonth.toString()
    Column(
        Modifier
            .aspectRatio(1f / 1.12f)
            .alpha(if (future) 0.55f else if (off) 0.4f else 1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (d.date == today) 2.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = d.date.longJa(); this.selected = selected }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = if (selected) c.appBg else c.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(if (enabled) formatNumber(d.spent) else "", color = if (selected) c.appBg else c.muted, fontSize = 9.5.sp, fontFamily = MonoFamily, maxLines = 1, letterSpacing = (-0.3).sp)
        val dot = when (d.state) {
            DayState.UNDER -> c.great
            DayState.OVER -> c.over
            DayState.TODAY -> if (d.spent <= d.budget) c.great else c.over
            else -> Color.Transparent
        }
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
    }
}
