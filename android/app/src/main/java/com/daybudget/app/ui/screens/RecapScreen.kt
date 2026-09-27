package com.daybudget.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.daybudget.app.domain.BudgetCalculator
import com.daybudget.app.domain.DayState
import com.daybudget.app.domain.Period
import com.daybudget.app.domain.PeriodRecap
import com.daybudget.app.domain.formatYen
import com.daybudget.app.domain.md
import com.daybudget.app.ui.AppState
import com.daybudget.app.ui.LocalMessenger
import com.daybudget.app.ui.Routes
import com.daybudget.app.ui.components.DbIconButton
import com.daybudget.app.ui.components.PrimaryButton
import com.daybudget.app.ui.components.ProTag
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.DisplayFamily
import com.daybudget.app.ui.theme.MonoFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/** 振り返りカードの配色 */
enum class RecapStyle(val label: String, val pro: Boolean, val top: Long, val bottom: Long, val ink: Long, val sub: Long, val good: Long, val bad: Long, val empty: Long) {
    NIGHT("ナイト", false, 0xFF141B2D, 0xFF0B0F1A, 0xFFF1F3F9, 0xFF9CA5B7, 0xFF3CC98A, 0xFFFF6B5E, 0xFF2B3243),
    SUNSET("サンセット", true, 0xFFF6C28B, 0xFFE0707A, 0xFF2A1420, 0xFF6B3A46, 0xFF1F7A55, 0xFF9C1F2E, 0x33FFFFFF),
    MINT("ミント", true, 0xFFE9F7F1, 0xFFBFE8D6, 0xFF0F2E25, 0xFF4F7468, 0xFF0E8F5E, 0xFFCF3B34, 0x220F2E25),
}

@Composable
fun RecapScreen(state: AppState.Ready, periodEnd: LocalDate, onClose: () -> Unit, navigate: (String) -> Unit) {
    val c = Db.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messenger = LocalMessenger.current
    val period = remember(periodEnd, state.settings.closingDay) { Period.of(periodEnd, state.settings.closingDay) }
    val recap = remember(state, period) { BudgetCalculator.recap(period, state.settings, state.expenses, state.today, state.planned) }
    var hide by rememberSaveable { mutableStateOf(false) }
    var style by rememberSaveable { mutableStateOf(RecapStyle.NIGHT) }
    val layer = rememberGraphicsLayer()

    fun share() = scope.launch {
        val ok = runCatching {
            val bitmap = layer.toImageBitmap().asAndroidBitmap()
            val uri = withContext(Dispatchers.IO) { saveForShare(context, bitmap) }
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "${recap.period.name}のふりかえり #DayBudget")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(send, "振り返りカードをシェア"))
        }.isSuccess
        if (!ok) messenger.show("画像を作れませんでした。もう一度お試しください")
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${recap.period.name}のふりかえり", color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 4.dp))
            DbIconButton(DbIcons.Close, "閉じる", onClick = onClose)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp).drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                },
            ) { RecapCard(recap, hide, style) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecapStyle.entries.forEach { s ->
                    val locked = s.pro && !state.settings.isPro
                    val on = s == style
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).border(1.5.dp, if (on) c.ink else c.line, RoundedCornerShape(12.dp))
                            .clickable(role = Role.RadioButton) { if (locked) navigate(Routes.PAYWALL) else style = s }
                            .padding(horizontal = 8.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(s.top), Color(s.bottom)))))
                        Text(s.label, color = c.ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                        if (locked) ProTag()
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("金額を隠す", color = c.ink, fontSize = 14.sp)
                    Text("シェアするときに金額を「¥•••」にします", color = c.muted, fontSize = 12.sp)
                }
                Switch(hide, { hide = it }, colors = SwitchDefaults.colors(checkedTrackColor = c.ink, checkedThumbColor = c.appBg))
            }
        }
        Box(Modifier.padding(vertical = 14.dp)) {
            PrimaryButton("画像をシェア", onClick = { share() })
        }
    }
}

/** SNS に載せやすい 4:5 のカード */
@Composable
fun RecapCard(r: PeriodRecap, hideAmounts: Boolean, style: RecapStyle) {
    val ink = Color(style.ink)
    val sub = Color(style.sub)
    val good = r.saved >= 0
    fun yen(v: Int) = if (hideAmounts) "¥•••" else formatYen(v)
    Column(
        Modifier.fillMaxWidth().aspectRatio(4f / 5f).clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(Color(style.top), Color(style.bottom))))
            .padding(24.dp),
    ) {
        Text("${r.period.name}のふりかえり", color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("${r.period.start.md()}〜${r.period.end.md()}", color = sub, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(if (good) "予算より" else "予算を", color = sub, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            if (hideAmounts) "¥•••" else formatYen(kotlin.math.abs(r.saved)),
            color = if (good) Color(style.good) else Color(style.bad),
            fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, letterSpacing = (-1.5).sp, maxLines = 1,
        )
        Text(if (good) "節約できました" else "オーバーしました。来月は取り返せます", color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        // 月度の毎日（緑=予算内・赤=超過）
        Row(Modifier.fillMaxWidth().height(20.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            r.days.forEach { d ->
                val col = when {
                    d.state == DayState.INACTIVE || d.state == DayState.FUTURE -> Color(style.empty)
                    d.spent <= d.budget -> Color(style.good)
                    else -> Color(style.bad)
                }
                Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(3.dp)).background(col))
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat("予算内の日", "${r.underDays}/${r.activeDays}日", ink, sub, Modifier.weight(1f))
            Stat("最長連続", "${r.longestStreak}日", ink, sub, Modifier.weight(1f))
            Stat("よく使った", r.topCategory?.label ?: "なし", ink, sub, Modifier.weight(1.3f))
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFFFFC53D)), contentAlignment = Alignment.Center) {
                Text("¥", color = Color(0xFF241A02), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(6.dp))
            Text("DayBudget", color = ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(if (hideAmounts) "" else "使った額 ${yen(r.spent)}", color = sub, fontSize = 11.sp, fontFamily = MonoFamily)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, ink: Color, sub: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = sub, fontSize = 11.sp)
        Text(value, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Start)
    }
}

private fun saveForShare(context: Context, bitmap: Bitmap): android.net.Uri {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "daybudget-recap.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
