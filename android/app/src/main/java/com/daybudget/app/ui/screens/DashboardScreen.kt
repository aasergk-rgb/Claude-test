package com.daybudget.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.CarryoverMode
import com.daybudget.app.domain.DayState
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.Status
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.md
import com.daybudget.app.ui.AppState
import com.daybudget.app.ui.LocalMessenger
import com.daybudget.app.ui.MainViewModel
import com.daybudget.app.ui.Routes
import com.daybudget.app.ui.components.CategoryIcon
import com.daybudget.app.ui.components.DbIconButton
import com.daybudget.app.ui.components.EmptyBox
import com.daybudget.app.ui.components.ListRow
import com.daybudget.app.ui.components.PrimaryButton
import com.daybudget.app.ui.components.SectionHeader
import com.daybudget.app.ui.components.SheetTarget
import com.daybudget.app.ui.components.StatusChip
import com.daybudget.app.ui.components.TopBar
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.MonoFamily
import com.daybudget.app.ui.theme.MonoStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("H:mm")

@Composable
fun DashboardScreen(state: AppState.Ready, viewModel: MainViewModel, openSheet: (SheetTarget) -> Unit, navigate: (String) -> Unit) {
    val c = Db.colors
    val snap = remember(state) { BudgetCalculator.computeToday(state.settings, state.expenses, state.today) }
    val days = remember(state) { BudgetCalculator.simulatePeriod(snap.period, state.settings, state.expenses, state.today) }
    val todays = remember(state) { state.expenses.filter { it.date == state.today }.sortedByDescending { it.createdAt } }
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current

    val statusColor by animateColorAsState(c.status(snap.status), label = "status")
    val shown by animateIntAsState(snap.todayAvailable, tween(520), label = "amount")
    val progress by animateFloatAsState(snap.progressRate.toFloat().coerceIn(0f, 1f), tween(500), label = "meter")
    val alarming = snap.status == Status.WARNING || snap.status == Status.OVER

    fun delete(e: Expense) = scope.launch {
        viewModel.deleteExpense(e.id)?.let { removed ->
            messenger.show("${Categories.of(removed.categoryId).label} ${formatYen(removed.amount)} を削除しました", "元に戻す") {
                viewModel.restoreExpense(removed)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 140.dp),
        ) {
            item {
                TopBar(
                    title = snap.period.name,
                    subtitle = "${snap.period.start.md()}〜${snap.period.end.md()} · 残り${snap.remainingDays}日",
                    left = { DbIconButton(DbIcons.Sliders, "設定", onClick = { navigate(Routes.SETTINGS) }) },
                    right = { DbIconButton(DbIcons.Calendar, "履歴", onClick = { navigate(Routes.HISTORY) }) },
                )
            }
            item {
                Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("今日使えるお金", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    BigAmount(shown, if (alarming) statusColor else c.ink, modifier = Modifier.padding(vertical = 4.dp))
                    StatusChip(snap.status.label, statusColor)
                }
            }
            item {
                Column(Modifier.padding(top = 20.dp, start = 4.dp, end = 4.dp)) {
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(c.surface2)) {
                        Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(99.dp)).background(statusColor))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        LabeledValue("本日の支出", formatYen(snap.todaySpent))
                        Spacer(Modifier.weight(1f))
                        LabeledValue("本日の割当", formatYen(snap.dailyBudget))
                    }
                }
            }
            item {
                val (label, value) = when {
                    state.settings.carryoverMode == CarryoverMode.SAVINGS -> "今月の貯金（昨日まで）" to snap.savingsAmount
                    snap.tomorrowBudget != null -> "このあと使わなければ、明日は" to snap.tomorrowBudget
                    else -> "今日で${snap.period.name}はおしまい。残りは" to snap.periodRemaining
                }
                Row(
                    Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface)
                        .border(1.dp, c.line, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(formatYen(value), color = c.great, fontSize = 14.sp, style = MonoStyle)
                }
            }
            item {
                Column(Modifier.padding(top = 22.dp)) {
                    SectionHeader("${snap.period.name}の歩み", trailing = "残り ${formatYen(snap.periodRemaining)}")
                    Row(Modifier.fillMaxWidth().height(22.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                        days.forEach { d ->
                            val (h, color) = when (d.state) {
                                DayState.TODAY -> 22.dp to c.accent
                                DayState.UNDER -> 12.dp to c.great.copy(alpha = 0.75f)
                                DayState.OVER -> 12.dp to c.over.copy(alpha = 0.8f)
                                DayState.INACTIVE -> 12.dp to c.line.copy(alpha = 0.4f)
                                DayState.FUTURE -> 12.dp to c.line
                            }
                            Box(Modifier.weight(1f).height(h).clip(RoundedCornerShape(2.dp)).background(color))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(snap.period.start.md(), color = c.faint, fontSize = 11.sp)
                        Spacer(Modifier.weight(1f))
                        Text(snap.period.end.md(), color = c.faint, fontSize = 11.sp)
                    }
                }
            }
            item { SectionHeader("今日の支出", Modifier.padding(top = 24.dp), trailing = "${todays.size}件") }
            if (todays.isEmpty()) item { EmptyBox("今日はまだ記録がありません") }
            items(todays, key = { it.id }) { e ->
                ExpenseItem(
                    e,
                    subtitle = e.createdAt.atZone(ZoneId.systemDefault()).format(timeFormat) + (e.memo?.let { " · $it" } ?: ""),
                    onClick = { openSheet(SheetTarget.Edit(e)) },
                    actionIcon = DbIcons.Trash,
                    actionDescription = "削除",
                    onAction = { delete(e) },
                    modifier = Modifier.animateItem().padding(bottom = 8.dp),
                )
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.38f to c.appBg))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        ) {
            PrimaryButton("支出を記録", onClick = { openSheet(SheetTarget.Add(state.today)) }, icon = DbIcons.Plus)
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    val c = Db.colors
    Text(
        buildAnnotatedString {
            append("$label ")
            withStyle(SpanStyle(color = c.ink, fontFamily = MonoFamily, fontWeight = FontWeight.Medium)) { append(value) }
        },
        color = c.muted,
        fontSize = 12.sp,
    )
}

@Composable
fun ExpenseItem(
    e: Expense,
    subtitle: String,
    onClick: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionDescription: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Db.colors
    val cat = Categories.of(e.categoryId)
    ListRow(onClick, modifier) {
        CategoryIcon(cat)
        Column(Modifier.weight(1f)) {
            Text(cat.label, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatYen(e.amount), color = c.ink, fontSize = 15.sp, style = MonoStyle)
        DbIconButton(actionIcon, actionDescription, onClick = onAction, tint = c.faint, modifier = Modifier.size(40.dp).semantics { contentDescription = actionDescription })
    }
}
