package com.daybudget.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.ui.LocalMessenger
import com.daybudget.app.ui.components.DbIconButton
import com.daybudget.app.ui.components.PrimaryButton
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db

private val ROWS = listOf(
    Triple("日割り計算・支出の記録", true, true),
    Triple("ウィジェット（小・ダーク）", true, true),
    Triple("ウィジェット全サイズ・着せ替え", false, true),
    Triple("貯金プールモード", false, true),
    Triple("CSVで書き出し", false, true),
)

@Composable
fun PaywallScreen(onClose: () -> Unit) {
    val c = Db.colors
    val messenger = LocalMessenger.current
    val notReady = { messenger.show("購入はまだ準備中です") }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { DbIconButton(DbIcons.Close, "閉じる", onClick = onClose) }
            Box(
                Modifier.padding(top = 8.dp, bottom = 20.dp).size(84.dp).rotate(-6f).clip(RoundedCornerShape(26.dp)).background(c.accent).align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) { Icon(DbIcons.Spark, null, tint = c.accentInk, modifier = Modifier.size(40.dp)) }
            Text("一度の購入で、ずっと快適に。", Modifier.fillMaxWidth(), color = c.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("月額課金はありません。1回買えば、この先ずっと使えます。", Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), color = c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)

            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp))) {
                Row(Modifier.background(c.surface2)) {
                    Text("機能", Modifier.weight(1f).padding(start = 14.dp, top = 10.dp, bottom = 10.dp), color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("無料", Modifier.width(56.dp).padding(vertical = 10.dp), color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Pro", Modifier.width(56.dp).background(c.accent.copy(alpha = 0.12f)).padding(vertical = 10.dp), color = c.ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                ROWS.forEach { (name, free, pro) ->
                    HorizontalDivider(color = c.line)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, Modifier.weight(1f).padding(start = 14.dp, top = 11.dp, bottom = 11.dp), color = c.ink, fontSize = 13.sp)
                        Mark(free, Modifier.width(56.dp))
                        Mark(pro, Modifier.width(56.dp).background(c.accent.copy(alpha = 0.12f)))
                    }
                }
            }
        }
        Column(Modifier.padding(vertical = 16.dp)) {
            PrimaryButton("¥1,000 で永久解放（買い切り）", onClick = notReady)
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                FooterLink("購入を復元", notReady)
                FooterLink("利用規約") { messenger.show("利用規約は準備中です") }
                FooterLink("プライバシー") { messenger.show("プライバシーポリシーは準備中です") }
            }
        }
    }
}

@Composable
private fun Mark(on: Boolean, modifier: Modifier) {
    Box(modifier.height(42.dp), contentAlignment = Alignment.Center) {
        Icon(if (on) DbIcons.Check else DbIcons.Minus, if (on) "あり" else "なし", tint = if (on) Db.colors.great else Db.colors.faint, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(text, Modifier.clickable(onClick = onClick).padding(4.dp), color = Db.colors.muted, fontSize = 12.sp, textDecoration = TextDecoration.Underline)
}

