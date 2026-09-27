package com.daybudget.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.formatNumber
import com.daybudget.app.domain.longJa
import com.daybudget.app.ui.theme.Db
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Db.colors.appBg,
    unfocusedContainerColor = Db.colors.appBg,
    unfocusedBorderColor = Db.colors.line,
    focusedBorderColor = Db.colors.healthy,
    focusedTextColor = Db.colors.ink,
    unfocusedTextColor = Db.colors.ink,
    cursorColor = Db.colors.ink,
)

@Composable
fun LabelField(value: String, onChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value, { if (it.length <= 20) onChange(it) },
        placeholder = { Text(placeholder, color = Db.colors.faint) },
        singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun AmountField(value: Int, onChange: (Int) -> Unit, placeholder: String = "金額") {
    OutlinedTextField(
        if (value > 0) formatNumber(value) else "",
        { s -> onChange(s.filter(Char::isDigit).take(7).toIntOrNull() ?: 0) },
        prefix = { Text("¥ ", color = Db.colors.muted) },
        placeholder = { Text(placeholder, color = Db.colors.faint) },
        singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(title: String, onDismiss: () -> Unit, canSave: Boolean, onSave: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Db.colors.surface) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, color = Db.colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            content()
            PrimaryButton("保存", enabled = canSave, onClick = {
                onSave()
                scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
            })
        }
    }
}

/** よく使う金額を追加する */
@Composable
fun PresetSheet(onDismiss: () -> Unit, onSave: (label: String, amount: Int, categoryId: String) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableIntStateOf(0) }
    var cat by rememberSaveable { mutableStateOf(Categories.CAFE.id) }
    FormSheet("よく使う金額を追加", onDismiss, canSave = label.isNotBlank() && amount > 0, onSave = { onSave(label.trim(), amount, cat) }) {
        LabelField(label, { label = it }, "名前（例：コーヒー）")
        AmountField(amount, { amount = it })
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Categories.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { c ->
                        val on = c.id == cat
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (on) Color(c.color).copy(alpha = 0.09f) else Color.Transparent)
                                .border(1.5.dp, if (on) Color(c.color) else Db.colors.line, RoundedCornerShape(12.dp))
                                .clickable(role = Role.RadioButton) { cat = c.id }.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CategoryIcon(c, 26.dp)
                            Text(c.label, color = Db.colors.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/** 先の大きな出費を予約する */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedSheet(today: LocalDate, onDismiss: () -> Unit, onSave: (label: String, amount: Int, date: LocalDate) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableIntStateOf(0) }
    var date by remember { mutableStateOf(today.plusDays(1)) }
    var picking by remember { mutableStateOf(false) }
    FormSheet("大きな出費を予定に入れる", onDismiss, canSave = label.isNotBlank() && amount > 0, onSave = { onSave(label.trim(), amount, date) }) {
        Text(
            "その日まで毎日の予算から少しずつ取り分けて、当日に使えるようにします。",
            color = Db.colors.muted, fontSize = 12.sp, lineHeight = 18.sp,
        )
        LabelField(label, { label = it }, "内容（例：飲み会）")
        AmountField(amount, { amount = it })
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Db.colors.line, RoundedCornerShape(12.dp))
                .clickable { picking = true }.padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Text("日付", color = Db.colors.muted, modifier = Modifier.weight(1f))
            Text(date.longJa(), color = Db.colors.ink, fontWeight = FontWeight.Bold)
        }
    }
    if (picking) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return d.isAfter(today) && !d.isAfter(today.plusYears(1))
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    picking = false
                }) { Text("決定") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("キャンセル") } },
        ) { DatePicker(picker, showModeToggle = false) }
    }
}
