package com.daybudget.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 左にスワイプで削除、右にスワイプでもう一度記録。
 *
 * スワイプの位置はこの行が画面にある間だけ持つ（記録の id ごとには保存しない）。
 * id ごとに保存すると、「元に戻す」で同じ記録が戻ったときにスワイプ済みの状態で戻って
 * すぐ消えたり、一覧に行が増えたときに別の行がずれて表示されたりするため。
 */
@Composable
fun SwipeableExpense(onDelete: () -> Unit, onDuplicate: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Db.colors
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val offset = remember { Animatable(0f) }
    var width by remember { mutableIntStateOf(0) }
    val deleteNow by rememberUpdatedState(onDelete)
    val duplicateNow by rememberUpdatedState(onDuplicate)
    val threshold = width * 0.35f

    fun release() = scope.launch {
        val x = offset.value
        when {
            width > 0 && x <= -threshold -> {
                offset.animateTo(-width.toFloat(), tween(160))
                deleteNow()
                // 削除されずに行が残った場合（何かで失敗したとき）は元の位置に戻す
                delay(1_500)
                offset.snapTo(0f)
            }
            width > 0 && x >= threshold -> {
                duplicateNow()
                offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
            }
            else -> offset.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f))
        }
    }

    val x = offset.value
    val past = width > 0 && abs(x) >= threshold
    Box(
        modifier
            .fillMaxWidth()
            .onSizeChanged { width = it.width }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("削除") { deleteNow(); true },
                    CustomAccessibilityAction("もう一度記録") { duplicateNow(); true },
                )
            },
    ) {
        if (x != 0f) {
            val bg = when {
                x < 0 && past -> c.over
                x > 0 && past -> c.great
                else -> c.surface2
            }
            val fg = if (past) Color.White else c.muted
            Row(
                Modifier.matchParentSize().clip(RoundedCornerShape(16.dp)).background(bg).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (x > 0) Arrangement.Start else Arrangement.End,
            ) {
                if (x > 0) {
                    Icon(DbIcons.Plus, null, tint = fg, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("もう一度記録", color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else {
                    Text("削除", color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(DbIcons.Trash, null, tint = fg, modifier = Modifier.size(20.dp))
                }
            }
        }
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    var wasPast = false
                    detectHorizontalDragGestures(
                        onDragStart = { wasPast = false },
                        onDragEnd = { release() },
                        onDragCancel = { release() },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            val next = (offset.value + amount).coerceIn(-size.width.toFloat(), size.width.toFloat())
                            scope.launch { offset.snapTo(next) }
                            val nowPast = abs(next) >= size.width * 0.35f
                            // しきい値を越えた瞬間に軽く振動して「離せば確定」を伝える
                            if (nowPast != wasPast) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                wasPast = nowPast
                            }
                        },
                    )
                },
        ) { content() }
    }
}
