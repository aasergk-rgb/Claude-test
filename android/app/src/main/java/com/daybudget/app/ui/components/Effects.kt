package com.daybudget.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.daybudget.app.domain.formatYen
import com.daybudget.app.ui.rememberReduceMotion
import com.daybudget.app.ui.theme.Db
import com.daybudget.app.ui.theme.DisplayFamily
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * 記録した金額を「今日使えるお金」の数字へ飛ばす演出。
 * 大きな数字の位置（hero）を覚えておき、飛ばす元の位置から放物線で飛ばす。
 */
@Stable
class FlyController {
    data class Flight(val id: Long, val amount: Int, val from: Offset, val to: Offset)

    var heroCenter by mutableStateOf<Offset?>(null)
    val flights = mutableStateListOf<Flight>()

    /** 着地した回数（大きな数字を弾ませるのに使う） */
    var landings by mutableLongStateOf(0L)
        private set

    private var nextId = 0L

    fun launch(amount: Int, from: Offset?) {
        val to = heroCenter ?: return
        flights += Flight(nextId++, amount, from ?: Offset(to.x, to.y + 900f), to)
    }

    fun landed(flight: Flight) {
        flights.remove(flight)
        landings++
    }
}

val LocalFly = staticCompositionLocalOf { FlyController() }

@Composable
fun FlightsOverlay(controller: FlyController) {
    val reduce = rememberReduceMotion()
    Box(Modifier.fillMaxSize()) {
        controller.flights.forEach { f ->
            androidx.compose.runtime.key(f.id) { FlyingAmount(f, reduce) { controller.landed(f) } }
        }
    }
}

@Composable
private fun FlyingAmount(f: FlyController.Flight, reduce: Boolean, onLanded: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val arc = with(LocalDensity.current) { androidx.compose.ui.unit.Dp(70f).toPx() }
    LaunchedEffect(f.id) {
        if (!reduce) progress.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        onLanded()
    }
    val p = progress.value
    val x = f.from.x + (f.to.x - f.from.x) * p
    val y = f.from.y + (f.to.y - f.from.y) * p - sin(p * PI).toFloat() * arc
    Text(
        "−" + formatYen(f.amount),
        Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(IntOffset((x - placeable.width / 2f).roundToInt(), (y - placeable.height / 2f).roundToInt()))
                }
            }
            .graphicsLayer {
                val s = 1f - 0.45f * p
                scaleX = s
                scaleY = s
                alpha = if (p < 0.75f) 1f else (1f - p) / 0.25f
            },
        color = Db.colors.over,
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
    )
}

private val CONFETTI_COLORS = listOf(0xFFFFC53D, 0xFF3CC98A, 0xFF6F9DFF, 0xFFFF6B5E, 0xFFFF7EB6, 0xFFB38CFF).map { Color(it) }

/** 紙吹雪（数秒で落ちきる） */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 90, durationMs: Long = 2600) {
    if (rememberReduceMotion()) return
    data class Piece(val x: Float, val vx: Float, val vy: Float, val spin: Float, val size: Float, val color: Color, val delay: Float)
    val pieces = remember {
        List(count) {
            Piece(
                x = Random.nextFloat(),
                vx = Random.nextFloat() * 0.5f - 0.25f,
                vy = 0.35f + Random.nextFloat() * 0.45f,
                spin = Random.nextFloat() * 720f - 360f,
                size = 6f + Random.nextFloat() * 8f,
                color = CONFETTI_COLORS.random(),
                delay = Random.nextFloat() * 0.35f,
            )
        }
    }
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (t < durationMs / 1000f) {
            withFrameNanos { now -> t = (now - start) / 1e9f }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        pieces.forEach { p ->
            val tt = (t - p.delay).coerceAtLeast(0f)
            if (tt <= 0f) return@forEach
            val px = (p.x + p.vx * tt) * size.width
            val py = -40f + (p.vy * tt + 0.45f * tt * tt) * size.height
            if (py > size.height + 40f) return@forEach
            val fade = (1f - (t / (durationMs / 1000f))).coerceIn(0f, 1f)
            rotate(p.spin * tt, Offset(px, py)) {
                drawRect(p.color.copy(alpha = fade), topLeft = Offset(px - p.size / 2, py - p.size / 4), size = Size(p.size, p.size / 2))
            }
        }
    }
}

/** 位置調整用（飛ばす元などの中心座標を取る） */
fun LayoutCoordinates.centerInRoot(): Offset = boundsInRoot().center

