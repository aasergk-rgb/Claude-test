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
import com.daybudget.app.ui.LocalMessenger
import com.daybudget.app.ui.MainViewModel
import com.daybudget.app.ui.components.PlannedSheet
import com.daybudget.app.ui.components.SwipeableExpense
import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.isRecurring
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
fun HistoryScreen(
    state: AppState.Ready,
    viewModel: MainViewModel,
    openSheet: (SheetTarget) -> Unit,
    onBack: () -> Unit,
    openRecap: (LocalDate) -> Unit = {},
) {
    val c = Db.colors
    val settings = state.settings
    var anchor by rememberSaveable(state.today) { mutableStateOf(state.today.toString()) }
    var selected by rememberSaveable(state.today) { mutableStateOf(state.today.toString()) }

    val period = remember(anchor, settings.closingDay) { Period.of(LocalDate.parse(anchor), settings.closingDay) }
    val isCurrent = state.today in period
    val range = BudgetCalculator.activeRange(period, settings.monthlyBudget, settings.startDate)
    val days = remember(period, state) { BudgetCalculator.simulatePeriod(period, settings, state.expenses, state.today, state.planned) }
    val spent = days.sumOf { it.spent }
    val third = if (isCurrent) {
        val elapsed = daysBetween(range.from, state.today) + 1
        "ペースより" to ((range.budget.toLong() * elapsed / range.days).toInt() - spent)
    } else if (period.start.isAfter(state.today)) {
        "予定の合計" to state.planned.filter { it.date in period }.sumOf { it.amount }
    } else {
        "残った額" to (range.budget - spent)
    }
    val isFuturePeriod = period.start.isAfter(state.today)
    val sel = LocalDate.parse(selected).let { s ->
        when {
            s in period && !s.isBefore(range.from) -> s
            isCurrent -> state.today
            isFuturePeriod -> maxOf(period.start, range.from)
            else -> period.end
        }
    }
    val selIsFuture = sel.isAfter(state.today)
    val dayInfo = days.firstOrNull { it.date == sel }
    val dayItems = remember(state, sel) { state.expenses.filter { it.date == sel }.sortedBy { it.createdAt } }
    val plannedByDate = remember(state.planned) { state.planned.groupBy { it.date } }
    val dayPlans = plannedByDate[sel].orEmpty()
    val hasOlder = settings.startDate?.isBefore(period.start) == true
    val hasLaterPlans = state.planned.any { it.date.isAfter(period.end) }
    var addingPlan by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current

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
                    DbIconButton(DbIcons.Right, "次の月度", onClick = { anchor = period.end.plusDays(1).toString() }, enabled = !isCurrent && !isFuturePeriod || hasLaterPlans)
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
        val totals = BudgetCalculator.categoryTotals(state.expenses, range.from, minOf(period.end, state.today))
        if (totals.isNotEmpty()) item {
            val sum = totals.sumOf { it.second }.coerceAtLeast(1)
            var showAll by remember { mutableStateOf(false) }
            Panel(Modifier.padding(bottom = 18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("カテゴリ別", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    // 1本の横棒をカテゴリの割合で塗り分ける
                    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(99.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        totals.forEach { (cat, amount) ->
                            Box(Modifier.weight(amount.toFloat().coerceAtLeast(1f)).fillMaxSize().background(Color(cat.color)))
                        }
                    }
                    (if (showAll) totals else totals.take(4)).forEach { (cat, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(cat.color)))
                            Spacer(Modifier.width(8.dp))
                            Text(cat.label, color = c.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text("${amount * 100 / sum}%", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(end = 10.dp))
                            Text(formatYen(amount), color = c.ink, fontSize = 13.sp, style = MonoStyle)
                        }
                    }
                    if (totals.size > 4) {
                        Text(
                            if (showAll) "閉じる" else "ほか${totals.size - 4}件を見る",
                            Modifier.clip(RoundedCornerShape(8.dp)).clickable { showAll = !showAll }.padding(vertical = 2.dp),
                            color = c.healthy, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        if (period.end.isBefore(state.today)) item {
            Row(
                Modifier.padding(bottom = 16.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.ink)
                    .clickable(role = Role.Button) { openRecap(period.end) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${period.name}の振り返りカードを見る", color = c.appBg, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Icon(DbIcons.Right, null, tint = c.appBg, modifier = Modifier.size(18.dp))
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
                                if (d != null) {
                                    DayCell(d, isFirst = d.date == period.start, today = state.today, selected = d.date == sel, planned = plannedByDate[d.date].orEmpty().sumOf { it.amount }) {
                                        selected = d.date.toString()
                                    }
                                }
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
                Legend(c.accent, "予定")
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
                Text(sel.longJa(), color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (selIsFuture) {
                    if (dayPlans.isNotEmpty()) Text("予定 ${formatYen(dayPlans.sumOf { it.amount })}", color = c.muted, fontSize = 12.sp)
                } else if (dayInfo != null) {
                    Text("${formatYen(dayInfo.spent)} / 予算 ${formatYen(dayInfo.budget)}", color = c.muted, fontSize = 12.sp)
                }
            }
        }
        items(dayPlans, key = { "plan-" + it.id }) { p ->
            Row(
                Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accent.copy(alpha = 0.14f))
                    .border(1.dp, c.accent.copy(alpha = 0.6f), RoundedCornerShape(16.dp)).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("予定", Modifier.clip(RoundedCornerShape(6.dp)).background(c.accent).padding(horizontal = 6.dp, vertical = 2.dp), color = c.accentInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(10.dp))
                Text(p.label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                Text(formatYen(p.amount), color = c.ink, fontSize = 15.sp, style = MonoStyle)
                if (p.isRecurring) {
                    Text("毎月", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                } else {
                    DbIconButton(DbIcons.Close, "予定を削除", onClick = { viewModel.deletePlanned(p.id) }, tint = c.muted, modifier = Modifier.size(40.dp))
                }
            }
        }
        if (selIsFuture) {
            if (dayPlans.isEmpty()) item { EmptyBox("この日の予定はありません") }
        } else if (dayItems.isEmpty()) item { EmptyBox("この日の支出はありません") }
        items(dayItems, key = { it.id }) { e ->
            SwipeableExpense(
                onDelete = {
                    scope.launch {
                        viewModel.deleteExpense(e.id)?.let { removed ->
                            messenger.show("${Categories.of(removed.categoryId).label} ${formatYen(removed.amount)} を削除しました", "元に戻す") {
                                viewModel.restoreExpense(removed)
                            }
                        }
                    }
                },
                onDuplicate = {
                    viewModel.addExpense(e.amount, e.categoryId, e.memo, state.today)
                    messenger.show("今日の支出として ${formatYen(e.amount)} を記録しました")
                },
                modifier = Modifier.animateItem().padding(bottom = 8.dp),
            ) {
                ExpenseItem(
                    e,
                    subtitle = e.memo ?: " ",
                    onClick = { openSheet(SheetTarget.Edit(e)) },
                    actionIcon = DbIcons.Right,
                    actionDescription = "編集",
                    onAction = { openSheet(SheetTarget.Edit(e)) },
                )
            }
        }
        item {
            Row(
                Modifier.padding(top = 4.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, c.line, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { if (selIsFuture) addingPlan = true else openSheet(SheetTarget.Add(sel)) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(DbIcons.Plus, null, tint = c.muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (selIsFuture) "この日に予定を追加" else "この日に支出を追加", color = c.muted, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (addingPlan) {
        PlannedSheet(state.today, initialDate = sel, onDismiss = { addingPlan = false }) { label, amount, date ->
            viewModel.addPlanned(label, amount, date)
            messenger.show("${date.md()}の「$label」を予定に入れました")
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
private fun DayCell(d: DaySummary, isFirst: Boolean, today: LocalDate, selected: Boolean, planned: Int, onClick: () -> Unit) {
    val c = Db.colors
    val future = d.state == DayState.FUTURE
    val off = d.state == DayState.INACTIVE
    // 先の日も、予定を見たり追加したりするために選べる
    val enabled = !off
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
            .alpha(if (future && planned == 0) 0.55f else if (off) 0.4f else 1f)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(if (d.date == today) 2.dp else 1.dp, border, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = d.date.longJa(); this.selected = selected }
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = if (selected) c.appBg else c.ink, fontSize = 12.sp, lineHeight = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        val amountText = when {
            off -> ""
            future -> if (planned > 0) formatNumber(planned) else ""
            else -> formatNumber(d.spent)
        }
        val amountColor = when {
            selected -> c.appBg
            future && planned > 0 -> if (c.isDark) c.accent else c.warning
            else -> c.muted
        }
        Text(amountText, color = amountColor, fontSize = 9.5.sp, lineHeight = 10.sp, fontFamily = MonoFamily, maxLines = 1, letterSpacing = (-0.3).sp)
        val dot = when (d.state) {
            DayState.UNDER -> c.great
            DayState.OVER -> c.over
            DayState.TODAY -> if (d.spent <= d.budget) c.great else c.over
            else -> Color.Transparent
        }
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (dot != Color.Transparent) Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            if (planned > 0) Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent))
            if (dot == Color.Transparent && planned == 0) Box(Modifier.size(6.dp))
        }
    }
}
