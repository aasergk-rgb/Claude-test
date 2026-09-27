package com.daybudget.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.DisplayFamily

private fun milestoneMessage(days: Int) = when {
    days >= 100 -> "100日。もう立派な習慣です。"
    days >= 60 -> "2か月続きました。お金の不安、減ってきましたか？"
    days >= 30 -> "1か月ずっと予算内。すごいペースです。"
    days >= 14 -> "2週間連続。このまま月末まで行けそうです。"
    days >= 7 -> "1週間ずっと予算内。いい流れです。"
    else -> "3日続きました。まずは1週間を目指しましょう。"
}

/** 連続日数の節目のお祝い（紙吹雪とバッジ） */
@Composable
fun MilestoneCelebration(days: Int, onDismiss: () -> Unit) {
    val c = Db.colors
    val haptics = LocalHapticFeedback.current
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 300f))
    }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Confetti()
        Column(
            Modifier.padding(32.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(RoundedCornerShape(28.dp)).background(c.surface).padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(92.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$days", color = c.accentInk, fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 38.sp, lineHeight = 38.sp)
                    Text("日連続", color = c.accentInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("${days}日連続で予算内！", color = c.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(milestoneMessage(days), color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Box(Modifier.padding(top = 6.dp).fillMaxWidth()) { PrimaryButton("やったね", onClick = onDismiss) }
        }
    }
}
