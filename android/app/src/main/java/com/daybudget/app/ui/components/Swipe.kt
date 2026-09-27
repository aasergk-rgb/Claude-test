package com.daybudget.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daybudget.app.ui.icons.DbIcons
import com.daybudget.app.ui.theme.Db

/** 左にスワイプで削除、右にスワイプでもう一度記録 */
@Composable
fun SwipeableExpense(onDelete: () -> Unit, onDuplicate: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = Db.colors
    val state = rememberSwipeToDismissBoxState(positionalThreshold = { it * 0.35f })
    // スワイプが確定したときに一度だけ実行する（ドラッグ中に何度も呼ばれないように）
    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> onDelete()
            SwipeToDismissBoxValue.StartToEnd -> {
                onDuplicate()
                // 複製は行を残すので元の位置へ戻す
                state.reset()
            }
            SwipeToDismissBoxValue.Settled -> Unit
        }
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            val dir = state.dismissDirection
            val bg by animateColorAsState(
                when (dir) {
                    SwipeToDismissBoxValue.EndToStart -> c.over
                    SwipeToDismissBoxValue.StartToEnd -> c.great
                    else -> Color.Transparent
                },
                label = "swipeBg",
            )
            Row(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(bg).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (dir == SwipeToDismissBoxValue.StartToEnd) Arrangement.Start else Arrangement.End,
            ) {
                if (dir == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(DbIcons.Plus, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("もう一度記録", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                } else if (dir == SwipeToDismissBoxValue.EndToStart) {
                    Text("削除", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(DbIcons.Trash, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        },
    ) { content() }
}
