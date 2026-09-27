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
import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import com.daybudget.app.domain.Categories
import com.daybudget.app.notify.DailyNotifications
import com.daybudget.app.ui.components.CategoryIcon
import com.daybudget.app.ui.components.PresetSheet
import com.daybudget.app.ui.components.RecurringSheet
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

private fun timeLabel(minutes: Int) = "%d:%02d".format(minutes / 60, minutes % 60)

private enum class NotifyKind { MORNING, EVENING }

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
    var addingPreset by remember { mutableStateOf(false) }
    var addingRecurring by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf<NotifyKind?>(null) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    var pendingNotify by remember { mutableStateOf<NotifyKind?>(null) }

    fun setNotify(kind: NotifyKind, on: Boolean) = viewModel.updateSettings {
        if (kind == NotifyKind.MORNING) it.copy(morningNotify = on, notifyPromptDismissed = true) else it.copy(eveningNotify = on, notifyPromptDismissed = true)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val kind = pendingNotify ?: return@rememberLauncherForActivityResult
        if (granted) setNotify(kind, true) else messenger.show("通知が許可されていません。端末の設定から許可できます")
        pendingNotify = null
    }
    fun toggleNotify(kind: NotifyKind, on: Boolean) {
        if (on && !DailyNotifications.hasPermission(context) && Build.VERSION.SDK_INT >= 33) {
            pendingNotify = kind
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            setNotify(kind, on)
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = viewModel.exportBackup()
                    context.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            messenger.show(if (ok) "バックアップを書き出しました" else "書き出せませんでした。保存先を変えてもう一度お試しください")
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            pendingImport = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            }
            if (pendingImport == null) messenger.show("ファイルを読み込めませんでした")
        }
    }

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

        Group("週末ブースト") {
            Column(Modifier.padding(14.dp)) {
                SegmentedControl(
                    listOf(100 to "なし", 125 to "1.25倍", 150 to "1.5倍", 200 to "2倍"),
                    s.weekendBoostPct,
                    onSelect = { v -> viewModel.updateSettings { it.copy(weekendBoostPct = v) } },
                )
                if (s.weekendBoostPct > 100) {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5 to "金", 6 to "土", 7 to "日").forEach { (d, label) ->
                            val on = d in s.weekendDays
                            Box(
                                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (on) c.ink else c.surface2)
                                    .clickable(role = Role.Checkbox) {
                                        val next = if (on) s.weekendDays - d else s.weekendDays + d
                                        if (next.isNotEmpty()) viewModel.updateSettings { it.copy(weekendDays = next) }
                                    }.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center,
                            ) { Text(label, color = if (on) c.appBg else c.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }
                    val p = Period.of(state.today, s.closingDay)
                    val weekend = p.dates().count { it.dayOfWeek.value in s.weekendDays }
                    val unit = s.monthlyBudget.toDouble() / (p.days - weekend + weekend * s.weekendBoostPct / 100.0)
                    Text(
                        "この月度なら 平日 約${formatYen(unit.toInt())}・週末 約${formatYen((unit * s.weekendBoostPct / 100).toInt())}（使い方で毎日変わります）",
                        color = c.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 10.dp, start = 2.dp),
                    )
                } else {
                    Text("週末に多めに使う人向け。週末の予算を平日より多く割り当てます。", color = c.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                }
            }
        }

        Group("毎月の決まった出費") {
            state.recurring.forEach { r ->
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(Categories.of(r.categoryId), 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.label, color = c.ink, fontSize = 14.sp)
                        Text("毎月${closingLabel(r.dayOfMonth)}", color = c.muted, fontSize = 12.sp)
                    }
                    Text(formatYen(r.amount), color = c.muted, style = MonoStyle, fontSize = 13.sp)
                    DbIconButton(DbIcons.Trash, "${r.label}を削除", onClick = { viewModel.deleteRecurring(r.id) }, tint = c.faint, modifier = Modifier.size(40.dp))
                }
                HorizontalDivider(color = c.line)
            }
            Item("＋ 追加する", null) { addingRecurring = true }
        }

        Group("カテゴリ") {
            Item("カテゴリを編集", "${Categories.entries.size}個") { navigate(Routes.CATEGORIES) }
        }

        Group("お知らせ") {
            NotifyRow("朝のお知らせ", "今日使える額", s.morningNotify, s.morningTime, { toggleNotify(NotifyKind.MORNING, it) }, { pickingTime = NotifyKind.MORNING })
            HorizontalDivider(color = c.line)
            NotifyRow("夜のリマインド", "記録忘れと明日の見込み", s.eveningNotify, s.eveningTime, { toggleNotify(NotifyKind.EVENING, it) }, { pickingTime = NotifyKind.EVENING })
        }

        Group("よく使う金額（1タップで記録）") {
            state.presets.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryIcon(Categories.of(p.categoryId), 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(p.label, color = c.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text(formatYen(p.amount), color = c.muted, style = MonoStyle, fontSize = 13.sp)
                    DbIconButton(DbIcons.Trash, "${p.label}を削除", onClick = { viewModel.deletePreset(p.id) }, tint = c.faint, modifier = Modifier.size(40.dp))
                }
                HorizontalDivider(color = c.line)
            }
            if (state.presets.size < 6) {
                Item("＋ 追加する", null) { addingPreset = true }
            } else {
                Text("6個まで登録できます", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(14.dp))
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
                WidgetTheme.entries.chunked(3).forEach { rowThemes ->
                Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowThemes.forEach { t ->
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
                    repeat(3 - rowThemes.size) { Spacer(Modifier.weight(1f)) }
                }
                }
                Text(
                    "ホーム画面を長押し →「ウィジェット」→ DayBudget から追加できます。小・中サイズは無料、大サイズの詳細表示と着せ替えは Pro 版です。",
                    color = c.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 10.dp, start = 2.dp),
                )
            }
        }

        Group("データ") {
            Item("CSVで書き出す", null, pro = !s.isPro) {
                requirePro { exportLauncher.launch("daybudget-${state.today.format(DateTimeFormatter.BASIC_ISO_DATE)}.csv") }
            }
            HorizontalDivider(color = c.line)
            Item("バックアップを書き出す", null) {
                backupLauncher.launch("daybudget-backup-${state.today.format(DateTimeFormatter.BASIC_ISO_DATE)}.json")
            }
            HorizontalDivider(color = c.line)
            Item("バックアップから復元", null) { restoreLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }
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

    if (addingRecurring) {
        RecurringSheet(state.today, onDismiss = { addingRecurring = false }) { label, amount, day, cat ->
            viewModel.addRecurring(label, amount, day, cat)
            messenger.show("「$label」を毎月の決まった出費に追加しました")
        }
    }

    if (addingPreset) {
        PresetSheet(onDismiss = { addingPreset = false }) { label, amount, cat ->
            viewModel.addPreset(label, amount, cat)
            messenger.show("「$label」を追加しました")
        }
    }

    pickingTime?.let { kind ->
        val current = if (kind == NotifyKind.MORNING) s.morningTime else s.eveningTime
        val timeState = rememberTimePickerState(current / 60, current % 60, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = null },
            title = { Text(if (kind == NotifyKind.MORNING) "朝のお知らせの時刻" else "夜のリマインドの時刻") },
            text = { TimePicker(timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val m = timeState.hour * 60 + timeState.minute
                    viewModel.updateSettings { if (kind == NotifyKind.MORNING) it.copy(morningTime = m) else it.copy(eveningTime = m) }
                    pickingTime = null
                }) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = null }) { Text("キャンセル") } },
            containerColor = c.surface,
        )
    }

    pendingImport?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("バックアップから復元しますか？") },
            text = { Text("今の記録と設定は、バックアップの内容に置き換わります。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val result = runCatching { viewModel.importBackup(json) }
                        messenger.show(result.fold({ "復元しました" }, { it.message ?: "復元できませんでした" }))
                    }
                }) { Text("復元する", color = c.over, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("キャンセル") } },
            containerColor = c.surface,
        )
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
private fun NotifyRow(title: String, sub: String, on: Boolean, minutes: Int, onToggle: (Boolean) -> Unit, onPickTime: () -> Unit) {
    val c = Db.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = c.ink, fontSize = 14.sp)
            Text(sub, color = c.muted, fontSize = 12.sp)
        }
        Text(
            timeLabel(minutes),
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onPickTime).padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (on) c.ink else c.faint, style = MonoStyle, fontSize = 14.sp,
        )
        Switch(on, onToggle, colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.appBg))
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
