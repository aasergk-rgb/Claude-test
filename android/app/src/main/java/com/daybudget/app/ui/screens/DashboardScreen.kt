package com.daybudget.app.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import com.daybudget.app.domain.isIncome
import com.daybudget.app.domain.isRecurring
import com.daybudget.app.domain.reachedMilestone
import com.daybudget.app.ui.UiPrefs
import com.daybudget.app.ui.components.LocalFly
import com.daybudget.app.ui.components.MilestoneCelebration
import com.daybudget.app.ui.components.RollingText
import com.daybudget.app.ui.components.SwipeableExpense
import com.daybudget.app.ui.components.centerInRoot
import com.daybudget.app.ui.rememberReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import com.daybudget.app.domain.Period
import com.daybudget.app.notify.DailyNotifications
import com.daybudget.app.ui.components.PlannedSheet
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
import com.daybudget.app.domain.Category
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
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val fly = LocalFly.current
    val context = LocalContext.current
    val prefs = remember { UiPrefs(context) }
    val reduce = rememberReduceMotion()
    val haptics = LocalHapticFeedback.current

    val snap = remember(state) { BudgetCalculator.computeToday(state.settings, state.expenses, state.today, state.planned) }
    val days = remember(state) { BudgetCalculator.simulatePeriod(snap.period, state.settings, state.expenses, state.today, state.planned) }
    val streak = remember(state) { BudgetCalculator.underBudgetStreak(state.settings, state.expenses, state.today, state.planned) }
    val upcoming = remember(state) {
        val end = Period.of(state.today, state.settings.closingDay).end
        state.planned.filter { !it.date.isBefore(state.today) && (!it.isRecurring || !it.date.isAfter(end)) }.sortedBy { it.date }
    }
    val todays = remember(state) { state.expenses.filter { it.date == state.today }.sortedByDescending { it.createdAt } }
    var addingPlan by remember { mutableStateOf(false) }
    var plansOpen by remember { mutableStateOf(prefs.plansExpanded) }

    fun onNotifyAnswer(granted: Boolean) {
        if (granted) {
            viewModel.updateSettings { it.copy(morningNotify = true, eveningNotify = true, notifyPromptDismissed = true) }
            messenger.show("毎朝と毎晩お知らせします。時刻は設定で変えられます")
        } else {
            viewModel.updateSettings { it.copy(notifyPromptDismissed = true) }
        }
    }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), ::onNotifyAnswer)

    // 月度が切り替わったら、前の月度の振り返りを一度だけ見せる
    LaunchedEffect(snap.period.start) {
        val prev = Period.of(snap.period.start.minusDays(1), state.settings.closingDay)
        val start = state.settings.startDate
        if (start != null && !start.isAfter(prev.end) && state.settings.lastRecapEnd != prev.end) {
            viewModel.updateSettings { it.copy(lastRecapEnd = prev.end) }
            navigate(Routes.recap(prev.end))
        }
    }

    // ── 節約ボーナスの演出（その日最初に開いたとき一度だけ） ──
    var heldBonus by remember { mutableIntStateOf(0) }
    var earnedBonus by remember { mutableIntStateOf(0) }
    val coinProgress = remember { Animatable(0f) }
    var coinFrom by remember { mutableStateOf<Offset?>(null) }
    LaunchedEffect(state.today) {
        val bonus = BudgetCalculator.savingsBonus(state.settings, state.expenses, state.today, state.planned)
        if (bonus <= 0 || prefs.bonusShownDate == state.today.toString()) return@LaunchedEffect
        prefs.bonusShownDate = state.today.toString()
        earnedBonus = bonus
        if (reduce) return@LaunchedEffect
        heldBonus = bonus
        coinProgress.snapTo(0f)
        delay(900)
        coinProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        heldBonus = 0
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    // ── 連続日数の節目のお祝い ──
    var celebrate by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(streak, state.today) {
        val m = reachedMilestone(streak) ?: return@LaunchedEffect
        val key = "${state.today.minusDays(streak.toLong())}:$m"
        if (prefs.celebratedStreak != key) {
            prefs.celebratedStreak = key
            delay(if (earnedBonus > 0) 2200 else 400)
            celebrate = m
        }
    }

    // 記録が着地したら大きな数字を少し弾ませる
    val bump = remember { Animatable(1f) }
    LaunchedEffect(fly.landings) {
        if (fly.landings == 0L || reduce) return@LaunchedEffect
        bump.snapTo(0.93f)
        bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }

    val statusColor by animateColorAsState(c.status(snap.status), tween(400), label = "status")
    val progress by animateFloatAsState(snap.progressRate.toFloat().coerceIn(0f, 1f), spring(dampingRatio = 0.8f, stiffness = 120f), label = "meter")
    val alarming = snap.status == Status.WARNING || snap.status == Status.OVER

    fun delete(e: Expense) = scope.launch {
        viewModel.deleteExpense(e.id)?.let { removed ->
            messenger.show("${Categories.of(removed.categoryId).label} ${formatYen(removed.amount)} を削除しました", "元に戻す") {
                viewModel.restoreExpense(removed)
            }
        }
    }

    fun duplicate(e: Expense) {
        viewModel.addExpense(e.amount, e.categoryId, e.memo, state.today)
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        fly.launch(e.amount, null)
        messenger.show("${e.memo ?: Categories.of(e.categoryId).label} ${formatYen(e.amount)} をもう一度記録しました")
    }

    Box(Modifier.fillMaxSize()) {
        // 状態の色を上部にうっすら敷く
        Box(
            Modifier.fillMaxWidth().height(360.dp)
                .background(Brush.verticalGradient(listOf(statusColor.copy(alpha = if (c.isDark) 0.16f else 0.10f), Color.Transparent))),
        )
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
                    BigAmount(
                        snap.todayAvailable - heldBonus,
                        if (alarming) statusColor else c.ink,
                        modifier = Modifier.padding(vertical = 4.dp)
                            .graphicsLayer { scaleX = bump.value; scaleY = bump.value }
                            .onGloballyPositioned { fly.heroCenter = it.centerInRoot() },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(snap.status.label, statusColor)
                        if (streak >= 2) StatusChip("予算内 ${streak}日連続", if (c.isDark) c.accent else c.accentInk)
                    }
                    // 節約ボーナスのコイン: 表示 → 大きな数字へ飛び込む → 「+¥◯ 昨日の節約」として残る
                    AnimatedVisibility(earnedBonus > 0, enter = fadeIn() + expandVertically(), exit = fadeOut()) {
                        Box(Modifier.padding(top = 12.dp)) {
                            val p = coinProgress.value
                            val hero = fly.heroCenter
                            val from = coinFrom
                            val landed = heldBonus == 0
                            Text(
                                if (landed) "昨日の節約で +${formatYen(earnedBonus)}" else "+${formatYen(earnedBonus)} 昨日の節約ぶん",
                                Modifier
                                    .onGloballyPositioned { if (coinFrom == null) coinFrom = it.centerInRoot() }
                                    .graphicsLayer {
                                        if (!landed && hero != null && from != null) {
                                            translationX = (hero.x - from.x) * p
                                            translationY = (hero.y - from.y) * p - sin(p * PI).toFloat() * 120f
                                            val sc = 1f - 0.5f * p
                                            scaleX = sc
                                            scaleY = sc
                                            alpha = if (p < 0.8f) 1f else (1f - p) / 0.2f
                                        }
                                    }
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(if (landed) c.great.copy(alpha = 0.14f) else c.accent)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (landed) c.great else c.accentInk,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
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
                    RollingText(formatYen(value), MonoStyle.copy(color = c.great, fontSize = 14.sp), increasing = true)
                }
            }
            if (state.presets.isNotEmpty()) item {
                Column(Modifier.padding(top = 20.dp)) {
                    SectionHeader("よく使う（タップで記録）", trailing = null)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.presets, key = { it.id }) { p ->
                            var chipCenter by remember { mutableStateOf<Offset?>(null) }
                            QuickChip(
                                "${p.label} ${formatYen(p.amount)}",
                                Categories.of(p.categoryId),
                                Modifier.onGloballyPositioned { chipCenter = it.centerInRoot() },
                            ) {
                                scope.launch {
                                    viewModel.quickAdd(p.id)?.let { added ->
                                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                        fly.launch(added.amount, chipCenter)
                                        messenger.show("${p.label} ${formatYen(added.amount)} を記録しました", "元に戻す") {
                                            scope.launch { viewModel.deleteExpense(added.id) }
                                        }
                                    }
                                }
                            }
                        }
                        item { QuickChip("編集", null) { navigate(Routes.SETTINGS) } }
                    }
                }
            }
            val showPrompt = !state.settings.notifyPromptDismissed && !state.settings.morningNotify && !state.settings.eveningNotify && state.expenses.isNotEmpty()
            if (showPrompt) item {
                Row(
                    Modifier.padding(top = 18.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.ink).padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("毎朝「今日使える額」をお知らせしますか？", color = c.appBg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("夜には記録忘れを教えます", color = c.appBg.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Text(
                        "オンにする",
                        Modifier.clip(RoundedCornerShape(10.dp)).background(c.accent).clickable {
                            if (Build.VERSION.SDK_INT >= 33 && !DailyNotifications.hasPermission(context)) notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else onNotifyAnswer(true)
                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = c.accentInk, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    DbIconButton(DbIcons.Close, "閉じる", onClick = { viewModel.updateSettings { it.copy(notifyPromptDismissed = true) } }, tint = c.appBg.copy(alpha = 0.7f), modifier = Modifier.size(40.dp))
                }
            }
            item {
                Column(Modifier.padding(top = 22.dp)) {
                    SectionHeader("${snap.period.name}の歩み", trailing = "残り ${formatYen(snap.periodRemaining)}")
                    Row(
                        Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(4.dp)).clickable { navigate(Routes.HISTORY) },
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        days.forEach { d ->
                            val (h, color) = when (d.state) {
                                DayState.TODAY -> 22.dp to c.accent
                                DayState.UNDER -> 12.dp to c.great.copy(alpha = 0.75f)
                                DayState.OVER -> 12.dp to c.over.copy(alpha = 0.8f)
                                DayState.INACTIVE -> 12.dp to c.line.copy(alpha = 0.4f)
                                DayState.FUTURE -> 12.dp to if (state.planned.any { it.date == d.date }) c.accent.copy(alpha = 0.45f) else c.line
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
            if (state.settings.carryoverMode == CarryoverMode.DISTRIBUTE) item {
                // 予定は1行にまとめ、タップで開く
                Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(14.dp))) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            if (upcoming.isEmpty()) addingPlan = true else { plansOpen = !plansOpen; prefs.plansExpanded = plansOpen }
                        }.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
                        Spacer(Modifier.width(10.dp))
                        if (upcoming.isEmpty()) {
                            Text("大きな出費を予定に入れる", color = c.ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Icon(DbIcons.Plus, null, tint = c.muted, modifier = Modifier.size(18.dp))
                        } else {
                            Text("予定 ${upcoming.size}件", color = c.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(" · ${formatYen(upcoming.sumOf { it.amount })} を取り分け中", color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val rot by animateFloatAsState(if (plansOpen) 90f else 0f, label = "chevron")
                            Icon(DbIcons.Right, if (plansOpen) "閉じる" else "開く", tint = c.muted, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = rot })
                        }
                    }
                    AnimatedVisibility(plansOpen && upcoming.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column {
                            upcoming.forEach { p ->
                                HorizontalDivider(color = c.line)
                                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 2.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (p.date == state.today) "今日" else p.date.md(), color = c.muted, fontSize = 12.sp, style = MonoStyle, modifier = Modifier.width(48.dp))
                                    Text(p.label, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(formatYen(p.amount), color = c.ink, fontSize = 14.sp, style = MonoStyle)
                                    if (p.isRecurring) {
                                        Text("毎月", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                                    } else {
                                        DbIconButton(DbIcons.Close, "予定を削除", onClick = { viewModel.deletePlanned(p.id) }, tint = c.faint, modifier = Modifier.size(40.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = c.line)
                            Text(
                                "＋ 予定を追加",
                                Modifier.fillMaxWidth().clickable { addingPlan = true }.padding(14.dp),
                                color = c.healthy, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            item { SectionHeader("今日の支出", Modifier.padding(top = 24.dp), trailing = "${todays.size}件") }
            if (todays.isEmpty()) item { EmptyBox("今日はまだ記録がありません") }
            items(todays, key = { it.id }) { e ->
                SwipeableExpense(
                    onDelete = { delete(e) },
                    onDuplicate = { duplicate(e) },
                    modifier = Modifier.animateItem().padding(bottom = 8.dp),
                ) {
                    ExpenseItem(
                        e,
                        subtitle = e.createdAt.atZone(ZoneId.systemDefault()).format(timeFormat) + (e.memo?.let { " · $it" } ?: ""),
                        onClick = { openSheet(SheetTarget.Edit(e)) },
                        actionIcon = DbIcons.Trash,
                        actionDescription = "削除",
                        onAction = { delete(e) },
                    )
                }
            }
            if (todays.isNotEmpty()) item {
                Text("左にスワイプで削除、右にスワイプでもう一度記録", color = c.faint, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), textAlign = TextAlign.Center)
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

        celebrate?.let { m -> MilestoneCelebration(m) { celebrate = null } }
    }

    if (addingPlan) {
        PlannedSheet(state.today, onDismiss = { addingPlan = false }) { label, amount, date ->
            viewModel.addPlanned(label, amount, date)
            plansOpen = true
            prefs.plansExpanded = true
            messenger.show("${date.md()}の「$label」を予定に入れました")
        }
    }
}

@Composable
private fun QuickChip(text: String, category: Category?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Db.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.4f, stiffness = 700f), label = "chip")
    Row(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale }.clip(RoundedCornerShape(99.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(99.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick).padding(start = if (category != null) 6.dp else 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (category != null) CategoryIcon(category, 24.dp)
        Text(text, color = if (category != null) c.ink else c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        if (e.isIncome) Text("+" + formatYen(-e.amount), color = c.great, fontSize = 15.sp, style = MonoStyle)
        else Text(formatYen(e.amount), color = c.ink, fontSize = 15.sp, style = MonoStyle)
        DbIconButton(actionIcon, actionDescription, onClick = onAction, tint = c.faint, modifier = Modifier.size(40.dp).semantics { contentDescription = actionDescription })
    }
}
