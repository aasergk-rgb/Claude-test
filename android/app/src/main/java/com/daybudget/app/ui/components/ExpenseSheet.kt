package com.daybudget.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.Expense
import com.daybudget.app.domain.formatNumber
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.longJa
import com.daybudget.app.ui.AppState
import com.daybudget.app.ui.LocalMessenger
import com.daybudget.app.ui.MainViewModel
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.DisplayFamily
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

sealed interface SheetTarget {
    data class Add(val date: LocalDate) : SheetTarget
    data class Edit(val expense: Expense) : SheetTarget
}

private const val MAX_DIGITS = 7
private val KEYS = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "del")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseSheet(target: SheetTarget, state: AppState.Ready, viewModel: MainViewModel, onDismiss: () -> Unit) {
    val c = Db.colors
    val editing = (target as? SheetTarget.Edit)?.expense
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val messenger = LocalMessenger.current

    var digits by rememberSaveable(target) { mutableStateOf(editing?.amount?.toString() ?: "") }
    var categoryId by rememberSaveable(target) { mutableStateOf(editing?.categoryId ?: state.settings.lastCategoryId) }
    var memo by rememberSaveable(target) { mutableStateOf(editing?.memo ?: "") }
    var date by remember(target) { mutableStateOf(editing?.date ?: (target as SheetTarget.Add).date) }
    var pickingDate by remember { mutableStateOf(false) }
    val amount = digits.toIntOrNull() ?: 0

    fun close() = scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }

    fun submit() {
        if (amount <= 0) return
        val cat = Categories.of(categoryId)
        val note = memo.trim().ifEmpty { null }
        if (editing != null) {
            viewModel.updateExpense(editing.id, amount, categoryId, note, date)
            messenger.show("変更しました")
        } else {
            viewModel.addExpense(amount, categoryId, note, date)
            messenger.show("${cat.label} ${formatYen(amount)} を記録しました")
        }
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        close()
    }

    fun remove() {
        val e = editing ?: return
        scope.launch {
            viewModel.deleteExpense(e.id)?.let { removed ->
                messenger.show("${Categories.of(removed.categoryId).label} ${formatYen(removed.amount)} を削除しました", "元に戻す") {
                    viewModel.restoreExpense(removed)
                }
            }
        }
        close()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(40.dp, 5.dp).clip(RoundedCornerShape(9.dp)).background(c.line)) },
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (editing != null) "支出を編集" else "支出を記録", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Spacer(Modifier.weight(1f))
                Text(
                    (if (date == state.today) "今日 " else "") + date.longJa(),
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(c.surface2)
                        .clickable { pickingDate = true }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                        .semantics { contentDescription = "日付を変更（${date.longJa()}）" },
                    color = c.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                DbIconButton(DbIcons.Close, "閉じる", onClick = { close() }, modifier = Modifier.size(36.dp))
            }

            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 26.sp, color = c.muted)) { append("¥") }
                    append(formatNumber(amount))
                },
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                letterSpacing = (-1.5).sp,
                color = if (amount > 0) c.ink else c.faint,
            )

            CategoryGrid(categoryId) { categoryId = it }

            OutlinedTextField(
                value = memo,
                onValueChange = { if (it.length <= 40) memo = it },
                placeholder = { Text("メモ（任意）例：コンビニ", color = c.faint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = c.appBg,
                    unfocusedContainerColor = c.appBg,
                    unfocusedBorderColor = c.line,
                    focusedBorderColor = c.healthy,
                    focusedTextColor = c.ink,
                    unfocusedTextColor = c.ink,
                    cursorColor = c.ink,
                ),
            )

            Keypad { key ->
                digits = when (key) {
                    "del" -> digits.dropLast(1)
                    else -> (digits + key).trimStart('0').take(MAX_DIGITS)
                }
                haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editing != null) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.over.copy(alpha = 0.11f))
                            .clickable(role = Role.Button, onClick = ::remove)
                            .semantics { contentDescription = "この支出を削除" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(DbIcons.Trash, null, tint = c.over) }
                }
                PrimaryButton(if (editing != null) "変更を保存" else "決定", onClick = ::submit, enabled = amount > 0, modifier = Modifier.weight(1f))
            }
        }
    }

    if (pickingDate) {
        val min = state.settings.startDate
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !d.isAfter(state.today) && (min == null || !d.isBefore(min))
                }

                override fun isSelectableYear(year: Int) = year <= state.today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("キャンセル") } },
        ) { DatePicker(pickerState, showModeToggle = false) }
    }
}

@Composable
private fun CategoryGrid(selected: String, onSelect: (String) -> Unit) {
    val c = Db.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Categories.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val on = cat.id == selected
                    val color = Color(cat.color)
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) color.copy(alpha = 0.09f) else Color.Transparent)
                            .border(1.5.dp, if (on) color else c.line, RoundedCornerShape(14.dp))
                            .clickable(role = Role.RadioButton) { onSelect(cat.id) }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CategoryIcon(cat, 30.dp)
                        Text(cat.label, color = c.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun Keypad(onKey: (String) -> Unit) {
    val c = Db.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KEYS.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .scale(if (pressed) 0.95f else 1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (pressed) c.line else c.surface2)
                            .clickable(interaction, indication = null, role = Role.Button) { onKey(key) }
                            .semantics { contentDescription = if (key == "del") "1文字消す" else key },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == "del") Icon(DbIcons.Backspace, null, tint = c.ink, modifier = Modifier.width(24.dp))
                        else Text(key, fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = c.ink)
                    }
                }
            }
        }
    }
}
