package com.daybudget.app.ui.icons

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.daybudget.app.domain.Categories

/** Web版と同じ線画アイコン（24x24、線幅1.9） */
object DbIcons {
    private fun icon(name: String, vararg paths: String, width: Float = 1.9f): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach {
            b.addPath(
                pathData = PathParser().parsePathString(it).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    val Sliders = icon("sliders", "M21 4h-7M10 4H3M21 12h-9M8 12H3M21 20h-5M12 20H3M14 2v4M8 10v4M16 18v4")
    val Calendar = icon("calendar", "M6 4h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3z", "M16 2v4M8 2v4M3 10h18")
    val Left = icon("left", "M15 18l-6-6 6-6")
    val Right = icon("right", "M9 18l6-6-6-6")
    val Close = icon("close", "M18 6L6 18M6 6l12 12")
    val Trash = icon("trash", "M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v5M14 11v5")
    val Lock = icon("lock", "M6 11h12a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-6a2 2 0 0 1 2-2z", "M8 11V7a4 4 0 0 1 8 0v4")
    val Check = icon("check", "M20 6L9 17l-5-5")
    val Minus = icon("minus", "M6 12h12")
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Backspace = icon("backspace", "M21 5H8l-6 7 6 7h13a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1z", "M16 9l-5 6M11 9l5 6")
    val Spark = icon("spark", "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z", "M19 17l.7 1.8 1.8.7-1.8.7L19 22l-.7-1.8-1.8-.7 1.8-.7z")
    val Shield = icon("shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z")
    val Tap = icon("tap", "M9 11V5a2 2 0 0 1 4 0v5l4.5.9a2 2 0 0 1 1.6 2.3L18 19a3 3 0 0 1-3 2.5h-3.3a3 3 0 0 1-2.4-1.2L5.6 15a1.8 1.8 0 0 1 2.7-2.4L9 13.4")
    val Roll = icon("roll", "M3 12a9 9 0 0 1 15.5-6.2L21 8M21 3v5h-5M21 12a9 9 0 0 1-15.5 6.2L3 16M3 21v-5h5")

    private val food = icon("food", "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2M7 2v20M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7")
    private val cafe = icon("cafe", "M17 8h1a4 4 0 1 1 0 8h-1", "M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4z", "M6 2v2M10 2v2M14 2v2")
    private val daily = icon("daily", "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z", "M3 6h18M16 10a4 4 0 0 1-8 0")
    private val transport = icon("transport", "M7 3h10a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z", "M4 11h16M12 3v8M8 19l-2 3M18 22l-2-3M8 15h.01M16 15h.01")
    private val fun_ = icon("fun", "M5 6h14a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3H5a3 3 0 0 1-3-3V9a3 3 0 0 1 3-3z", "M6 12h4M8 10v4M15 13h.01M18 11h.01")
    private val other = icon("other", "M5 12h.01M12 12h.01M19 12h.01", width = 4f)

    fun category(c: Categories): ImageVector = when (c) {
        Categories.FOOD -> food
        Categories.CAFE -> cafe
        Categories.DAILY -> daily
        Categories.TRANSPORT -> transport
        Categories.FUN -> fun_
        Categories.OTHER -> other
    }
}
