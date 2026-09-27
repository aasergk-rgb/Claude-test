package com.daybudget.app.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.Categories
import com.daybudget.app.domain.Category
import com.daybudget.app.ui.AppState
import com.daybudget.app.ui.LocalMessenger
import com.daybudget.app.ui.MainViewModel
import com.daybudget.app.ui.components.CategoryIcon
import com.daybudget.app.ui.components.DbIconButton
import com.daybudget.app.ui.components.FormSheet
import com.daybudget.app.ui.components.LabelField
import com.daybudget.app.ui.components.Panel
import com.daybudget.app.ui.components.TopBar
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db

private const val MAX_VISIBLE = 12

/** カテゴリの名前・色・アイコン・並び順・表示を変える */
@Composable
fun CategoriesScreen(state: AppState.Ready, viewModel: MainViewModel, onBack: () -> Unit) {
    val c = Db.colors
    val messenger = LocalMessenger.current
    val list = state.categories.ifEmpty { Categories.DEFAULTS }.sortedBy { it.order }
    var editing by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }
    val visibleCount = list.count { !it.hidden }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 32.dp).navigationBarsPadding()) {
        TopBar("カテゴリ", left = { DbIconButton(DbIcons.Left, "戻る", onClick = onBack) })
        Text("タップで名前・色・アイコンを変えられます。使わないカテゴリは非表示にできます。", color = c.muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
        Panel(Modifier.padding(top = 4.dp)) {
            list.forEachIndexed { i, cat ->
                if (i > 0) HorizontalDivider(color = c.line)
                Row(
                    Modifier.fillMaxWidth().clickable { editing = cat }.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.alpha(if (cat.hidden) 0.4f else 1f)) { CategoryIcon(cat, 34.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(cat.label, color = if (cat.hidden) c.faint else c.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (cat.hidden) Text("非表示", color = c.faint, fontSize = 11.sp)
                    }
                    DbIconButton(DbIcons.Left, "上へ", onClick = { viewModel.moveCategory(cat.id, up = true) }, enabled = i > 0, tint = c.muted, modifier = Modifier.size(40.dp).rotateArrow(90f))
                    DbIconButton(DbIcons.Left, "下へ", onClick = { viewModel.moveCategory(cat.id, up = false) }, enabled = i < list.lastIndex, tint = c.muted, modifier = Modifier.size(40.dp).rotateArrow(-90f))
                }
            }
        }
        Row(
            Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.5.dp, c.line, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) {
                    if (visibleCount >= MAX_VISIBLE) messenger.show("表示できるのは${MAX_VISIBLE}個までです。使わないものを非表示にしてください")
                    else adding = true
                }
                .padding(14.dp),
            horizontalArrangement = Arrangement.Center,
        ) { Text("＋ カテゴリを追加", color = c.muted, fontWeight = FontWeight.Bold) }
    }

    editing?.let { cat ->
        CategoryEditSheet(
            initial = cat,
            onDismiss = { editing = null },
            onSave = { updated -> viewModel.saveCategory(updated) },
            onDelete = if (cat.builtIn) null else ({ viewModel.deleteCategory(cat.id); messenger.show("「${cat.label}」を削除しました。これまでの記録は「その他」として表示されます") }),
            canHide = cat.builtIn,
            visibleCount = visibleCount,
        )
    }
    if (adding) {
        CategoryEditSheet(
            initial = null,
            onDismiss = { adding = false },
            onSave = { new -> viewModel.addCategory(new.label, new.icon, new.color) },
            onDelete = null,
            canHide = false,
            visibleCount = visibleCount,
        )
    }
}

private fun Modifier.rotateArrow(deg: Float) = this.rotate(deg)

@Composable
private fun CategoryEditSheet(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit,
    onDelete: (() -> Unit)?,
    canHide: Boolean,
    visibleCount: Int,
) {
    val c = Db.colors
    var name by rememberSaveable { mutableStateOf(initial?.label ?: "") }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: "heart") }
    var color by rememberSaveable { mutableLongStateOf(initial?.color ?: Categories.PALETTE[10]) }
    var hidden by rememberSaveable { mutableStateOf(initial?.hidden ?: false) }
    val preview = Category(initial?.id ?: "new", name.ifBlank { "新しいカテゴリ" }, color, icon)

    FormSheet(
        if (initial == null) "カテゴリを追加" else "カテゴリを編集",
        onDismiss,
        canSave = name.isNotBlank(),
        onSave = { onSave((initial ?: preview).copy(label = name.trim(), icon = icon, color = color, hidden = hidden)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(preview, 44.dp)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.weight(1f)) { LabelField(name, { name = it }, "名前（例：推し活）") }
        }
        Text("アイコン", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Categories.ICONS.chunked(8).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { key ->
                        val on = key == icon
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (on) Color(color).copy(alpha = 0.18f) else c.surface2)
                                .border(1.5.dp, if (on) Color(color) else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable(role = Role.RadioButton) { icon = key },
                            contentAlignment = Alignment.Center,
                        ) { Icon(DbIcons.categoryIcon(key), null, tint = if (on) Color(color) else c.muted, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
        Text("色", color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Categories.PALETTE.forEach { col ->
                val on = col == color
                Box(
                    Modifier.weight(1f).aspectRatio(1f).clip(CircleShape).background(Color(col))
                        .border(if (on) 3.dp else 0.dp, if (on) c.ink else Color.Transparent, CircleShape)
                        .clickable(role = Role.RadioButton) { color = col },
                )
            }
        }
        if (canHide) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("入力画面に出さない", color = c.ink, fontSize = 14.sp)
                    Text("これまでの記録はそのまま残ります", color = c.muted, fontSize = 12.sp)
                }
                Switch(
                    hidden,
                    { if (!it && visibleCount >= MAX_VISIBLE) Unit else hidden = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.appBg),
                )
            }
        }
        if (onDelete != null) {
            Text(
                "このカテゴリを削除",
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onDelete(); onDismiss() }.padding(vertical = 12.dp),
                color = c.over, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            )
        }
    }
}
