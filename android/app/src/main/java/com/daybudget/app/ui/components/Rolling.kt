package com.daybudget.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import com.daybudget.app.ui.rememberReduceMotion

/**
 * 数字を1桁ずつスロットのように入れ替えて表示する。
 * 増えたときは下から、減ったときは上から新しい数字が入ってくる。
 */
@Composable
fun RollingText(text: String, style: TextStyle, increasing: Boolean, modifier: Modifier = Modifier) {
    val reduce = rememberReduceMotion()
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            // 右から数えた位置で覚える（桁が増えても一の位は同じ場所のまま）
            key(text.length - i) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        if (reduce) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            val spec = spring<IntOffset>(dampingRatio = 0.75f, stiffness = 380f)
                            (slideInVertically(spec) { h -> if (increasing) h else -h } + fadeIn()) togetherWith
                                (slideOutVertically(spec) { h -> if (increasing) -h else h } + fadeOut()) using SizeTransform(clip = true)
                        }
                    },
                    label = "digit",
                ) { c -> Text(c.toString(), style = style, maxLines = 1) }
            }
        }
    }
}
