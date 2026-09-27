package com.daybudget.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.Categories
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db

@Composable
fun DbIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, tint: Color = Db.colors.ink) {
    Box(
        modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (enabled) tint else tint.copy(alpha = 0.3f), modifier = Modifier.size(22.dp))
    }
}

/** 左右にボタン、中央にタイトル */
@Composable
fun TopBar(title: String, subtitle: String? = null, left: (@Composable () -> Unit)? = null, right: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(44.dp)) { left?.invoke() }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Db.colors.ink)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Db.colors.muted)
        }
        Box(Modifier.width(44.dp)) { right?.invoke() }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.13f)).padding(start = 9.dp, end = 11.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    val c = Db.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(if (enabled) 8.dp else 0.dp, RoundedCornerShape(18.dp), ambientColor = c.accent, spotColor = c.accent)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) c.accent else c.accent.copy(alpha = 0.45f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = c.accentInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = c.accentInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Db.colors.muted, fontWeight = FontWeight.Bold) }
}

@Composable
fun CategoryIcon(category: Categories, size: Dp = 40.dp) {
    val color = Color(category.color)
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) { Icon(DbIcons.category(category), null, tint = color, modifier = Modifier.size(size * 0.48f)) }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(title, Modifier.weight(1f), color = Db.colors.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (trailing != null) Text(trailing, color = Db.colors.muted, fontSize = 13.sp)
    }
}

/** 枠線付きの面（設定のグループ、サマリーなど） */
@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Db.colors.surface).border(1.dp, Db.colors.line, RoundedCornerShape(16.dp)),
        content = content,
    )
}

@Composable
fun <T> SegmentedControl(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, badge: (T) -> Boolean = { false }) {
    val c = Db.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Row(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) c.surface else Color.Transparent)
                    .clickable { onSelect(value) }
                    .semantics { role = Role.RadioButton; this.selected = on },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = if (on) c.ink else c.muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (badge(value)) {
                    Spacer(Modifier.width(5.dp))
                    ProTag()
                }
            }
        }
    }
}

@Composable
fun ProTag() {
    Text(
        "PRO",
        Modifier.clip(RoundedCornerShape(5.dp)).background(Db.colors.accent).padding(horizontal = 5.dp, vertical = 1.dp),
        color = Db.colors.accentInk,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
    )
}

/** 横並びの行（アイコン・本文・右端） */
@Composable
fun ListRow(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Db.colors.surface)
            .border(1.dp, Db.colors.line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
fun EmptyBox(text: String) {
    Box(
        Modifier.fillMaxWidth().border(1.5.dp, Db.colors.line, RoundedCornerShape(16.dp)).padding(22.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Db.colors.muted, fontSize = 13.sp, textAlign = TextAlign.Center) }
}
