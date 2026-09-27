package com.daybudget.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.BuildConfig
import com.daybudget.app.domain.AppTheme
import com.daybudget.app.domain.CarryoverMode
import com.daybudget.app.domain.Csv
import com.daybudget.app.domain.Period
import com.daybudget.app.domain.WidgetTheme
import com.daybudget.app.domain.closingLabel
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.md
import com.daybudget.app.ui.AppState
import com.daybudget.app.ui.LocalMessenger
import com.daybudget.app.ui.MainViewModel
import com.daybudget.app.ui.Routes
import com.daybudget.app.ui.components.BudgetInput
import com.daybudget.app.ui.components.ClosingDayPicker
import com.daybudget.app.ui.components.DbIconButton
import com.daybudget.app.ui.components.Panel
import com.daybudget.app.ui.components.PreviewBox
import com.daybudget.app.ui.components.PrimaryButton
import com.daybudget.app.ui.components.ProTag
import com.daybudget.app.ui.components.SegmentedControl
import com.daybudget.app.ui.components.TopBar
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.DisplayFamily
import com.daybudget.app.ui.theme.MonoStyle
import com.daybudget.app.widget.WidgetColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter

private val MODE_HELP = mapOf(
    CarryoverMode.DISTRIBUTE to "使わなかった分は、翌日以降の予算に均等に上乗せされます。使いすぎた分は翌日以降から均等に差し引かれます。",
    CarryoverMode.SAVINGS to "毎日の予算は固定です。使わなかった分は翌日に回さず、今月の貯金として貯まっていきます。",
)

private enum class Editor { BUDGET, CLOSING }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: AppState.Ready, viewModel: MainViewModel, onBack: () -> Unit, navigate: (String) -> Unit) {
    val c = Db.colors
    val s = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    var editor by remember { mutableStateOf<Editor?>(null) }
    var draft by remember { mutableIntStateOf(0) }
    var confirmReset by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val csv = Csv.fromExpenses(viewModel.allExpenses())
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(csv.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            messenger.show(if (ok) "CSVを書き出しました" else "書き出せませんでした。保存先を変えてもう一度お試しください")
        }
    }

    fun requirePro(block: () -> Unit) = if (s.isPro) block() else navigate(Routes.PAYWALL)

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 32.dp).navigationBarsPadding(),
    ) {
        TopBar("設定", left = { DbIconButton(DbIcons.Left, "戻る", onClick = onBack) })

        Row(
            Modifier.padding(top = 4.dp, bottom = 22.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.ink)
                .clickable(enabled = !s.isPro) { navigate(Routes.PAYWALL) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(if (s.isPro) DbIcons.Check else DbIcons.Spark, null, tint = c.accentInk)
            }
            Column(Modifier.weight(1f)) {
                Text(if (s.isPro) "Pro版 有効" else "DayBudget Pro", color = c.appBg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(if (s.isPro) "すべての機能が使えます" else "着せ替え・貯金プール・CSV", color = c.appBg.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            if (!s.isPro) {
                Text("¥1,000", color = c.appBg, style = MonoStyle, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(c.appBg.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }

        Group("予算") {
            Item("月の予算", formatYen(s.monthlyBudget)) { draft = s.monthlyBudget; editor = Editor.BUDGET }
            HorizontalDivider(color = c.line)
            Item("締め日", closingLabel(s.closingDay)) { draft = s.closingDay; editor = Editor.CLOSING }
            HorizontalDivider(color = c.line)
            Column(Modifier.padding(14.dp)) {
                SegmentedControl(
                    listOf(CarryoverMode.DISTRIBUTE to "均等配分", CarryoverMode.SAVINGS to "貯金プール"),
                    s.carryoverMode,
                    onSelect = { m ->
                        if (m == CarryoverMode.SAVINGS && !s.isPro) navigate(Routes.PAYWALL)
                        else viewModel.updateSettings { it.copy(carryoverMode = m) }
                    },
                    badge = { it == CarryoverMode.SAVINGS && !s.isPro },
                )
                Text(MODE_HELP.getValue(s.carryoverMode), color = c.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
            }
        }

        Group("表示") {
            Column(Modifier.padding(14.dp)) {
                SegmentedControl(
                    listOf(AppTheme.SYSTEM to "自動", AppTheme.LIGHT to "ライト", AppTheme.DARK to "ダーク"),
                    s.theme,
                    onSelect = { t -> viewModel.updateSettings { it.copy(theme = t) } },
                )
            }
        }

        Group("ホーム画面ウィジェット") {
            Column(Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetTheme.entries.forEach { t ->
                        val locked = t != WidgetTheme.DARK && !s.isPro
                        val on = s.widgetTheme == t
                        val wc = WidgetColors.of(t)
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).border(1.5.dp, if (on) c.ink else c.line, RoundedCornerShape(14.dp))
                                .clickable(role = Role.RadioButton) { if (locked) navigate(Routes.PAYWALL) else viewModel.updateSettings { it.copy(widgetTheme = t) } }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(9.dp)).background(Color(wc.background))
                                    .border(1.dp, if (t == WidgetTheme.WHITE) c.line else Color.Transparent, RoundedCornerShape(9.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("¥2,450", color = Color(wc.number), fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                if (locked) Icon(DbIcons.Lock, "Pro版で使えます", tint = Color(wc.text), modifier = Modifier.align(Alignment.TopEnd).padding(5.dp).size(13.dp))
                            }
                            Text(t.label, color = c.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    "ホーム画面を長押し →「ウィジェット」→ DayBudget から追加できます。" + if (s.isPro) "" else "無料版は小サイズ・ダークテーマのみです。",
                    color = c.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 10.dp, start = 2.dp),
                )
            }
        }

        Group("データ") {
            Item("CSVで書き出す", null, pro = !s.isPro) {
                requirePro { exportLauncher.launch("daybudget-${state.today.format(DateTimeFormatter.BASIC_ISO_DATE)}.csv") }
            }
            HorizontalDivider(color = c.line)
            Item("すべてのデータを消去", null, danger = true) { confirmReset = true }
            if (BuildConfig.DEBUG) {
                HorizontalDivider(color = c.line)
                Item("開発用: Pro版を${if (s.isPro) "無効" else "有効"}にする", null) { viewModel.updateSettings { it.copy(isPro = !it.isPro) } }
            }
        }

        Text(
            "DayBudget ${BuildConfig.VERSION_NAME}\n記録はこの端末の中だけに保存され、外部には送信されません。",
            Modifier.fillMaxWidth().padding(top = 4.dp),
            color = c.faint, fontSize = 11.sp, lineHeight = 18.sp, textAlign = TextAlign.Center,
        )
    }

    editor?.let { kind ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { editor = null }, sheetState = sheetState, containerColor = c.surface) {
            Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (kind == Editor.BUDGET) "月の予算" else "締め日", color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (kind == Editor.BUDGET) {
                    BudgetInput(draft) { draft = it }
                } else {
                    ClosingDayPicker(draft, showAll = true) { draft = it }
                    val p = Period.of(state.today, draft)
                    PreviewBox(listOf("今の月度" to "${p.start.md()}〜${p.end.md()}（${p.days}日間）"), background = c.appBg)
                }
                PrimaryButton("保存", enabled = draft > 0, onClick = {
                    val v = draft
                    viewModel.updateSettings { if (kind == Editor.BUDGET) it.copy(monthlyBudget = v) else it.copy(closingDay = v) }
                    scope.launch { sheetState.hide() }.invokeOnCompletion { editor = null }
                    messenger.show("保存しました")
                })
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("すべてのデータを消去しますか？") },
            text = { Text("支出の記録と予算の設定がすべて消えます。元に戻すことはできません。") },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; viewModel.resetAll() }) { Text("消去する", color = c.over, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("キャンセル") } },
            containerColor = c.surface,
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title, color = Db.colors.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
    Panel(Modifier.padding(bottom = 22.dp)) { content() }
}

@Composable
private fun Item(label: String, value: String?, pro: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val c = Db.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (danger) c.over else c.ink, fontSize = 14.sp, fontWeight = if (danger) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (pro) { ProTag(); Spacer(Modifier.width(4.dp)) }
        if (value != null) Text(value, color = c.muted, style = MonoStyle, fontSize = 13.sp)
        if (!danger) Icon(DbIcons.Right, null, tint = c.muted, modifier = Modifier.size(16.dp))
    }
}
