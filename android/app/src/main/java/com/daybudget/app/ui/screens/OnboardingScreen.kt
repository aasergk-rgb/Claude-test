package com.daybudget.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.CLOSING_END_OF_MONTH
import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.MAX_PRESETS
import com.daybudget.app.domain.OnboardingChoices
import com.daybudget.app.domain.PRESET_SUGGESTIONS
import com.daybudget.app.domain.Period
import com.daybudget.app.domain.Status
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.md
import com.daybudget.app.notify.DailyNotifications
import com.daybudget.app.ui.components.BudgetInput
import com.daybudget.app.ui.components.CategoryIcon
import com.daybudget.app.ui.components.ClosingDayPicker
import com.daybudget.app.ui.components.GhostButton
import com.daybudget.app.ui.components.PreviewBox
import com.daybudget.app.ui.components.PrimaryButton
import com.daybudget.app.ui.components.StatusChip
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.MonoStyle
import java.time.LocalDate

/** 初回設定の手順。「今月の残り」は月度の途中から始めるときだけ出す */
private enum class Page { INTRO, BUDGET, CLOSING, REMAINING, PRESETS, NOTIFY, PRO }

/** 初回設定: 紹介 → 予算 → 締め日 →（今月の残り）→ よく使う金額 → お知らせ → Pro版のおすすめ */
@Composable
fun OnboardingScreen(today: LocalDate, onDone: (choices: OnboardingChoices, openPaywall: Boolean) -> Unit) {
    val c = Db.colors
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var budget by rememberSaveable { mutableIntStateOf(50_000) }
    var closingDay by rememberSaveable { mutableIntStateOf(CLOSING_END_OF_MONTH) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    // 選んだ候補の番号（最初は上の3つ）
    var picked by rememberSaveable { mutableStateOf(listOf(0, 1, 2)) }
    var morning by rememberSaveable { mutableStateOf(true) }
    var evening by rememberSaveable { mutableStateOf(true) }
    var notifyGranted by rememberSaveable { mutableStateOf(true) }
    // 今月の残り（null なら日割りで自動計算）。予算や締め日を変えたら自動に戻す
    var customRemaining by rememberSaveable { mutableStateOf<Int?>(null) }
    var remainingKey by rememberSaveable { mutableStateOf("$budget:$closingDay") }
    LaunchedEffect(budget, closingDay) {
        val key = "$budget:$closingDay"
        if (key != remainingKey) {
            remainingKey = key
            customRemaining = null
        }
    }

    val period = Period.of(today, closingDay)
    val prorated = BudgetCalculator.activeRange(period, budget, today)
    val midPeriod = prorated.days < period.days
    val pages = Page.entries.filter { it != Page.REMAINING || midPeriod }
    val page = pages[step.coerceIn(0, pages.lastIndex)]
    fun go(p: Page) { step = pages.indexOf(p) }

    fun choices() = OnboardingChoices(
        monthlyBudget = budget,
        closingDay = closingDay,
        presets = picked.sorted().map { PRESET_SUGGESTIONS[it] },
        morningNotify = morning && notifyGranted,
        eveningNotify = evening && notifyGranted,
        firstPeriodBudget = if (midPeriod) customRemaining else null,
    )

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifyGranted = granted
        go(Page.PRO)
    }

    fun afterNotifyStep() {
        val wantsAny = morning || evening
        if (wantsAny && Build.VERSION.SDK_INT >= 33 && !DailyNotifications.hasPermission(context)) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            go(Page.PRO)
        }
    }

    // 端末の「戻る」は、アプリを閉じずに前の手順へ
    BackHandler(enabled = step > 0) { step -= 1 }

    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 26.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
            repeat(pages.size) { i ->
                val w by animateDpAsState(if (i == step) 36.dp else 16.dp, label = "step")
                Box(Modifier.width(w).height(5.dp).clip(RoundedCornerShape(9.dp)).background(if (i <= step) c.ink else c.line))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).animateContentSize()) {
            when (page) {
                Page.INTRO -> Intro()
                Page.BUDGET -> {
                    Heading("1か月に自由に使えるお金は？", "家賃や光熱費など、毎月決まって出ていくお金を除いた金額です。")
                    BudgetInput(budget) { budget = it }
                    Spacer(Modifier.height(18.dp))
                    PreviewBox(listOf("1日あたり" to "約 ${formatYen(budget / 30)}"))
                }
                Page.CLOSING -> {
                    val range = prorated
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
                            if (range.days < period.days) add("今月度の残り（今日から${range.days}日）" to "次で決めます")
                            else add("1日あたり" to formatYen(range.budget / range.days))
                        },
                    )
                }
                Page.REMAINING -> {
                    val remaining = customRemaining ?: prorated.budget
                    Heading(
                        "今月の残りはいくらですか？",
                        "${period.start.md()}〜${period.end.md()} の月度は、今日を入れてあと${prorated.days}日です。今月もう使った分を除いた、自由に使えるお金の残りを入れてください。",
                    )
                    BudgetInput(remaining, showPresets = false) { customRemaining = it }
                    Spacer(Modifier.height(14.dp))
                    if (customRemaining != null && customRemaining != prorated.budget) {
                        TextButton(onClick = { customRemaining = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("日割りの ${formatYen(prorated.budget)} に戻す", color = c.healthy, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "わからなければそのままで大丈夫です（月の予算 ${formatYen(budget)} を日割りした額）。",
                            color = c.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    PreviewBox(
                        listOf(
                            "今月度の残り（${prorated.days}日）" to formatYen(remaining),
                            "1日あたり" to formatYen(remaining / prorated.days),
                            "来月度から" to "月の予算 ${formatYen(budget)}",
                        ),
                    )
                    Text(
                        "この金額はここでだけ決められます。あとから変えるには「すべてのデータを消去」して最初から設定します。",
                        color = c.faint, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp),
                    )
                }
                Page.PRESETS -> {
                    Heading("よく使う出費はどれですか？", "選んだものは、ホーム画面とウィジェットから1タップで記録できます。あとで設定から変えられます。")
                    PRESET_SUGGESTIONS.forEachIndexed { i, (label, amount, cat) ->
                        val on = i in picked
                        val full = picked.size >= MAX_PRESETS && !on
                        Row(
                            Modifier.padding(bottom = 8.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .background(if (on) c.ink.copy(alpha = 0.06f) else c.surface)
                                .border(1.5.dp, if (on) c.ink else c.line, RoundedCornerShape(14.dp))
                                .clickable(enabled = !full, role = Role.Checkbox) { picked = if (on) picked - i else picked + i }
                                .semantics { selected = on }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CategoryIcon(Categories.of(cat), 32.dp)
                            Text(label, color = if (full) c.faint else c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(formatYen(amount), color = if (full) c.faint else c.muted, style = MonoStyle, fontSize = 14.sp)
                            Box(
                                Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(if (on) c.ink else Color.Transparent)
                                    .border(1.5.dp, if (on) c.ink else c.line, RoundedCornerShape(7.dp)),
                                contentAlignment = Alignment.Center,
                            ) { if (on) Icon(DbIcons.Check, null, tint = c.appBg, modifier = Modifier.size(14.dp)) }
                        }
                    }
                    Text("${picked.size}/${MAX_PRESETS}個まで選べます", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Page.NOTIFY -> {
                    Heading("お知らせを受け取りますか？", "続けるコツは、毎日「今日いくら使えるか」を目にすることです。")
                    NotifyChoice("朝のお知らせ（8:00）", "例：「今日は ¥1,850 使えます」", morning) { morning = it }
                    Spacer(Modifier.height(10.dp))
                    NotifyChoice("夜のリマインド（21:00）", "記録忘れと、明日使える額の見込み", evening) { evening = it }
                    Text("時刻はあとで設定から変えられます。", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                }
                Page.PRO -> ProPitch()
            }
        }
        Column(Modifier.padding(top = 20.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val next = { step = (step + 1).coerceAtMost(pages.lastIndex) }
            val prev = { step = (step - 1).coerceAtLeast(0) }
            when (page) {
                Page.INTRO -> PrimaryButton("はじめる", onClick = next)
                Page.BUDGET -> {
                    PrimaryButton("次へ", onClick = next, enabled = budget > 0)
                    GhostButton("戻る", onClick = prev)
                }
                Page.CLOSING, Page.REMAINING -> {
                    PrimaryButton("次へ", onClick = next)
                    GhostButton("戻る", onClick = prev)
                }
                Page.PRESETS -> {
                    PrimaryButton(if (picked.isEmpty()) "選ばずに次へ" else "次へ", onClick = next)
                    GhostButton("戻る", onClick = prev)
                }
                Page.NOTIFY -> {
                    PrimaryButton(if (morning || evening) "次へ" else "受け取らずに次へ", onClick = ::afterNotifyStep)
                    GhostButton("戻る", onClick = prev)
                }
                Page.PRO -> {
                    PrimaryButton("Pro版にする（¥1,000）", onClick = { onDone(choices(), true) }, icon = DbIcons.Spark)
                    GhostButton("まずは無料ではじめる", onClick = { onDone(choices(), false) })
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
private fun NotifyChoice(title: String, example: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val c = Db.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp))
            .clickable(role = Role.Switch) { onChange(!on) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(example, color = c.muted, fontSize = 12.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(on, onChange, colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.appBg))
    }
}

@Composable
private fun ProPitch() {
    val c = Db.colors
    Box(
        Modifier.padding(start = 6.dp, top = 6.dp, bottom = 18.dp).size(72.dp).rotate(-6f).clip(RoundedCornerShape(22.dp)).background(c.accent),
        contentAlignment = Alignment.Center,
    ) { Icon(DbIcons.Spark, null, tint = c.accentInk, modifier = Modifier.size(34.dp)) }
    Text("Pro版で、もっと続けやすく。", fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = c.ink)
    Spacer(Modifier.height(8.dp))
    Text("無料のままでも使えます。気に入ったらいつでも設定から購入できます。", color = c.muted, fontSize = 14.sp)
    Spacer(Modifier.height(20.dp))
    ProPoint(DbIcons.Calendar, "大きいウィジェットと着せ替え5種", "今日の支出と節約ペースまでホーム画面に")
    ProPoint(DbIcons.Roll, "貯金プールモード", "使わなかった分を「今月の貯金」として貯める")
    ProPoint(DbIcons.Spark, "振り返りカードのデザイン追加", "月末のシェア画像をサンセット・ミントでも")
    ProPoint(DbIcons.Check, "CSVで書き出し", "表計算ソフトで自由に集計")
    Row(
        Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.accent.copy(alpha = 0.16f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("¥1,000", color = c.ink, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, style = MonoStyle)
        Spacer(Modifier.width(10.dp))
        Text("買い切り・月額なし\n一度の購入でずっと使えます", color = c.ink, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun ProPoint(icon: ImageVector, title: String, body: String) {
    val c = Db.colors
    Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.accent.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, color = c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(body, color = c.muted, fontSize = 12.sp)
        }
    }
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
