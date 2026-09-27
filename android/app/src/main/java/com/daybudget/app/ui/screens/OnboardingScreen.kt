package com.daybudget.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.CLOSING_END_OF_MONTH
import com.daybudget.app.domain.Period
import com.daybudget.app.domain.Status
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.md
import com.daybudget.app.ui.components.BudgetInput
import com.daybudget.app.ui.components.ClosingDayPicker
import com.daybudget.app.ui.components.GhostButton
import com.daybudget.app.ui.components.PreviewBox
import com.daybudget.app.ui.components.PrimaryButton
import com.daybudget.app.ui.components.StatusChip
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import java.time.LocalDate

@Composable
fun OnboardingScreen(today: LocalDate, onDone: (budget: Int, closingDay: Int) -> Unit) {
    val c = Db.colors
    var step by rememberSaveable { mutableIntStateOf(0) }
    var budget by rememberSaveable { mutableIntStateOf(50_000) }
    var closingDay by rememberSaveable { mutableIntStateOf(CLOSING_END_OF_MONTH) }
    var showAll by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(3) { i ->
                val w by animateDpAsState(if (i == step) 40.dp else 22.dp, label = "step")
                Box(Modifier.width(w).height(5.dp).clip(RoundedCornerShape(9.dp)).background(if (i == step) c.ink else c.line))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).animateContentSize()) {
            when (step) {
                0 -> Intro()
                1 -> {
                    Heading("1か月に自由に使えるお金は？", "家賃や光熱費など、毎月決まって出ていくお金を除いた金額です。")
                    BudgetInput(budget) { budget = it }
                    Spacer(Modifier.height(18.dp))
                    PreviewBox(listOf("1日あたり" to "約 ${formatYen(budget / 30)}"))
                }
                else -> {
                    val period = Period.of(today, closingDay)
                    val range = BudgetCalculator.activeRange(period, budget, today)
                    Heading("毎月何日で区切りますか？", "選んだ日の翌日から、新しい月度が始まります。給料日の前日を選ぶ人が多いです。")
                    ClosingDayPicker(closingDay, showAll) { closingDay = it }
                    if (!showAll) {
                        TextButton(onClick = { showAll = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("ほかの日を選ぶ", color = c.healthy, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(Modifier.height(18.dp))
                    }
                    PreviewBox(
                        buildList {
                            add("今の月度" to "${period.start.md()}〜${period.end.md()}（${period.days}日間）")
                            if (range.days < period.days) add("今月度の予算（今日から${range.days}日分）" to formatYen(range.budget))
                            add("1日あたり" to formatYen(range.budget / range.days))
                        },
                    )
                }
            }
        }
        Column(Modifier.padding(top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when (step) {
                0 -> PrimaryButton("はじめる", onClick = { step = 1 })
                1 -> {
                    PrimaryButton("次へ", onClick = { step = 2 }, enabled = budget > 0)
                    GhostButton("戻る", onClick = { step = 0 })
                }
                else -> {
                    PrimaryButton("この設定ではじめる", onClick = { onDone(budget, closingDay) })
                    GhostButton("戻る", onClick = { step = 1 })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(DbIcons.Shield, null, tint = c.muted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("入力した内容はこの端末の外に送信されません", color = c.muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(title: String, lead: String) {
    Text(title, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = Db.colors.ink)
    Spacer(Modifier.height(8.dp))
    Text(lead, color = Db.colors.muted, fontSize = 14.sp)
    Spacer(Modifier.height(22.dp))
}

@Composable
private fun Intro() {
    val c = Db.colors
    Text("今日あといくら使えるか、ひと目でわかる。", fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = c.ink)
    Column(
        Modifier.padding(vertical = 22.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(26.dp)).padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("今日使えるお金", color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        BigAmount(2450, c.ink, maxSize = 64)
        StatusChip(Status.GREAT.label, c.great)
    }
    Point(DbIcons.Shield, "口座連携なし", "記録はこの端末の中だけに保存します")
    Point(DbIcons.Tap, "金額とカテゴリだけで記録", "レシート撮影も細かい分類もいりません")
    Point(DbIcons.Roll, "使わなかった分は明日に回る", "節約した日の翌日は、使える額が増えます")
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    val c = Db.colors
    Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.ink.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(body, color = c.muted, fontSize = 12.sp)
        }
    }
}
