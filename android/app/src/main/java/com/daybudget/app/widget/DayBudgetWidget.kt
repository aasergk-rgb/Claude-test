package com.daybudget.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.daybudget.app.DayBudgetApp
import com.daybudget.app.MainActivity
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.CarryoverMode
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.PlannedExpense
import com.daybudget.app.domain.QuickPreset
import com.daybudget.app.domain.Status
import com.daybudget.app.domain.UserSettings
import com.daybudget.app.domain.WidgetTheme
import com.daybudget.app.domain.formatNumber
import com.daybudget.app.domain.formatSignedYen
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.md
import com.daybudget.app.ui.Routes
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

/** ウィジェットに表示する値（DBから計算） */
data class WidgetData(
    val onboarded: Boolean,
    val isPro: Boolean,
    val theme: WidgetTheme,
    val today: LocalDate,
    val todayAvailable: Int,
    val dailyBudget: Int,
    val progress: Float,
    val status: Status,
    val recent: List<Expense>,
    val paceLabel: String,
    val paceValue: Int,
    /** 1タップで記録するボタン（最大3つ） */
    val presets: List<QuickPreset> = emptyList(),
    val streak: Int = 0,
) {
    companion object {
        fun from(
            settings: UserSettings?,
            expenses: List<Expense>,
            today: LocalDate,
            presets: List<QuickPreset> = emptyList(),
            planned: List<PlannedExpense> = emptyList(),
        ): WidgetData {
            val s = settings ?: UserSettings()
            val snap = BudgetCalculator.computeToday(s, expenses, today, planned)
            val savings = s.carryoverMode == CarryoverMode.SAVINGS
            return WidgetData(
                onboarded = s.onboarded,
                isPro = s.isPro,
                // 着せ替えは Pro 版のみ
                theme = if (s.isPro) s.widgetTheme else WidgetTheme.DARK,
                today = today,
                todayAvailable = snap.todayAvailable,
                dailyBudget = snap.dailyBudget,
                progress = snap.progressRate.toFloat().coerceIn(0f, 1f),
                status = snap.status,
                recent = expenses.filter { it.date == today }.sortedByDescending { it.createdAt }.take(2),
                paceLabel = if (savings) "今月の貯金" else "今月の節約ペース",
                paceValue = if (savings) snap.savingsAmount else snap.pace,
                presets = presets.sortedBy { it.order }.take(3),
                streak = BudgetCalculator.underBudgetStreak(s, expenses, today, planned),
            )
        }
    }
}

class DayBudgetWidget : GlanceAppWidget() {
    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
        val LARGE = DpSize(250.dp, 230.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as DayBudgetApp).repository
        repo.loadCategories()
        val initial = WidgetData.from(repo.loadSettings(), repo.loadExpenses(), LocalDate.now(), repo.loadPresets(), repo.loadAllPlanned(LocalDate.now()))
        // ウィジェットの表示中はデータベースの変化を直接受け取って描き直す。
        // （最初に1回読むだけだと、表示中の更新で古い値が出たままになる）
        val updates = combine(repo.settings, repo.expenses, repo.presets, repo.planned, repo.recurring) { s, e, p, pl, rec ->
            val today = LocalDate.now()
            WidgetData.from(s, e, today, p, pl + rec.flatMap { it.asPlanned(today.minusYears(1), today.plusYears(1)) })
        }
        provideContent {
            val data by updates.collectAsState(initial)
            WidgetContent(context, data)
        }
    }
}

private fun color(v: Long) = ColorProvider(Color(v))

private fun openApp(context: Context, route: String? = null): Action = actionStartActivity(
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route)
    },
)

@Composable
private fun WidgetContent(context: Context, d: WidgetData) {
    val wc = WidgetColors.of(d.theme)
    val size = LocalSize.current
    val wide = size.width >= DayBudgetWidget.MEDIUM.width
    val tall = size.height >= DayBudgetWidget.LARGE.height
    Box(
        GlanceModifier.fillMaxSize().appWidgetBackground().cornerRadius(22.dp).background(Color(wc.background)).padding(14.dp)
            .clickable(openApp(context)),
    ) {
        if (!d.onboarded) {
            Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text("DayBudget", style = TextStyle(color = color(wc.text), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.height(4.dp))
                Text("タップして予算を設定", style = TextStyle(color = color(wc.sub), fontSize = 12.sp))
            }
            return@Box
        }
        when {
            // 大: 残額と詳細（Pro）、下に1タップ記録
            wide && tall -> Column(GlanceModifier.fillMaxSize()) {
                Row(GlanceModifier.fillMaxWidth().defaultWeight()) {
                    Summary(context, d, wc, GlanceModifier.defaultWeight().fillMaxHeight(), showAdd = false)
                    Divider(wc)
                    if (d.isPro) Details(d, wc, GlanceModifier.defaultWeight().fillMaxHeight())
                    else Locked(context, wc, GlanceModifier.defaultWeight().fillMaxHeight())
                }
                Spacer(GlanceModifier.height(12.dp))
                QuickRow(context, d, wc)
            }
            // 中: 残額と1タップ記録（無料）
            wide -> Row(GlanceModifier.fillMaxSize()) {
                Summary(context, d, wc, GlanceModifier.defaultWeight().fillMaxHeight(), showAdd = false)
                Divider(wc)
                QuickColumn(context, d, wc, GlanceModifier.defaultWeight().fillMaxHeight())
            }
            // 小: 残額と「＋」
            else -> Summary(context, d, wc, GlanceModifier.fillMaxSize(), showAdd = true)
        }
    }
}

@Composable
private fun Divider(wc: WidgetColors) {
    Spacer(GlanceModifier.width(12.dp))
    Box(GlanceModifier.width(1.dp).fillMaxHeight().background(Color(wc.divider))) {}
    Spacer(GlanceModifier.width(12.dp))
}

@Composable
private fun Summary(context: Context, d: WidgetData, wc: WidgetColors, modifier: GlanceModifier, showAdd: Boolean) {
    val statusColor = wc.status(d.status)
    val number = if (d.status == Status.OVER) statusColor else wc.number
    Column(modifier) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("今日使えるお金", GlanceModifier.defaultWeight(), style = TextStyle(color = color(wc.sub), fontSize = 11.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            if (showAdd) {
                Box(
                    GlanceModifier.width(26.dp).height(26.dp).cornerRadius(13.dp).background(Color(0xFFFFC53D)).clickable(openApp(context, Routes.ADD)),
                    contentAlignment = Alignment.Center,
                ) { Text("+", style = TextStyle(color = color(0xFF241A02), fontSize = 16.sp, fontWeight = FontWeight.Bold)) }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            (if (d.todayAvailable < 0) "−¥" else "¥") + formatNumber(kotlin.math.abs(d.todayAvailable)),
            style = TextStyle(color = color(number), fontSize = if (formatNumber(d.todayAvailable).length > 6) 24.sp else 30.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Spacer(GlanceModifier.defaultWeight())
        LinearProgressIndicator(d.progress, GlanceModifier.fillMaxWidth().height(6.dp), color = color(statusColor), backgroundColor = color(wc.track))
        Spacer(GlanceModifier.height(7.dp))
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(d.status.short, style = TextStyle(color = color(statusColor), fontSize = 10.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Text(if (d.streak >= 2) "${d.streak}日連続" else d.today.md(), style = TextStyle(color = color(wc.sub), fontSize = 10.sp))
        }
    }
}

@Composable
private fun QuickButton(context: Context, p: QuickPreset, wc: WidgetColors, modifier: GlanceModifier) {
    Box(
        modifier.height(34.dp).cornerRadius(12.dp).background(Color(wc.track))
            .clickable(actionRunCallback<QuickAddAction>(actionParametersOf(QuickAddAction.PRESET_ID to p.id))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${p.label} ${formatYen(p.amount)}",
            GlanceModifier.padding(horizontal = 8.dp),
            style = TextStyle(color = color(wc.text), fontSize = 11.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickColumn(context: Context, d: WidgetData, wc: WidgetColors, modifier: GlanceModifier) {
    Column(modifier) {
        Text("タップで記録", style = TextStyle(color = color(wc.sub), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(6.dp))
        if (d.presets.isEmpty()) {
            Text("アプリの設定で「よく使う金額」を追加できます", style = TextStyle(color = color(wc.sub), fontSize = 11.sp))
        }
        d.presets.forEachIndexed { i, p ->
            if (i > 0) Spacer(GlanceModifier.height(5.dp))
            QuickButton(context, p, wc, GlanceModifier.fillMaxWidth())
        }
    }
}

@Composable
private fun QuickRow(context: Context, d: WidgetData, wc: WidgetColors) {
    Row(GlanceModifier.fillMaxWidth()) {
        d.presets.forEachIndexed { i, p ->
            if (i > 0) Spacer(GlanceModifier.width(6.dp))
            QuickButton(context, p, wc, GlanceModifier.defaultWeight())
        }
        if (d.presets.isNotEmpty()) Spacer(GlanceModifier.width(6.dp))
        Box(
            GlanceModifier.width(40.dp).height(34.dp).cornerRadius(12.dp).background(Color(0xFFFFC53D)).clickable(openApp(context, Routes.ADD)),
            contentAlignment = Alignment.Center,
        ) { Text("+", style = TextStyle(color = color(0xFF241A02), fontSize = 16.sp, fontWeight = FontWeight.Bold)) }
    }
}

@Composable
private fun Details(d: WidgetData, wc: WidgetColors, modifier: GlanceModifier) {
    Column(modifier) {
        Text("今日の支出", style = TextStyle(color = color(wc.sub), fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(4.dp))
        if (d.recent.isEmpty()) {
            Text("まだありません", style = TextStyle(color = color(wc.sub), fontSize = 11.sp))
        }
        d.recent.forEach { e ->
            Row(GlanceModifier.fillMaxWidth().padding(bottom = 3.dp)) {
                Text(e.memo ?: Categories.of(e.categoryId).label, GlanceModifier.defaultWeight(), style = TextStyle(color = color(wc.text), fontSize = 11.sp), maxLines = 1)
                Text(formatYen(e.amount), style = TextStyle(color = color(wc.text), fontSize = 11.sp))
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(d.paceLabel, style = TextStyle(color = color(wc.sub), fontSize = 10.sp))
        Text(formatSignedYen(d.paceValue), style = TextStyle(color = color(wc.status(if (d.paceValue >= 0) Status.GREAT else Status.OVER)), fontSize = 15.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun Locked(context: Context, wc: WidgetColors, modifier: GlanceModifier) {
    Column(modifier.clickable(openApp(context, Routes.PAYWALL)), verticalAlignment = Alignment.CenterVertically) {
        Text("今日の支出と節約ペース", style = TextStyle(color = color(wc.text), fontSize = 12.sp, fontWeight = FontWeight.Bold))
        Spacer(GlanceModifier.height(4.dp))
        Text("Pro版で表示できます。タップして詳しく見る", style = TextStyle(color = color(wc.sub), fontSize = 11.sp))
    }
}
